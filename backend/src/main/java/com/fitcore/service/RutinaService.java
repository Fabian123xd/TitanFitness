package com.fitcore.service;

import com.fitcore.dto.DiaRutinaDTO;
import com.fitcore.dto.EjercicioRutinaDTO;
import com.fitcore.dto.RutinaRequestDTO;
import com.fitcore.dto.RutinaResponseDTO;
import com.fitcore.exception.RecursoNoEncontradoException;
import com.fitcore.exception.ReglaDeNegocioException;
import com.fitcore.model.Cliente;
import com.fitcore.model.DiaRutina;
import com.fitcore.model.DiaSemana;
import com.fitcore.model.EjercicioRutina;
import com.fitcore.model.Entrenador;
import com.fitcore.model.Rutina;
import com.fitcore.repository.ClienteRepository;
import com.fitcore.repository.EntrenadorRepository;
import com.fitcore.repository.RutinaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Servicio de lógica de negocio para la gestión de rutinas de entrenamiento.
 *
 * Reglas de negocio aplicadas:
 *  RU-01 No se asigna una rutina a un entrenador inactivo.
 *  RU-02 No se reactiva una rutina cuyo entrenador está inactivo.
 *  RU-03 Un cliente conserva todas sus rutinas (historial); desactivar no borra.
 *  RU-04 fechaFin = fechaInicio + duracionSemanas * 7 días (si hay duración).
 *  RU-05 Un mismo día de la semana no puede repetirse dentro de una rutina.
 *  RU-06 Los ejercicios y días se ordenan por su campo orden.
 *  RU-07 Al actualizar una rutina existente, la colección de días se sincroniza
 *        reutilizando las entidades DiaRutina cuyo diaSemana coincida y eliminando
 *        los días descartados antes de insertar nuevos, evitando violaciones
 *        SQLState 23505 en la restricción UNIQUE (rutina_id, dia_semana).
 */
@Service
@Transactional(readOnly = true)
public class RutinaService {

    private final RutinaRepository rutinaRepository;
    private final ClienteRepository clienteRepository;
    private final EntrenadorRepository entrenadorRepository;
    private final Clock clock;

    public RutinaService(RutinaRepository rutinaRepository,
                         ClienteRepository clienteRepository,
                         EntrenadorRepository entrenadorRepository,
                         Clock clock) {
        this.rutinaRepository = rutinaRepository;
        this.clienteRepository = clienteRepository;
        this.entrenadorRepository = entrenadorRepository;
        this.clock = clock;
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    public List<RutinaResponseDTO> listarTodas() {
        return rutinaRepository.findAllConRelaciones().stream().map(r -> toDTO(r, false)).toList();
    }

    public RutinaResponseDTO buscarPorId(Long id) {
        return toDTO(obtener(id), true);
    }

    public List<RutinaResponseDTO> listarPorCliente(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new RecursoNoEncontradoException("Cliente", clienteId);
        }
        return rutinaRepository.findByClienteId(clienteId).stream().map(r -> toDTO(r, false)).toList();
    }

    public List<RutinaResponseDTO> listarActivasPorCliente(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new RecursoNoEncontradoException("Cliente", clienteId);
        }
        return rutinaRepository.findActivasByClienteId(clienteId).stream().map(r -> toDTO(r, false)).toList();
    }

    public List<RutinaResponseDTO> listarPorEntrenador(Long entrenadorId) {
        if (!entrenadorRepository.existsById(entrenadorId)) {
            throw new RecursoNoEncontradoException("Entrenador", entrenadorId);
        }
        return rutinaRepository.findByEntrenadorId(entrenadorId).stream().map(r -> toDTO(r, false)).toList();
    }

    // ── Escritura ─────────────────────────────────────────────────────────────

    @Transactional
    public RutinaResponseDTO registrar(RutinaRequestDTO request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", request.clienteId()));
        Entrenador entrenador = entrenadorRepository.findById(request.entrenadorId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Entrenador", request.entrenadorId()));
        validarEntrenadorActivo(entrenador);
        validarDiasSinRepetir(request.dias());

        Rutina rutina = new Rutina();
        rutina.setCliente(cliente);
        rutina.setEntrenador(entrenador);
        rutina.setActiva(true);
        rutina.setFechaAsignacion(LocalDate.now(clock));
        aplicarDatos(rutina, request);
        return toDTO(rutinaRepository.save(rutina), true);
    }

    @Transactional
    public RutinaResponseDTO actualizar(Long id, RutinaRequestDTO request) {
        Rutina rutina = obtener(id);
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", request.clienteId()));
        Entrenador entrenador = entrenadorRepository.findById(request.entrenadorId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Entrenador", request.entrenadorId()));

        // Solo se exige entrenador activo si se (re)asigna a otro entrenador.
        if (!entrenador.getId().equals(rutina.getEntrenador().getId())) {
            validarEntrenadorActivo(entrenador);
        }
        validarDiasSinRepetir(request.dias());

        rutina.setCliente(cliente);
        rutina.setEntrenador(entrenador);
        aplicarDatos(rutina, request);
        return toDTO(rutinaRepository.save(rutina), true);
    }

    @Transactional
    public RutinaResponseDTO desactivar(Long id) {
        Rutina rutina = obtener(id);
        rutina.setActiva(false);
        return toDTO(rutinaRepository.save(rutina), false);
    }

    @Transactional
    public RutinaResponseDTO reactivar(Long id) {
        Rutina rutina = obtener(id);
        if (!Boolean.TRUE.equals(rutina.getEntrenador().getActivo())) {
            throw new ReglaDeNegocioException(
                    "No se puede reactivar la rutina: su entrenador está inactivo. Reasígnela a un entrenador activo.");
        }
        rutina.setActiva(true);
        return toDTO(rutinaRepository.save(rutina), false);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Rutina obtener(Long id) {
        return rutinaRepository.findByIdConRelaciones(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rutina", id));
    }

    private void validarEntrenadorActivo(Entrenador entrenador) {
        if (!Boolean.TRUE.equals(entrenador.getActivo())) {
            throw new ReglaDeNegocioException(
                    "No se puede asignar una rutina a un entrenador inactivo (" + entrenador.getNombres() + " "
                            + entrenador.getApellidos() + ").");
        }
    }

    private void validarDiasSinRepetir(List<DiaRutinaDTO> dias) {
        if (dias == null) {
            return;
        }
        Set<DiaSemana> vistos = EnumSet.noneOf(DiaSemana.class);
        for (DiaRutinaDTO dia : dias) {
            if (!vistos.add(dia.diaSemana())) {
                throw new ReglaDeNegocioException(
                        "El día " + dia.diaSemana() + " está repetido en la rutina.");
            }
        }
    }

    /**
     * Copia los datos escalares del request a la entidad y sincroniza la colección de
     * días y ejercicios sin provocar violaciones de clave única (SQLState 23505).
     *
     * Cuando la rutina ya posee un DiaRutina con el mismo DiaSemana, reutiliza y actualiza
     * esa instancia en lugar de eliminarla e insertar otra con la misma clave única
     * (rutina_id, dia_semana). Los días que ya no están en la petición se eliminan por
     * orphanRemoval.
     */
    private void aplicarDatos(Rutina rutina, RutinaRequestDTO request) {
        rutina.setNombre(request.nombre().trim());
        rutina.setObjetivo(request.objetivo().trim());
        rutina.setDescripcion(request.descripcion() == null || request.descripcion().isBlank()
                ? null : request.descripcion().trim());
        rutina.setDuracionSemanas(request.duracionSemanas());

        LocalDate inicio = request.fechaInicio() != null ? request.fechaInicio() : LocalDate.now(clock);
        rutina.setFechaInicio(inicio);
        rutina.setFechaFin(request.duracionSemanas() != null
                ? inicio.plusWeeks(request.duracionSemanas()) : null);

        if (request.dias() == null || request.dias().isEmpty()) {
            rutina.getDias().clear();
            return;
        }

        // Indexar los días actuales por DiaSemana para reutilizar los que permanecen
        Map<DiaSemana, DiaRutina> existentesPorDia = new EnumMap<>(DiaSemana.class);
        for (DiaRutina existente : rutina.getDias()) {
            if (existente.getDiaSemana() != null) {
                existentesPorDia.put(existente.getDiaSemana(), existente);
            }
        }

        // Determinar qué días siguen presentes en la solicitud
        Set<DiaSemana> diasSolicitados = EnumSet.noneOf(DiaSemana.class);
        for (DiaRutinaDTO dto : request.dias()) {
            diasSolicitados.add(dto.diaSemana());
        }

        // Eliminar únicamente los días que ya no vienen en la solicitud
        rutina.getDias().removeIf(d -> !diasSolicitados.contains(d.getDiaSemana()));

        int ordenDia = 1;
        for (DiaRutinaDTO diaDto : request.dias()) {
            DiaRutina dia = existentesPorDia.get(diaDto.diaSemana());
            boolean esNuevo = (dia == null);
            if (esNuevo) {
                dia = new DiaRutina();
                dia.setDiaSemana(diaDto.diaSemana());
            }

            dia.setNombreDia(diaDto.nombreDia() == null || diaDto.nombreDia().isBlank()
                    ? null : diaDto.nombreDia().trim());
            dia.setOrden(diaDto.orden() != null ? diaDto.orden() : ordenDia);
            ordenDia++;

            // Reconstruir los ejercicios del día (no tienen restricción UNIQUE que colisione)
            dia.getEjercicios().clear();
            int ordenEj = 1;
            if (diaDto.ejercicios() != null) {
                for (EjercicioRutinaDTO ejDto : diaDto.ejercicios()) {
                    EjercicioRutina ej = new EjercicioRutina();
                    ej.setNombre(ejDto.nombre().trim());
                    ej.setSeries(ejDto.series());
                    ej.setRepeticiones(ejDto.repeticiones());
                    ej.setDescansoSeg(ejDto.descansoSeg());
                    ej.setNotas(ejDto.notas() == null || ejDto.notas().isBlank() ? null : ejDto.notas().trim());
                    ej.setOrden(ejDto.orden() != null ? ejDto.orden() : ordenEj);
                    ordenEj++;
                    dia.agregarEjercicio(ej);
                }
            }

            if (esNuevo) {
                rutina.agregarDia(dia);
            }
        }

        // Mantener la lista en memoria ordenada por 'orden'
        rutina.getDias().sort((a, b) -> Integer.compare(
                a.getOrden() != null ? a.getOrden() : 0,
                b.getOrden() != null ? b.getOrden() : 0));
    }

    RutinaResponseDTO toDTO(Rutina r, boolean conDetalle) {
        List<DiaRutinaDTO> dias = new ArrayList<>();
        int totalEjercicios = 0;
        for (DiaRutina dia : r.getDias()) {
            List<EjercicioRutinaDTO> ejercicios = dia.getEjercicios().stream()
                    .map(e -> new EjercicioRutinaDTO(e.getId(), e.getNombre(), e.getSeries(),
                            e.getRepeticiones(), e.getDescansoSeg(), e.getNotas(), e.getOrden()))
                    .toList();
            totalEjercicios += ejercicios.size();
            if (conDetalle) {
                dias.add(new DiaRutinaDTO(dia.getId(), dia.getDiaSemana(), dia.getNombreDia(),
                        dia.getOrden(), ejercicios));
            }
        }
        Cliente c = r.getCliente();
        Entrenador e = r.getEntrenador();
        return new RutinaResponseDTO(
                r.getId(), r.getNombre(), r.getObjetivo(), r.getDescripcion(),
                c.getId(), c.getNombres() + " " + c.getApellidos(),
                e.getId(), e.getNombres() + " " + e.getApellidos(),
                Boolean.TRUE.equals(e.getActivo()),
                r.getDuracionSemanas(), r.getFechaInicio(), r.getFechaFin(),
                r.getFechaAsignacion(), r.getActiva(),
                r.getDias().size(), totalEjercicios,
                dias);
    }
}
