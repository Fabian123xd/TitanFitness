package com.fitcore.exception;

import java.time.LocalDateTime;

/**
 * Estructura estándar de respuesta de error para la API REST.
 * Serializada como JSON en todas las respuestas de error.
 */
public record ErrorResponse(
        String codigo,
        String mensaje,
        LocalDateTime timestamp
) {
    /** Constructor conveniente que agrega timestamp automáticamente. */
    public ErrorResponse(String codigo, String mensaje) {
        this(codigo, mensaje, LocalDateTime.now());
    }
}
