package com.fitcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calendario laboral usado para las alertas de vencimiento.
 *
 * Un día hábil es cualquier día que no sea sábado, domingo ni feriado.
 * Los feriados se configuran en application.properties:
 *   fitcore.feriados=2026-12-25,2027-01-01
 */
@Component
public class CalendarioLaboral {

    private final Set<LocalDate> feriados;

    @Autowired
    public CalendarioLaboral(@Value("${fitcore.feriados:}") String feriadosConfigurados) {
        this.feriados = parsear(feriadosConfigurados);
    }

    /** Constructor para pruebas unitarias. */
    public CalendarioLaboral(Set<LocalDate> feriados) {
        this.feriados = Set.copyOf(feriados);
    }

    private static Set<LocalDate> parsear(String texto) {
        if (texto == null || texto.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(texto.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(LocalDate::parse)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean esHabil(LocalDate fecha) {
        DayOfWeek dia = fecha.getDayOfWeek();
        return dia != DayOfWeek.SATURDAY
                && dia != DayOfWeek.SUNDAY
                && !feriados.contains(fecha);
    }

    /**
     * Cuenta los días hábiles entre {@code desde} (exclusivo) y {@code hasta} (inclusivo).
     * Devuelve 0 si hasta no es posterior a desde.
     */
    public long diasHabilesEntre(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || !hasta.isAfter(desde)) {
            return 0L;
        }
        long cuenta = 0;
        for (LocalDate d = desde.plusDays(1); !d.isAfter(hasta); d = d.plusDays(1)) {
            if (esHabil(d)) {
                cuenta++;
            }
        }
        return cuenta;
    }
}
