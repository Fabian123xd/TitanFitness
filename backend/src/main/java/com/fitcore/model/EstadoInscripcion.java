package com.fitcore.model;

/**
 * Estados posibles de una inscripción de membresía.
 *
 * PENDIENTE  → Inscripción registrada, pendiente de activación por el administrador.
 * ACTIVA     → Inscripción vigente y activa (dentro del período contratado).
 * VENCIDA    → Fecha de fin superada sin renovación.
 * CANCELADA  → Cancelada manualmente por el administrador.
 *
 * IMPORTANTE: Los valores "PENDIENTE" y "ACTIVA" son compatibles con los datos
 * históricos existentes en la base de datos (campo estado VARCHAR(20)).
 */
public enum EstadoInscripcion {
    PENDIENTE,
    ACTIVA,
    VENCIDA,
    CANCELADA
}
