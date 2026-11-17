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
 * Servicio de lógica de negocio para la gestión de rutinas de entrenamiento
 * y seguimiento del progreso de ejercicios.
 *
 * Reglas de negocio aplicadas:
 *  RU-01 No se asigna una rutina a un entrenador inactivo.
 *  RU-02 No se reactiva una rutina cuyo entrenador está inactivo.
 *  RU-03 Un cliente conserva todas sus rutinas (historial); desactivar no borra.
 *  RU-04 fechaFin = fechaInicio + duracionSemanas * 7 días (si hay duración).
 *  RU-05 Un mismo día de la semana no puede repetirse dentro de una rutina.
 *  RU-06 Los ejercicios y días se ordenan por su campo orden.
 *  RU-07 Sincronización de días por DiaSemana para evitar SQLState 23505.
 *  RU-08 Seguimiento de progreso: porcentajeProgreso = round(completados * 100.0 / total),
 *        devolviendo 0% cuando la rutina no tiene ejercicios.
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

    /**
     * Marca o desmarca un ejercicio como completado dentro de la rutina indicada
     * y recalcula el porcentaje de progreso.
     */
    @Transactional
    public RutinaResponseDTO toggleEjercicioCompletado(Long rutinaId, Long ejercicioId) {
        Rutina rutina = obtener(rutinaId);
        EjercicioRutina ejercicio = buscarEjercicioEnRutina(rutina, ejercicioId);
        ejercicio.setCompletado(!Boolean.TRUE.equals(ejercicio.getCompletado()));
        return toDTO(rutinaRepository.save(rutina), true);
    }

    /**
     * Registra o actualiza el peso utilizado (kg) en un ejercicio perteneciente a la rutina.
     */
    @Transactional
    public RutinaResponseDTO actualizarPesoEjercicio(Long rutinaId, Long ejercicioId, Double pesoKg) {
        validarRangoPeso(pesoKg);
        Rutina rutina = obtener(rutinaId);
        EjercicioRutina ejercicio = buscarEjercicioEnRutina(rutina, ejercicioId);
        ejercicio.setPesoKg(pesoKg);
        return toDTO(rutinaRepository.save(rutina), true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validarRangoPeso(Double pesoKg) {
        if (pesoKg == null) {
            return;
        }
        if (pesoKg < 0) {
            throw new ReglaDeNegocioException("El peso utilizado no puede ser negativo.");
        }
        if (pesoKg > 1000.0) {
            throw new ReglaDeNegocioException("El peso utilizado no puede superar 1000 kg.");
        }
    }

    private Rutina obtener(Long id) {
        return rutinaRepository.findByIdConRelaciones(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rutina", id));
    }

    private EjercicioRutina buscarEjercicioEnRutina(Rutina rutina, Long ejercicioId) {
        if (ejercicioId == null) {
            throw new ReglaDeNegocioException("El ID del ejercicio es obligatorio.");
        }
        for (DiaRutina dia : rutina.getDias()) {
            for (EjercicioRutina ej : dia.getEjercicios()) {
                if (ejercicioId.equals(ej.getId())) {
                    return ej;
                }
            }
        }
        throw new RecursoNoEncontradoException(
                "El ejercicio con ID " + ejercicioId + " no existe o no pertenece a la rutina #" + rutina.getId());
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

        Map<DiaSemana, DiaRutina> existentesPorDia = new EnumMap<>(DiaSemana.class);
        for (DiaRutina existente : rutina.getDias()) {
            if (existente.getDiaSemana() != null) {
                existentesPorDia.put(existente.getDiaSemana(), existente);
            }
        }

        Set<DiaSemana> diasSolicitados = EnumSet.noneOf(DiaSemana.class);
        for (DiaRutinaDTO dto : request.dias()) {
            diasSolicitados.add(dto.diaSemana());
        }

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

            dia.getEjercicios().clear();
            int ordenEj = 1;
            if (diaDto.ejercicios() != null) {
                for (EjercicioRutinaDTO ejDto : diaDto.ejercicios()) {
                    validarRangoPeso(ejDto.pesoKg());
                    EjercicioRutina ej = new EjercicioRutina();
                    ej.setNombre(ejDto.nombre().trim());
                    ej.setSeries(ejDto.series());
                    ej.setRepeticiones(ejDto.repeticiones());
                    ej.setDescansoSeg(ejDto.descansoSeg());
                    ej.setNotas(ejDto.notas() == null || ejDto.notas().isBlank() ? null : ejDto.notas().trim());
                    ej.setOrden(ejDto.orden() != null ? ejDto.orden() : ordenEj);
                    ej.setCompletado(Boolean.TRUE.equals(ejDto.completado()));
                    ej.setPesoKg(ejDto.pesoKg());
                    ordenEj++;
                    dia.agregarEjercicio(ej);
                }
            }

            if (esNuevo) {
                rutina.agregarDia(dia);
            }
        }

        rutina.getDias().sort((a, b) -> Integer.compare(
                a.getOrden() != null ? a.getOrden() : 0,
                b.getOrden() != null ? b.getOrden() : 0));
    }

    RutinaResponseDTO toDTO(Rutina r, boolean conDetalle) {
        List<DiaRutinaDTO> dias = new ArrayList<>();
        int totalEjercicios = 0;
        int ejerciciosCompletados = 0;

        for (DiaRutina dia : r.getDias()) {
            List<EjercicioRutinaDTO> ejercicios = new ArrayList<>();
            for (EjercicioRutina e : dia.getEjercicios()) {
                boolean comp = Boolean.TRUE.equals(e.getCompletado());
                if (comp) {
                    ejerciciosCompletados++;
                }
                totalEjercicios++;
                if (conDetalle) {
                    ejercicios.add(new EjercicioRutinaDTO(
                            e.getId(),
                            e.getNombre(),
                            e.getSeries(),
                            e.getRepeticiones(),
                            e.getDescansoSeg(),
                            e.getNotas(),
                            e.getOrden(),
                            comp,
                            e.getPesoKg()
                    ));
                }
            }
            if (conDetalle) {
                dias.add(new DiaRutinaDTO(dia.getId(), dia.getDiaSemana(), dia.getNombreDia(),
                        dia.getOrden(), ejercicios));
            }
        }

        int porcentajeProgreso = totalEjercicios > 0
                ? (int) Math.round((ejerciciosCompletados * 100.0) / totalEjercicios)
                : 0;

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
                ejerciciosCompletados, porcentajeProgreso,
                dias);
    }
}
