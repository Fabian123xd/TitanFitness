package com.fitcore.service;

import com.fitcore.dto.InscripcionRequestDTO;
import com.fitcore.dto.InscripcionResponseDTO;
import com.fitcore.dto.ResumenInscripcionesDTO;
import com.fitcore.exception.RecursoNoEncontradoException;
import com.fitcore.exception.ReglaDeNegocioException;
import com.fitcore.model.Cliente;
import com.fitcore.model.EstadoInscripcion;
import com.fitcore.model.Inscripcion;
import com.fitcore.model.Membresia;
import com.fitcore.repository.ClienteRepository;
import com.fitcore.repository.InscripcionRepository;
import com.fitcore.repository.MembresiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Gestión de inscripciones (contratación de un plan por un cliente) con trazabilidad
 * completa de renovaciones, activaciones y cancelaciones y protección frente a concurrencia.
 *
 * Reglas de negocio:
 *  IN-01 Un cliente no puede tener períodos PENDIENTE/ACTIVA superpuestos.
 *  IN-02 La fecha de inicio no puede ser pasada (por defecto, hoy).
 *  IN-03 fechaFin = fechaInicio + duración del plan.
 *  IN-04 El administrador activa la inscripción (PENDIENTE → ACTIVA) registrando fechaActivacion.
 *  IN-05 VENCIDA y CANCELADA no se reactivan: se renueva creando otra inscripción.
 *  IN-06 Renovar crea una inscripción nueva con inscripcionOrigenId = idBase,
 *        iniciando el día siguiente al fin de la base (o hoy si ya había vencido).
 *  IN-07 Solo se renueva desde ACTIVA o VENCIDA, con el plan activo y sin duplicar renovación.
 *  IN-08 ACTIVA con fechaFin pasada se considera VENCIDA.
 *  IN-09 Alerta: quedan 7 o menos días hábiles (sin sábados, domingos ni feriados).
 *  IN-10 Cancelar registra fechaCancelacion y motivoCancelacion.
 *  IN-11 Bloqueo pesimista por cliente al registrar y renovar para evitar condiciones de carrera.
 */
@Service
@Transactional(readOnly = true)
public class InscripcionService {

    /** Umbral de la alerta de vencimiento, en días hábiles. */
    public static final int DIAS_HABILES_ALERTA = 7;

    private final InscripcionRepository inscripcionRepository;
    private final ClienteRepository clienteRepository;
    private final MembresiaRepository membresiaRepository;
    private final CalendarioLaboral calendario;
    private final Clock clock;

    public InscripcionService(
            InscripcionRepository inscripcionRepository,
            ClienteRepository clienteRepository,
            MembresiaRepository membresiaRepository,
            CalendarioLaboral calendario,
            Clock clock) {
        this.inscripcionRepository = inscripcionRepository;
        this.clienteRepository = clienteRepository;
        this.membresiaRepository = membresiaRepository;
        this.calendario = calendario;
        this.clock = clock;
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    public List<InscripcionResponseDTO> listarTodas() {
        return inscripcionRepository.findAllConRelaciones().stream().map(this::toDTO).toList();
    }

    public Optional<InscripcionResponseDTO> buscarPorId(Long id) {
        return inscripcionRepository.findById(id).map(this::toDTO);
    }

    /** Historial completo (de la más reciente a la más antigua). */
    public List<InscripcionResponseDTO> listarPorCliente(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new RecursoNoEncontradoException("Cliente", clienteId);
        }
        return inscripcionRepository.findByClienteId(clienteId).stream().map(this::toDTO).toList();
    }

    public List<InscripcionResponseDTO> listarActivas() {
        return inscripcionRepository.findActivasVigentes(hoy()).stream().map(this::toDTO).toList();
    }

    public List<InscripcionResponseDTO> listarVencidas() {
        return inscripcionRepository.findVencidas(hoy()).stream().map(this::toDTO).toList();
    }

    /** Inscripciones activas a 7 días hábiles o menos de vencer. */
    public List<InscripcionResponseDTO> listarProximasAVencer() {
        return inscripcionRepository.findActivasVigentes(hoy()).stream()
                .filter(this::esProximaAVencer)
                .map(this::toDTO)
                .toList();
    }

    public ResumenInscripcionesDTO obtenerResumen() {
        LocalDate hoy = hoy();
        long proximas = inscripcionRepository.findActivasVigentes(hoy).stream()
                .filter(this::esProximaAVencer)
                .count();
        return new ResumenInscripcionesDTO(
                inscripcionRepository.countActivas(hoy),
                inscripcionRepository.countPendientes(),
                proximas,
                inscripcionRepository.countVencidas(hoy),
                inscripcionRepository.countCanceladas(),
                inscripcionRepository.count());
    }

    // ── Operaciones de escritura ──────────────────────────────────────────────

    /** Asigna un plan a un cliente adquiriendo bloqueo pesimista sobre el socio. */
    @Transactional
    public InscripcionResponseDTO registrar(InscripcionRequestDTO request) {
        Cliente cliente = obtenerClienteConBloqueo(request.clienteId());
        Membresia membresia = membresiaRepository.findById(request.membresiaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Membresía", request.membresiaId()));

        validarPlanActivo(membresia);

        LocalDate hoy = hoy();
        LocalDate inicio = request.fechaInicio() != null ? request.fechaInicio() : hoy;
        if (inicio.isBefore(hoy)) {
            throw new ReglaDeNegocioException("La fecha de inicio no puede ser anterior a hoy.");
        }
        LocalDate fin = inicio.plusDays(membresia.getDuracionDias());

        inscripcionRepository.marcarVencidas(hoy);
        validarSinSuperposicion(cliente.getId(), inicio, fin, -1L);

        Inscripcion inscripcion = nuevaInscripcion(cliente, membresia, inicio, fin, request.observaciones(), null);
        return toDTO(inscripcionRepository.save(inscripcion));
    }

    /** Activa una inscripción PENDIENTE registrando la fecha y hora exacta de activación. */
    @Transactional
    public InscripcionResponseDTO activar(Long id) {
        Inscripcion inscripcion = obtener(id);
        if (inscripcion.getEstado() != EstadoInscripcion.PENDIENTE) {
            throw new ReglaDeNegocioException(
                    "Solo se pueden activar inscripciones PENDIENTES. Estado actual: " + inscripcion.getEstado());
        }
        if (inscripcion.getFechaFin().isBefore(hoy())) {
            throw new ReglaDeNegocioException(
                    "No se puede activar una inscripción cuyo período ya terminó. Cree o renueve una nueva.");
        }
        inscripcion.setEstado(EstadoInscripcion.ACTIVA);
        inscripcion.setFechaActivacion(ahora());
        return toDTO(inscripcionRepository.save(inscripcion));
    }

    /** Cancela una inscripción sin motivo explícito (usa motivo predeterminado). */
    @Transactional
    public InscripcionResponseDTO cancelar(Long id) {
        return cancelar(id, null);
    }

    /** Cancela una inscripción PENDIENTE o ACTIVA registrando fecha y motivo de cancelación. */
    @Transactional
    public InscripcionResponseDTO cancelar(Long id, String motivo) {
        Inscripcion inscripcion = obtener(id);
        if (inscripcion.getEstado() != EstadoInscripcion.PENDIENTE
                && inscripcion.getEstado() != EstadoInscripcion.ACTIVA) {
            throw new ReglaDeNegocioException(
                    "Solo se pueden cancelar inscripciones PENDIENTES o ACTIVAS. Estado actual: "
                            + inscripcion.getEstado());
        }
        inscripcion.setEstado(EstadoInscripcion.CANCELADA);
        inscripcion.setFechaCancelacion(ahora());
        inscripcion.setMotivoCancelacion(
                (motivo != null && !motivo.isBlank()) ? motivo.trim() : "Cancelada por el administrador"
        );
        return toDTO(inscripcionRepository.save(inscripcion));
    }

    /**
     * Renueva una inscripción creando una NUEVA (PENDIENTE), vinculándola mediante
     * inscripcionOrigenId y dejando intacta la inscripción anterior.
     */
    @Transactional
    public InscripcionResponseDTO renovar(Long idBase, String observaciones) {
        Inscripcion base = obtener(idBase);
        Cliente cliente = clienteRepository.findByIdForUpdate(base.getCliente().getId())
                .orElse(base.getCliente());
        LocalDate hoy = hoy();

        if (base.getEstado() != EstadoInscripcion.ACTIVA && base.getEstado() != EstadoInscripcion.VENCIDA) {
            throw new ReglaDeNegocioException(
                    "Solo se puede renovar una inscripción ACTIVA o VENCIDA. Estado actual: " + base.getEstado());
        }
        Membresia plan = base.getMembresia();
        if (!Boolean.TRUE.equals(plan.getActivo())) {
            throw new ReglaDeNegocioException("El plan '" + plan.getNombre()
                    + "' ya no está activo. Cree una nueva inscripción con un plan vigente.");
        }

        if (inscripcionRepository.existsRenovacionDeOrigen(base.getId())) {
            throw new ReglaDeNegocioException(
                    "La inscripción #" + base.getId() + " ya cuenta con una renovación registrada.");
        }

        LocalDate inicio = base.getFechaFin().plusDays(1);
        if (inicio.isBefore(hoy)) {
            inicio = hoy;
        }
        LocalDate fin = inicio.plusDays(plan.getDuracionDias());

        inscripcionRepository.marcarVencidas(hoy);
        validarSinSuperposicion(cliente.getId(), inicio, fin, base.getId());

        String nota = (observaciones != null && !observaciones.isBlank())
                ? observaciones.trim()
                : "Renovación de la inscripción #" + idBase;
        Inscripcion nueva = nuevaInscripcion(cliente, plan, inicio, fin, nota, base.getId());
        return toDTO(inscripcionRepository.save(nueva));
    }

    /** Elimina una inscripción registrada por error. Solo PENDIENTE; las demás forman parte del historial. */
    @Transactional
    public void eliminar(Long id) {
        Inscripcion inscripcion = obtener(id);
        if (inscripcion.getEstado() != EstadoInscripcion.PENDIENTE) {
            throw new ReglaDeNegocioException(
                    "Solo se pueden eliminar inscripciones PENDIENTES. Las demás se conservan como historial; use Cancelar.");
        }
        inscripcionRepository.delete(inscripcion);
    }

    /** Marca como VENCIDA las inscripciones activas cuya fecha de fin ya pasó. */
    @Transactional
    public int sincronizarVencimientos() {
        return inscripcionRepository.marcarVencidas(hoy());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private LocalDate hoy() {
        return LocalDate.now(clock);
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }

    private Cliente obtenerClienteConBloqueo(Long clienteId) {
        if (clienteId == null) {
            throw new ReglaDeNegocioException("El ID del cliente es obligatorio.");
        }
        return clienteRepository.findByIdForUpdate(clienteId)
                .or(() -> clienteRepository.findById(clienteId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", clienteId));
    }

    private Inscripcion obtener(Long id) {
        return inscripcionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Inscripción", id));
    }

    private void validarPlanActivo(Membresia membresia) {
        if (!Boolean.TRUE.equals(membresia.getActivo())) {
            throw new ReglaDeNegocioException("El plan '" + membresia.getNombre()
                    + "' no está activo. Seleccione un plan vigente.");
        }
    }

    private void validarSinSuperposicion(Long clienteId, LocalDate inicio, LocalDate fin, Long excluirId) {
        if (inscripcionRepository.existsSolapamiento(clienteId, inicio, fin, excluirId)) {
            throw new ReglaDeNegocioException(
                    "El cliente ya tiene una inscripción activa o pendiente que se superpone con el período "
                            + inicio + " a " + fin + ".");
        }
    }

    private Inscripcion nuevaInscripcion(
            Cliente cliente,
            Membresia plan,
            LocalDate inicio,
            LocalDate fin,
            String obs,
            Long origenId) {
        Inscripcion i = new Inscripcion();
        i.setCliente(cliente);
        i.setMembresia(plan);
        i.setFechaInicio(inicio);
        i.setFechaFin(fin);
        i.setEstado(EstadoInscripcion.PENDIENTE);
        i.setFechaRegistro(hoy());
        i.setObservaciones(obs);
        i.setInscripcionOrigenId(origenId);
        return i;
    }

    /** Activa, no vencida y con 7 o menos días hábiles por delante. */
    boolean esProximaAVencer(Inscripcion i) {
        LocalDate hoy = hoy();
        return i.getEstado() == EstadoInscripcion.ACTIVA
                && i.getFechaFin() != null
                && !i.getFechaFin().isBefore(hoy)
                && calendario.diasHabilesEntre(hoy, i.getFechaFin()) <= DIAS_HABILES_ALERTA;
    }

    InscripcionResponseDTO toDTO(Inscripcion ins) {
        LocalDate hoy = hoy();
        LocalDate fin = ins.getFechaFin();
        boolean vencida = ins.getEstado() == EstadoInscripcion.VENCIDA
                || (ins.getEstado() == EstadoInscripcion.ACTIVA && fin != null && fin.isBefore(hoy));

        EstadoInscripcion estadoEfectivo = vencida ? EstadoInscripcion.VENCIDA : ins.getEstado();

        boolean enCurso = fin != null && !fin.isBefore(hoy);
        long diasRestantes = enCurso ? ChronoUnit.DAYS.between(hoy, fin) : 0L;
        long diasHabiles = enCurso ? calendario.diasHabilesEntre(hoy, fin) : 0L;

        return new InscripcionResponseDTO(
                ins.getId(),
                ins.getCliente().getId(),
                ins.getCliente().getNombres() + " " + ins.getCliente().getApellidos(),
                ins.getMembresia().getId(),
                ins.getMembresia().getNombre(),
                ins.getFechaInicio(),
                fin,
                ins.getFechaRegistro(),
                estadoEfectivo.name(),
                ins.getObservaciones(),
                diasRestantes,
                diasHabiles,
                esProximaAVencer(ins),
                vencida,
                ins.getInscripcionOrigenId(),
                ins.getFechaActivacion(),
                ins.getFechaCancelacion(),
                ins.getMotivoCancelacion());
    }
}
