package com.fitcore.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarioLaboralTest {

    // 2026-03-18 es miércoles
    private static final LocalDate MIERCOLES = LocalDate.of(2026, 3, 18);

    private final CalendarioLaboral calendario = new CalendarioLaboral(Set.of());

    @Test
    @DisplayName("Sábado y domingo no son días hábiles")
    void finesDeSemanaNoSonHabiles() {
        assertFalse(calendario.esHabil(LocalDate.of(2026, 3, 21)));
        assertFalse(calendario.esHabil(LocalDate.of(2026, 3, 22)));
        assertTrue(calendario.esHabil(LocalDate.of(2026, 3, 23)));
    }

    @Test
    @DisplayName("Una semana calendario (7 días) equivale a 5 días hábiles")
    void sieteDiasCalendarioSonCincoHabiles() {
        assertEquals(5, calendario.diasHabilesEntre(MIERCOLES, MIERCOLES.plusDays(7)));
    }

    @Test
    @DisplayName("El día de partida no se cuenta y el de llegada sí")
    void extremos() {
        assertEquals(0, calendario.diasHabilesEntre(MIERCOLES, MIERCOLES));
        assertEquals(1, calendario.diasHabilesEntre(MIERCOLES, MIERCOLES.plusDays(1)));
        assertEquals(0, calendario.diasHabilesEntre(MIERCOLES, MIERCOLES.minusDays(3)));
    }

    @Test
    @DisplayName("Los feriados configurados no cuentan como hábiles")
    void feriadosSeExcluyen() {
        var conFeriado = new CalendarioLaboral(Set.of(LocalDate.of(2026, 3, 23)));
        assertEquals(4, conFeriado.diasHabilesEntre(MIERCOLES, MIERCOLES.plusDays(7)));
    }

    @Test
    @DisplayName("Los feriados se leen desde texto de configuración")
    void feriadosDesdeTexto() {
        var cal = new CalendarioLaboral("2026-03-23, 2026-03-24");
        assertFalse(cal.esHabil(LocalDate.of(2026, 3, 23)));
        assertFalse(cal.esHabil(LocalDate.of(2026, 3, 24)));
        assertTrue(new CalendarioLaboral("").esHabil(LocalDate.of(2026, 3, 23)));
    }
}
