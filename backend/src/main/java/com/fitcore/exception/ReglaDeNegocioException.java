package com.fitcore.exception;

/**
 * Excepción lanzada cuando una operación viola una regla de negocio del sistema.
 * Corresponde a HTTP 400 Bad Request.
 *
 * Ejemplos: inscripción duplicada, entrenador inactivo, estado inválido para renovación.
 */
public class ReglaDeNegocioException extends RuntimeException {

    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
