package com.fitcore.dto;

import java.time.LocalDate;
import java.util.List;

/** Rutina completa para la interfaz (sin entidades JPA: evita ciclos de serialización). */
public record RutinaResponseDTO(
        Long id,
        String nombre,
        String objetivo,
        String descripcion,
        Long clienteId,
        String nombreCliente,
        Long entrenadorId,
        String nombreEntrenador,
        boolean entrenadorActivo,
        Integer duracionSemanas,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        LocalDate fechaAsignacion,
        Boolean activa,
        int totalDias,
        int totalEjercicios,
        List<DiaRutinaDTO> dias
) {}
