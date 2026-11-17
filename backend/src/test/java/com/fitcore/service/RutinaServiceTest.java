package com.fitcore.service;

import com.fitcore.dto.DiaRutinaDTO;
import com.fitcore.dto.EjercicioRutinaDTO;
import com.fitcore.dto.RutinaRequestDTO;
import com.fitcore.dto.RutinaResponseDTO;
import com.fitcore.exception.RecursoNoEncontradoException;
import com.fitcore.exception.ReglaDeNegocioException;
import com.fitcore.model.Cliente;
import com.fitcore.model.DiaSemana;
import com.fitcore.model.Entrenador;
import com.fitcore.model.Rutina;
import com.fitcore.repository.ClienteRepository;
import com.fitcore.repository.EntrenadorRepository;
import com.fitcore.repository.RutinaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RutinaServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 3, 18);

    @Mock RutinaRepository rutinaRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock EntrenadorRepository entrenadorRepository;

    private RutinaService service;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(HOY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new RutinaService(rutinaRepository, clienteRepository, entrenadorRepository, reloj);
    }

    private Cliente cliente(Long id) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setNombres("Luis");
        c.setApellidos("Rojas");
        return c;
    }

    private Entrenador entrenador(Long id, boolean activo) {
        Entrenador e = new Entrenador();
        e.setId(id);
        e.setNombres("Carla");
        e.setApellidos("Diaz");
        e.setActivo(activo);
        return e;
    }

    private EjercicioRutinaDTO ejercicio(String nombre, int series, int reps, Integer descanso) {
        return new EjercicioRutinaDTO(null, nombre, series, reps, descanso, null, null);
    }

    private RutinaRequestDTO request(Long clienteId, Long entrenadorId, List<DiaRutinaDTO> dias, Integer semanas) {
        return new RutinaRequestDTO("Fuerza", "Ganar masa muscular", "desc", clienteId, entrenadorId, semanas, null, dias);
    }

    private void stubSave() {
        when(rutinaRepository.save(any(Rutina.class))).thenAnswer(inv -> {
            Rutina r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(50L);
            }
            return r;
        });
    }

    @Test
    @DisplayName("Registrar rutina con entrenador activo: guarda objetivo, fechas, días y ejercicios")
    void registrarCompleta() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L)));
        when(entrenadorRepository.findById(2L)).thenReturn(Optional.of(entrenador(2L, true)));
        stubSave();

        List<DiaRutinaDTO> dias = List.of(
                new DiaRutinaDTO(null, DiaSemana.LUNES, "Pecho", null,
                        List.of(ejercicio("Press banca", 4, 12, 60), ejercicio("Fondos", 3, 15, 45))),
                new DiaRutinaDTO(null, DiaSemana.MIERCOLES, "Espalda", null,
                        List.of(ejercicio("Dominadas", 4, 8, 90))));

        RutinaResponseDTO dto = service.registrar(request(1L, 2L, dias, 4));

        assertEquals("Ganar masa muscular", dto.objetivo());
        assertEquals(HOY, dto.fechaInicio());
        assertEquals(HOY.plusWeeks(4), dto.fechaFin());
        assertEquals(HOY, dto.fechaAsignacion());
        assertTrue(dto.activa());
        assertEquals(2, dto.totalDias());
        assertEquals(3, dto.totalEjercicios());
        assertEquals(DiaSemana.LUNES, dto.dias().get(0).diaSemana());
        assertEquals("Press banca", dto.dias().get(0).ejercicios().get(0).nombre());
        assertEquals(4, dto.dias().get(0).ejercicios().get(0).series());
        assertEquals(45, dto.dias().get(0).ejercicios().get(1).descansoSeg());
    }

    @Test
    @DisplayName("Sin duración no se calcula fecha de fin")
    void sinDuracion() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L)));
        when(entrenadorRepository.findById(2L)).thenReturn(Optional.of(entrenador(2L, true)));
        stubSave();

        RutinaResponseDTO dto = service.registrar(request(1L, 2L, null, null));

        assertEquals(null, dto.fechaFin());
        assertEquals(0, dto.totalDias());
    }

    @Test
    @DisplayName("Entrenador inactivo → no se puede asignar la rutina")
    void entrenadorInactivo() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L)));
        when(entrenadorRepository.findById(2L)).thenReturn(Optional.of(entrenador(2L, false)));

        ReglaDeNegocioException ex = assertThrows(ReglaDeNegocioException.class,
                () -> service.registrar(request(1L, 2L, null, 4)));
        assertTrue(ex.getMessage().contains("inactivo"));
        verify(rutinaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cliente o entrenador inexistentes → RecursoNoEncontrado")
    void inexistentes() {
        when(clienteRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.registrar(request(9L, 2L, null, 4)));

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L)));
        when(entrenadorRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.registrar(request(1L, 9L, null, 4)));
    }

    @Test
    @DisplayName("Un día de la semana no puede repetirse en la misma rutina")
    void diaRepetido() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L)));
        when(entrenadorRepository.findById(2L)).thenReturn(Optional.of(entrenador(2L, true)));

        List<DiaRutinaDTO> dias = List.of(
                new DiaRutinaDTO(null, DiaSemana.LUNES, "A", null, List.of()),
                new DiaRutinaDTO(null, DiaSemana.LUNES, "B", null, List.of()));

        assertThrows(ReglaDeNegocioException.class, () -> service.registrar(request(1L, 2L, dias, 4)));
        verify(rutinaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar rutina: reemplaza datos, días y ejercicios")
    void actualizarReemplazaDias() {
        Rutina existente = new Rutina();
        existente.setId(10L);
        existente.setCliente(cliente(1L));
        existente.setEntrenador(entrenador(2L, true));
        existente.setActiva(true);
        when(rutinaRepository.findByIdConRelaciones(10L)).thenReturn(Optional.of(existente));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L)));
        when(entrenadorRepository.findById(2L)).thenReturn(Optional.of(entrenador(2L, true)));
        stubSave();

        RutinaRequestDTO req = new RutinaRequestDTO("Nueva", "Resistencia", null, 1L, 2L, 8, HOY.plusDays(7),
                List.of(new DiaRutinaDTO(null, DiaSemana.VIERNES, "Cardio", null,
                        List.of(ejercicio("Trote", 1, 1, 0)))));

        RutinaResponseDTO dto = service.actualizar(10L, req);

        assertEquals("Nueva", dto.nombre());
        assertEquals("Resistencia", dto.objetivo());
        assertEquals(HOY.plusDays(7).plusWeeks(8), dto.fechaFin());
        assertEquals(1, dto.totalDias());
        assertEquals(DiaSemana.VIERNES, dto.dias().get(0).diaSemana());
    }

    @Test
    @DisplayName("Reasignar a un entrenador inactivo al actualizar → rechazado")
    void actualizarConEntrenadorInactivo() {
        Rutina existente = new Rutina();
        existente.setId(10L);
        existente.setCliente(cliente(1L));
        existente.setEntrenador(entrenador(2L, true));
        when(rutinaRepository.findByIdConRelaciones(10L)).thenReturn(Optional.of(existente));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L)));
        when(entrenadorRepository.findById(3L)).thenReturn(Optional.of(entrenador(3L, false)));

        assertThrows(ReglaDeNegocioException.class, () -> service.actualizar(10L, request(1L, 3L, null, 4)));
        verify(rutinaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Editar una rutina cuyo entrenador (el mismo) se inactivó sí está permitido")
    void actualizarMismoEntrenadorInactivo() {
        Entrenador inactivo = entrenador(2L, false);
        Rutina existente = new Rutina();
        existente.setId(10L);
        existente.setCliente(cliente(1L));
        existente.setEntrenador(inactivo);
        when(rutinaRepository.findByIdConRelaciones(10L)).thenReturn(Optional.of(existente));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L)));
        when(entrenadorRepository.findById(2L)).thenReturn(Optional.of(inactivo));
        stubSave();

        RutinaResponseDTO dto = service.actualizar(10L, request(1L, 2L, null, 4));

        assertFalse(dto.entrenadorActivo());
    }

    @Test
    @DisplayName("Desactivar conserva la rutina en el historial; reactivar exige entrenador activo")
    void desactivarYReactivar() {
        Rutina r = new Rutina();
        r.setId(10L);
        r.setCliente(cliente(1L));
        r.setEntrenador(entrenador(2L, true));
        r.setActiva(true);
        when(rutinaRepository.findByIdConRelaciones(10L)).thenReturn(Optional.of(r));
        stubSave();

        assertFalse(service.desactivar(10L).activa());
        assertTrue(service.reactivar(10L).activa());
    }

    @Test
    @DisplayName("Reactivar con entrenador inactivo → rechazado")
    void reactivarEntrenadorInactivo() {
        Rutina r = new Rutina();
        r.setId(10L);
        r.setCliente(cliente(1L));
        r.setEntrenador(entrenador(2L, false));
        r.setActiva(false);
        when(rutinaRepository.findByIdConRelaciones(10L)).thenReturn(Optional.of(r));

        assertThrows(ReglaDeNegocioException.class, () -> service.reactivar(10L));
        assertFalse(r.getActiva());
    }

    @Test
    @DisplayName("Consultar rutinas de un cliente o entrenador inexistente → RecursoNoEncontrado")
    void consultasInexistentes() {
        when(clienteRepository.existsById(5L)).thenReturn(false);
        when(entrenadorRepository.existsById(6L)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () -> service.listarPorCliente(5L));
        assertThrows(RecursoNoEncontradoException.class, () -> service.listarPorEntrenador(6L));
    }

    @Test
    @DisplayName("Seguimiento de progreso: marcar y desmarcar ejercicios calcula porcentajeProgreso exacto")
    void toggleEjercicioCompletadoYPorcentaje() {
        Rutina r = new Rutina();
        r.setId(20L);
        r.setNombre("Progreso");
        r.setObjetivo("Fuerza");
        r.setCliente(cliente(1L));
        r.setEntrenador(entrenador(2L, true));
        r.setActiva(true);

        com.fitcore.model.DiaRutina dia = new com.fitcore.model.DiaRutina();
        dia.setId(1L);
        dia.setDiaSemana(DiaSemana.LUNES);

        com.fitcore.model.EjercicioRutina ej1 = new com.fitcore.model.EjercicioRutina();
        ej1.setId(101L);
        ej1.setNombre("Sentadilla");
        ej1.setSeries(4);
        ej1.setRepeticiones(10);
        ej1.setCompletado(false);

        com.fitcore.model.EjercicioRutina ej2 = new com.fitcore.model.EjercicioRutina();
        ej2.setId(102L);
        ej2.setNombre("Prensa");
        ej2.setSeries(4);
        ej2.setRepeticiones(12);
        ej2.setCompletado(false);

        dia.agregarEjercicio(ej1);
        dia.agregarEjercicio(ej2);
        r.agregarDia(dia);

        when(rutinaRepository.findByIdConRelaciones(20L)).thenReturn(Optional.of(r));
        stubSave();

        // Marcar primer ejercicio -> 1/2 = 50%
        RutinaResponseDTO dto1 = service.toggleEjercicioCompletado(20L, 101L);
        assertEquals(1, dto1.ejerciciosCompletados());
        assertEquals(50, dto1.porcentajeProgreso());
        assertTrue(dto1.dias().get(0).ejercicios().get(0).completado());

        // Marcar segundo ejercicio -> 2/2 = 100%
        RutinaResponseDTO dto2 = service.toggleEjercicioCompletado(20L, 102L);
        assertEquals(2, dto2.ejerciciosCompletados());
        assertEquals(100, dto2.porcentajeProgreso());

        // Desmarcar primer ejercicio -> 1/2 = 50%
        RutinaResponseDTO dto3 = service.toggleEjercicioCompletado(20L, 101L);
        assertEquals(1, dto3.ejerciciosCompletados());
        assertEquals(50, dto3.porcentajeProgreso());
    }

    @Test
    @DisplayName("Actualizar peso en ejercicio: acepta valores positivos/cero y rechaza negativos o ejercicio ajeno")
    void actualizarPesoEjercicioValidaciones() {
        Rutina r = new Rutina();
        r.setId(20L);
        r.setNombre("Progreso");
        r.setObjetivo("Fuerza");
        r.setCliente(cliente(1L));
        r.setEntrenador(entrenador(2L, true));

        com.fitcore.model.DiaRutina dia = new com.fitcore.model.DiaRutina();
        dia.setId(1L);
        dia.setDiaSemana(DiaSemana.LUNES);

        com.fitcore.model.EjercicioRutina ej = new com.fitcore.model.EjercicioRutina();
        ej.setId(101L);
        ej.setNombre("Press banca");
        ej.setSeries(4);
        ej.setRepeticiones(10);
        dia.agregarEjercicio(ej);
        r.agregarDia(dia);

        when(rutinaRepository.findByIdConRelaciones(20L)).thenReturn(Optional.of(r));
        stubSave();

        RutinaResponseDTO actualizado = service.actualizarPesoEjercicio(20L, 101L, 62.5);
        assertEquals(62.5, actualizado.dias().get(0).ejercicios().get(0).pesoKg());

        // Peso negativo -> ReglaDeNegocioException
        assertThrows(ReglaDeNegocioException.class,
                () -> service.actualizarPesoEjercicio(20L, 101L, -10.0));

        // Peso superior a 1000 kg -> ReglaDeNegocioException
        assertThrows(ReglaDeNegocioException.class,
                () -> service.actualizarPesoEjercicio(20L, 101L, 1000.5));

        // Ejercicio que no pertenece a la rutina -> RecursoNoEncontradoException
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.toggleEjercicioCompletado(20L, 999L));
    }
}
