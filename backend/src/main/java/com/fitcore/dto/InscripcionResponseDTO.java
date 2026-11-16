package com.fitcore.dto;

import java.time.LocalDate;

/**
 * DTO de salida que representa una inscripción de membresía.
 *
 * Incluye datos calculados como diasRestantes, proximaAVencer y vencida
 * para facilitar el renderizado en el frontend sin cálculos adicionales.
 *
 * @param id                ID de la inscripción
 * @param clienteId         ID del cliente
 * @param nombreCliente     Nombre completo del cliente
 * @param membresiaId       ID del plan de membresía
 * @param nombreMembresia   Nombre del plan de membresía
 * @param fechaInicio       Fecha de inicio del período
 * @param fechaFin          Fecha de vencimiento
 * @param fechaRegistro     Fecha en que se registró la inscripción
 * @param estado            Estado actual (PENDIENTE/ACTIVA/VENCIDA/CANCELADA)
 * @param observaciones     Notas del administrador
 * @param diasRestantes     Días calendario hasta el vencimiento (0 si ya venció)
 * @param diasHabilesRestantes Días hábiles hasta el vencimiento (excluye sáb/dom)
 * @param proximaAVencer    true si quedan ≤ 7 días hábiles para vencer
 * @param vencida           true si fechaFin < hoy
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
        boolean vencida
) {}
