package com.fitcore.exception;

/**
 * Excepción lanzada cuando un recurso solicitado no existe en la base de datos.
 * Corresponde a HTTP 404 Not Found.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public RecursoNoEncontradoException(String entidad, Long id) {
        super(entidad + " con ID " + id + " no encontrado/a");
    }
}
