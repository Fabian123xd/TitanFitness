package com.fitcore.service;

import com.fitcore.dto.InscripcionRequestDTO;
import com.fitcore.dto.InscripcionResponseDTO;
import com.fitcore.exception.RecursoNoEncontradoException;
import com.fitcore.exception.ReglaDeNegocioException;
import com.fitcore.model.Cliente;
import com.fitcore.model.EstadoInscripcion;
import com.fitcore.model.Inscripcion;
import com.fitcore.model.Membresia;
import com.fitcore.repository.ClienteRepository;
import com.fitcore.repository.InscripcionRepository;
import com.fitcore.repository.MembresiaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InscripcionServiceTest {

    /** Miércoles 18 de marzo de 2026 (fecha fija para pruebas deterministas). */
    private static final LocalDate HOY = LocalDate.of(2026, 3, 18);

    @Mock InscripcionRepository inscripcionRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock MembresiaRepository membresiaRepository;

    private InscripcionService service;

    @BeforeEach
    void setUp() {
        service = crearServicio(Set.of());
    }

    private InscripcionService crearServicio(Set<LocalDate> feriados) {
        Clock reloj = Clock.fixed(HOY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        return new InscripcionService(inscripcionRepository, clienteRepository, membresiaRepository,
                new CalendarioLaboral(feriados), reloj);
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private Cliente cliente(Long id) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setNombres("Ana");
        c.setApellidos("Perez");
        return c;
    }

    private Membresia plan(Long id, int dias, boolean activo) {
        Membresia m = new Membresia();
        m.setId(id);
        m.setNombre("Plan " + id);
        m.setDuracionDias(dias);
        m.setPrecio(BigDecimal.TEN);
        m.setActivo(activo);
        return m;
    }

    private Inscripcion inscripcion(Long id, Membresia plan, LocalDate inicio, LocalDate fin, EstadoInscripcion estado) {
        Inscripcion i = new Inscripcion();
        i.setId(id);
        i.setCliente(cliente(1L));
        i.setMembresia(plan);
        i.setFechaInicio(inicio);
        i.setFechaFin(fin);
        i.setEstado(estado);
        return i;
    }

    private void stubSave() {
        when(inscripcionRepository.save(any(Inscripcion.class))).thenAnswer(inv -> {
            Inscripcion i = inv.getArgument(0);
            if (i.getId() == null) {
                i.setId(100L);
            }
            return i;
        });
    }

    // ── Registro / asignación ─────────────────────────────────────────────────

    @Test
    @DisplayName("Asignar plan: queda PENDIENTE y la fecha de fin se calcula con la duración del plan")
    void registrarCalculaVencimiento() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L)));
        when(membresiaRepository.findById(2L)).thenReturn(Optional.of(plan(2L, 30, true)));
        when(inscripcionRepository.existsSolapamiento(eq(1L), any(), any(), anyLong())).thenReturn(false);
        stubSave();

        InscripcionResponseDTO dto = service.registrar(new InscripcionRequestDTO(1L, 2L, null, "nota"));

        assertEquals("PENDIENTE", dto.estado());
        assertEquals(HOY, dto.fechaInicio());
        assertEquals(HOY.plusDays(30), dto.fechaFin());
        assertEquals("Ana Perez", dto.nombreCliente());
        verify(inscripcionRepository).marcarVencidas(HOY);
    }

    @Test
    @DisplayName("Asignar plan con fecha de inicio futura calcula el vencimiento desde esa fecha")
    void registrarConFechaFutura() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L)));
        when(membresiaRepository.findById(2L)).thenReturn(Optional.of(plan(2L, 90, true)));
        stubSave();

        LocalDate inicio = HOY.plusDays(10);
        InscripcionResponseDTO dto = service.registrar(new InscripcionRequestDTO(1L, 2L, inicio, null));

        assertEquals(inicio.plusDays(90), dto.fechaFin());
    }

    @Test
    @DisplayName("No se acepta una fecha de inicio en el pasado")
    void registrarFechaPasada() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L)));
        when(membresiaRepository.findById(2L)).thenReturn(Optional.of(plan(2L, 30, true)));

        assertThrows(ReglaDeNegocioException.class,
                () -> service.registrar(new InscripcionRequestDTO(1L, 2L, HOY.minusDays(1), null)));
        verify(inscripcionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cliente inexistente → RecursoNoEncontrado")
    void registrarClienteInexistente() {
        when(clienteRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.registrar(new InscripcionRequestDTO(9L, 2L, null, null)));
    }

    @Test
    @DisplayName("Plan inexistente → RecursoNoEncontrado")
    void registrarPlanInexistente() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L)));
        when(membresiaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.registrar(new InscripcionRequestDTO(1L, 9L, null, null)));
    }

    @Test
    @DisplayName("Plan inactivo → no se puede asignar")
    void registrarPlanInactivo() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L)));
        when(membresiaRepository.findById(2L)).thenReturn(Optional.of(plan(2L, 30, false)));

        ReglaDeNegocioException ex = assertThrows(ReglaDeNegocioException.class,
                () -> service.registrar(new InscripcionRequestDTO(1L, 2L, null, null)));
        assertTrue(ex.getMessage().contains("no está activo"));
        verify(inscripcionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Prevención de duplicados: período superpuesto → rechazado")
    void registrarSuperpuesto() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L)));
        when(membresiaRepository.findById(2L)).thenReturn(Optional.of(plan(2L, 30, true)));
        when(inscripcionRepository.existsSolapamiento(1L, HOY, HOY.plusDays(30), -1L)).thenReturn(true);

        ReglaDeNegocioException ex = assertThrows(ReglaDeNegocioException.class,
                () -> service.registrar(new InscripcionRequestDTO(1L, 2L, null, null)));
        assertTrue(ex.getMessage().contains("se superpone"));
        verify(inscripcionRepository, never()).save(any());
    }

    // ── Activación sin pagos ──────────────────────────────────────────────────

    @Test
    @DisplayName("Activar: PENDIENTE → ACTIVA sin intervención de pagos")
    void activarPendiente() {
        Inscripcion ins = inscripcion(5L, plan(2L, 30, true), HOY, HOY.plusDays(30), EstadoInscripcion.PENDIENTE);
        when(inscripcionRepository.findById(5L)).thenReturn(Optional.of(ins));
        stubSave();

        assertEquals("ACTIVA", service.activar(5L).estado());
        assertEquals(EstadoInscripcion.ACTIVA, ins.getEstado());
    }

    @Test
    @DisplayName("Activar una inscripción que no está PENDIENTE → rechazado")
    void activarNoPendiente() {
        Inscripcion ins = inscripcion(5L, plan(2L, 30, true), HOY, HOY.plusDays(30), EstadoInscripcion.ACTIVA);
        when(inscripcionRepository.findById(5L)).thenReturn(Optional.of(ins));

        assertThrows(ReglaDeNegocioException.class, () -> service.activar(5L));
        verify(inscripcionRepository, never()).save(any());
    }

    @Test
    @DisplayName("No se activa una inscripción cuyo período ya terminó")
    void activarPeriodoTerminado() {
        Inscripcion ins = inscripcion(5L, plan(2L, 30, true), HOY.minusDays(40), HOY.minusDays(10), EstadoInscripcion.PENDIENTE);
        when(inscripcionRepository.findById(5L)).thenReturn(Optional.of(ins));

        assertThrows(ReglaDeNegocioException.class, () -> service.activar(5L));
    }

    @Test
    @DisplayName("Activar inscripción inexistente → RecursoNoEncontrado")
    void activarInexistente() {
        when(inscripcionRepository.findById(77L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.activar(77L));
    }

    // ── Cancelación y eliminación ─────────────────────────────────────────────

    @Test
    @DisplayName("Cancelar una inscripción ACTIVA conserva el registro como CANCELADA")
    void cancelarActiva() {
        Inscripcion ins = inscripcion(5L, plan(2L, 30, true), HOY, HOY.plusDays(30), EstadoInscripcion.ACTIVA);
        when(inscripcionRepository.findById(5L)).thenReturn(Optional.of(ins));
        stubSave();

        assertEquals("CANCELADA", service.cancelar(5L).estado());
    }

    @Test
    @DisplayName("Una inscripción VENCIDA no se puede cancelar")
    void cancelarVencida() {
        Inscripcion ins = inscripcion(5L, plan(2L, 30, true), HOY.minusDays(60), HOY.minusDays(30), EstadoInscripcion.VENCIDA);
        when(inscripcionRepository.findById(5L)).thenReturn(Optional.of(ins));

        assertThrows(ReglaDeNegocioException.class, () -> service.cancelar(5L));
    }

    @Test
    @DisplayName("Solo se eliminan inscripciones PENDIENTES; las demás son historial")
    void eliminarProtegeHistorial() {
        Inscripcion activa = inscripcion(5L, plan(2L, 30, true), HOY, HOY.plusDays(30), EstadoInscripcion.ACTIVA);
        when(inscripcionRepository.findById(5L)).thenReturn(Optional.of(activa));
        assertThrows(ReglaDeNegocioException.class, () -> service.eliminar(5L));
        verify(inscripcionRepository, never()).delete(any());

        Inscripcion pendiente = inscripcion(6L, plan(2L, 30, true), HOY, HOY.plusDays(30), EstadoInscripcion.PENDIENTE);
        when(inscripcionRepository.findById(6L)).thenReturn(Optional.of(pendiente));
        service.eliminar(6L);
        verify(inscripcionRepository).delete(pendiente);
    }

    // ── Renovación ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Renovar ACTIVA: crea una inscripción nueva desde el día siguiente al fin y NO modifica la anterior")
    void renovarConservaHistorial() {
        LocalDate finBase = HOY.plusDays(5);
        Inscripcion base = inscripcion(5L, plan(2L, 30, true), HOY.minusDays(25), finBase, EstadoInscripcion.ACTIVA);
        when(inscripcionRepository.findById(5L)).thenReturn(Optional.of(base));
        when(inscripcionRepository.existsSolapamiento(eq(1L), any(), any(), eq(5L))).thenReturn(false);
        stubSave();

        InscripcionResponseDTO dto = service.renovar(5L, null);

        assertEquals("PENDIENTE", dto.estado());
        assertEquals(finBase.plusDays(1), dto.fechaInicio());
        assertEquals(finBase.plusDays(1).plusDays(30), dto.fechaFin());
        assertTrue(dto.observaciones().contains("#5"));

        ArgumentCaptor<Inscripcion> captor = ArgumentCaptor.forClass(Inscripcion.class);
        verify(inscripcionRepository).save(captor.capture());
        assertEquals(100L, captor.getValue().getId()); // se guardó UNA inscripción nueva
        // la base queda exactamente como estaba
        assertEquals(EstadoInscripcion.ACTIVA, base.getEstado());
        assertEquals(finBase, base.getFechaFin());
        assertEquals(5L, base.getId());
    }

    @Test
    @DisplayName("Renovar una VENCIDA antigua inicia hoy (no se crean períodos en el pasado)")
    void renovarVencidaInicioHoy() {
        Inscripcion base = inscripcion(5L, plan(2L, 30, true), HOY.minusDays(90), HOY.minusDays(60), EstadoInscripcion.VENCIDA);
        when(inscripcionRepository.findById(5L)).thenReturn(Optional.of(base));
        stubSave();

        InscripcionResponseDTO dto = service.renovar(5L, "Vuelve al gimnasio");

        assertEquals(HOY, dto.fechaInicio());
        assertEquals(HOY.plusDays(30), dto.fechaFin());
        assertEquals("Vuelve al gimnasio", dto.observaciones());
    }

    @Test
    @DisplayName("No se renueva desde PENDIENTE ni CANCELADA")
    void renovarEstadoInvalido() {
        for (EstadoInscripcion estado : List.of(EstadoInscripcion.PENDIENTE, EstadoInscripcion.CANCELADA)) {
            Inscripcion base = inscripcion(5L, plan(2L, 30, true), HOY, HOY.plusDays(30), estado);
            when(inscripcionRepository.findById(5L)).thenReturn(Optional.of(base));
            assertThrows(ReglaDeNegocioException.class, () -> service.renovar(5L, null));
        }
        verify(inscripcionRepository, never()).save(any());
    }

    @Test
    @DisplayName("No se renueva si el plan fue desactivado")
    void renovarPlanInactivo() {
        Inscripcion base = inscripcion(5L, plan(2L, 30, false), HOY, HOY.plusDays(30), EstadoInscripcion.ACTIVA);
        when(inscripcionRepository.findById(5L)).thenReturn(Optional.of(base));

        assertThrows(ReglaDeNegocioException.class, () -> service.renovar(5L, null));
    }

    @Test
    @DisplayName("Renovación duplicada (ya existe una pendiente que se superpone) → rechazada")
    void renovarDuplicada() {
        Inscripcion base = inscripcion(5L, plan(2L, 30, true), HOY.minusDays(25), HOY.plusDays(5), EstadoInscripcion.ACTIVA);
        when(inscripcionRepository.findById(5L)).thenReturn(Optional.of(base));
        when(inscripcionRepository.existsSolapamiento(eq(1L), any(), any(), eq(5L))).thenReturn(true);

        assertThrows(ReglaDeNegocioException.class, () -> service.renovar(5L, null));
        verify(inscripcionRepository, never()).save(any());
    }

    // ── Alertas: 7 días hábiles vs días calendario ────────────────────────────

    private List<Inscripcion> activaQueVence(LocalDate fin) {
        return List.of(inscripcion(7L, plan(2L, 30, true), HOY.minusDays(20), fin, EstadoInscripcion.ACTIVA));
    }

    @Test
    @DisplayName("Alerta: vence en 5 días hábiles → aparece")
    void alertaCincoHabiles() {
        when(inscripcionRepository.findActivasVigentes(HOY)).thenReturn(activaQueVence(HOY.plusDays(7)));

        List<InscripcionResponseDTO> alertas = service.listarProximasAVencer();

        assertEquals(1, alertas.size());
        assertEquals(5, alertas.get(0).diasHabilesRestantes());
        assertTrue(alertas.get(0).proximaAVencer());
    }

    @Test
    @DisplayName("Alerta: 9 días calendario pero exactamente 7 hábiles (vie 27-mar) → SÍ alerta")
    void alertaSieteHabilesConNueveCalendario() {
        LocalDate viernes = LocalDate.of(2026, 3, 27);
        when(inscripcionRepository.findActivasVigentes(HOY)).thenReturn(activaQueVence(viernes));

        List<InscripcionResponseDTO> alertas = service.listarProximasAVencer();

        assertEquals(1, alertas.size());
        assertEquals(9, alertas.get(0).diasRestantes());
        assertEquals(7, alertas.get(0).diasHabilesRestantes());
    }

    @Test
    @DisplayName("Alerta: 12 días calendario = 8 hábiles (lun 30-mar) → NO alerta")
    void sinAlertaOchoHabiles() {
        LocalDate lunes = LocalDate.of(2026, 3, 30);
        when(inscripcionRepository.findActivasVigentes(HOY)).thenReturn(activaQueVence(lunes));

        assertTrue(service.listarProximasAVencer().isEmpty());
    }

    @Test
    @DisplayName("Alerta: un feriado reduce los días hábiles y activa la alerta (lun 30-mar con feriado 23-mar)")
    void feriadoActivaAlerta() {
        service = crearServicio(Set.of(LocalDate.of(2026, 3, 23)));
        when(inscripcionRepository.findActivasVigentes(HOY)).thenReturn(activaQueVence(LocalDate.of(2026, 3, 30)));

        assertEquals(1, service.listarProximasAVencer().size());
    }

    @Test
    @DisplayName("Una inscripción que vence hoy está en alerta con 0 días hábiles")
    void venceHoy() {
        when(inscripcionRepository.findActivasVigentes(HOY)).thenReturn(activaQueVence(HOY));

        List<InscripcionResponseDTO> alertas = service.listarProximasAVencer();

        assertEquals(1, alertas.size());
        assertEquals(0, alertas.get(0).diasHabilesRestantes());
    }

    // ── Vigencia / estados ────────────────────────────────────────────────────

    @Test
    @DisplayName("ACTIVA con fecha de fin pasada se muestra VENCIDA y permite renovar")
    void activaExpiradaSeMuestraVencida() {
        Inscripcion ins = inscripcion(8L, plan(2L, 30, true), HOY.minusDays(60), HOY.minusDays(2), EstadoInscripcion.ACTIVA);
        when(inscripcionRepository.findById(8L)).thenReturn(Optional.of(ins));

        InscripcionResponseDTO dto = service.buscarPorId(8L).orElseThrow();

        assertEquals("VENCIDA", dto.estado());
        assertTrue(dto.vencida());
        assertFalse(dto.proximaAVencer());
        assertEquals(0, dto.diasRestantes());
    }

    @Test
    @DisplayName("Resumen del dashboard combina contadores y alertas en días hábiles")
    void resumen() {
        when(inscripcionRepository.findActivasVigentes(HOY)).thenReturn(List.of(
                inscripcion(1L, plan(2L, 30, true), HOY.minusDays(20), HOY.plusDays(3), EstadoInscripcion.ACTIVA),
                inscripcion(2L, plan(2L, 30, true), HOY.minusDays(5), HOY.plusDays(25), EstadoInscripcion.ACTIVA)));
        when(inscripcionRepository.countActivas(HOY)).thenReturn(2L);
        when(inscripcionRepository.countPendientes()).thenReturn(1L);
        when(inscripcionRepository.countVencidas(HOY)).thenReturn(4L);
        when(inscripcionRepository.countCanceladas()).thenReturn(3L);
        when(inscripcionRepository.count()).thenReturn(10L);

        var resumen = service.obtenerResumen();

        assertEquals(2, resumen.activas());
        assertEquals(1, resumen.proximasAVencer());
        assertEquals(4, resumen.vencidas());
        assertEquals(1, resumen.pendientes());
        assertEquals(3, resumen.canceladas());
        assertEquals(10, resumen.total());
    }

    @Test
    @DisplayName("Trazabilidad: activar registra fechaActivacion, cancelar guarda fechaCancelacion y motivo, renovar vincula inscripcionOrigenId")
    void trazabilidadCompletaMembresias() {
        Inscripcion ins = inscripcion(15L, plan(2L, 30, true), HOY, HOY.plusDays(30), EstadoInscripcion.PENDIENTE);
        when(inscripcionRepository.findById(15L)).thenReturn(Optional.of(ins));
        stubSave();

        // 1. Activar registra fechaActivacion
        InscripcionResponseDTO activada = service.activar(15L);
        assertEquals("ACTIVA", activada.estado());
        assertEquals(HOY.atStartOfDay(), activada.fechaActivacion());

        // 2. Renovar vincula inscripcionOrigenId = 15
        when(inscripcionRepository.existsRenovacionDeOrigen(15L)).thenReturn(false);
        when(inscripcionRepository.existsSolapamiento(eq(1L), any(), any(), eq(15L))).thenReturn(false);
        InscripcionResponseDTO renovada = service.renovar(15L, "Segunda etapa");
        assertEquals(15L, renovada.inscripcionOrigenId());

        // 3. Intentar renovar por segunda vez la misma inscripción origen -> rechazado
        when(inscripcionRepository.existsRenovacionDeOrigen(15L)).thenReturn(true);
        assertThrows(ReglaDeNegocioException.class, () -> service.renovar(15L, null));

        // 4. Cancelar registra fechaCancelacion y motivoCancelacion
        InscripcionResponseDTO cancelada = service.cancelar(15L, "Viaje laboral");
        assertEquals("CANCELADA", cancelada.estado());
        assertEquals(HOY.atStartOfDay(), cancelada.fechaCancelacion());
        assertEquals("Viaje laboral", cancelada.motivoCancelacion());
    }
}
