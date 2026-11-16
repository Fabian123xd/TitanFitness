package com.fitcore.service;

import com.fitcore.exception.RecursoNoEncontradoException;
import com.fitcore.exception.ReglaDeNegocioException;
import com.fitcore.model.Membresia;
import com.fitcore.repository.InscripcionRepository;
import com.fitcore.repository.MembresiaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class MembresiaServiceTest {

    @Mock MembresiaRepository membresiaRepository;
    @Mock InscripcionRepository inscripcionRepository;

    private MembresiaService service;

    @BeforeEach
    void setUp() {
        service = new MembresiaService(membresiaRepository, inscripcionRepository);
    }

    private Membresia plan(Long id, String nombre, boolean activo) {
        Membresia m = new Membresia();
        m.setId(id);
        m.setNombre(nombre);
        m.setDuracionDias(30);
        m.setPrecio(BigDecimal.valueOf(80));
        m.setActivo(activo);
        return m;
    }

    @Test
    @DisplayName("Registrar plan nuevo lo guarda activo por defecto")
    void registrar() {
        Membresia nuevo = plan(null, "Mensual", true);
        nuevo.setActivo(null);
        when(membresiaRepository.existsByNombre("Mensual")).thenReturn(false);
        when(membresiaRepository.save(any(Membresia.class))).thenAnswer(inv -> inv.getArgument(0));

        assertTrue(service.registrar(nuevo).getActivo());
    }

    @Test
    @DisplayName("Registrar plan con nombre duplicado → rechazado")
    void registrarDuplicado() {
        when(membresiaRepository.existsByNombre("Mensual")).thenReturn(true);

        assertThrows(ReglaDeNegocioException.class, () -> service.registrar(plan(null, "Mensual", true)));
        verify(membresiaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Editar un plan cambiando el nombre a uno de otro plan → rechazado")
    void actualizarNombreDuplicado() {
        when(membresiaRepository.findById(1L)).thenReturn(Optional.of(plan(1L, "Mensual", true)));
        when(membresiaRepository.existsByNombre("Anual")).thenReturn(true);

        assertThrows(ReglaDeNegocioException.class, () -> service.actualizar(1L, plan(null, "Anual", true)));
    }

    @Test
    @DisplayName("Editar características del plan conservando su nombre")
    void actualizarMismoNombre() {
        Membresia existente = plan(1L, "Mensual", true);
        when(membresiaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(membresiaRepository.save(any(Membresia.class))).thenAnswer(inv -> inv.getArgument(0));

        Membresia datos = plan(null, "mensual", true);
        datos.setDuracionDias(45);
        datos.setPrecio(BigDecimal.valueOf(100));

        Membresia resultado = service.actualizar(1L, datos);

        assertEquals(45, resultado.getDuracionDias());
        assertEquals(BigDecimal.valueOf(100), resultado.getPrecio());
    }

    @Test
    @DisplayName("Activar/desactivar plan alterna el estado del PLAN (no de las inscripciones)")
    void toggle() {
        Membresia m = plan(1L, "Mensual", true);
        when(membresiaRepository.findById(1L)).thenReturn(Optional.of(m));
        when(membresiaRepository.save(any(Membresia.class))).thenAnswer(inv -> inv.getArgument(0));

        assertFalse(service.toggleActivo(1L).getActivo());
        assertTrue(service.toggleActivo(1L).getActivo());
    }

    @Test
    @DisplayName("No se elimina un plan con inscripciones en el historial")
    void eliminarProtegido() {
        when(membresiaRepository.existsById(1L)).thenReturn(true);
        when(inscripcionRepository.existsByMembresiaId(1L)).thenReturn(true);

        assertThrows(ReglaDeNegocioException.class, () -> service.eliminar(1L));
        verify(membresiaRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Eliminar plan inexistente → RecursoNoEncontrado")
    void eliminarInexistente() {
        when(membresiaRepository.existsById(8L)).thenReturn(false);
        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(8L));
    }
}
