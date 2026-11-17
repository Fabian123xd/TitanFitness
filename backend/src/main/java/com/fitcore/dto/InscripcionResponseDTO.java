package com.fitcore.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de salida que representa una inscripción de membresía con su trazabilidad
 * completa (inscripción de origen, fecha de activación, fecha y motivo de cancelación).
 */
public record InscripcionResponseDTO(
        Long id,
        Long clienteId,
        String nombreCliente,
        Long membresiaId,
        String nombreMembresia,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        LocalDate fechaRegistro,
        String estado,
        String observaciones,
        long diasRestantes,
        long diasHabilesRestantes,
        boolean proximaAVencer,
        boolean vencida,
        Long inscripcionOrigenId,
        LocalDateTime fechaActivacion,
        LocalDateTime fechaCancelacion,
        String motivoCancelacion
) {
    /** Constructor de compatibilidad de 14 parámetros. */
    public InscripcionResponseDTO(
            Long id,
            Long clienteId,
            String nombreCliente,
            Long membresiaId,
            String nombreMembresia,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            LocalDate fechaRegistro,
            String estado,
            String observaciones,
            long diasRestantes,
            long diasHabilesRestantes,
            boolean proximaAVencer,
            boolean vencida
    ) {
        this(id, clienteId, nombreCliente, membresiaId, nombreMembresia,
                fechaInicio, fechaFin, fechaRegistro, estado, observaciones,
                diasRestantes, diasHabilesRestantes, proximaAVencer, vencida,
                null, null, null, null);
    }
}
