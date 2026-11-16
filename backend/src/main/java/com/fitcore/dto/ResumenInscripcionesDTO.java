package com.fitcore.dto;

/**
 * DTO con contadores de inscripciones para el dashboard.
 *
 * @param activas          Inscripciones en estado ACTIVA con fechaFin >= hoy
 * @param pendientes       Inscripciones en estado PENDIENTE
 * @param proximasAVencer  Inscripciones ACTIVA con ≤ 7 días hábiles para vencer
 * @param vencidas         Inscripciones cuya fechaFin < hoy (estado ACTIVA o VENCIDA)
 * @param canceladas       Inscripciones en estado CANCELADA
 * @param total            Total de inscripciones registradas
 */
public record ResumenInscripcionesDTO(
        long activas,
        long pendientes,
        long proximasAVencer,
        long vencidas,
        long canceladas,
        long total
) {}
