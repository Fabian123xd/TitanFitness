package com.fitcore.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Rutina completa para la interfaz (incluye progreso de ejercicios completados y porcentaje de avance).
 */
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
        int ejerciciosCompletados,
        int porcentajeProgreso,
        List<DiaRutinaDTO> dias
) {
    /** Constructor de compatibilidad. */
    public RutinaResponseDTO(
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
    ) {
        this(id, nombre, objetivo, descripcion, clienteId, nombreCliente, entrenadorId, nombreEntrenador,
                entrenadorActivo, duracionSemanas, fechaInicio, fechaFin, fechaAsignacion, activa,
                totalDias, totalEjercicios, 0, 0, dias);
    }
}
