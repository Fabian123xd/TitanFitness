package com.fitcore.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para todos los controladores REST.
 * Convierte excepciones de negocio y validación en respuestas HTTP estructuradas.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 404 — Recurso no encontrado */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> recursoNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    /** 400 — Regla de negocio violada */
    @ExceptionHandler(ReglaDeNegocioException.class)
    public ResponseEntity<ErrorResponse> reglaNegocio(ReglaDeNegocioException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BUSINESS_RULE", ex.getMessage()));
    }

    /** 400 — Errores de validación @Valid en @RequestBody */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacionCuerpo(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", mensaje));
    }

    /** 400 — Violaciones de constraints de bean validation */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> constraintViolation(ConstraintViolationException ex) {
        String mensaje = ex.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("CONSTRAINT_VIOLATION", mensaje));
    }

    /** 409 — Violación de integridad referencial (clave foránea, unicidad) */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "INTEGRITY_ERROR",
                        "Operación no permitida: el registro está relacionado con otros datos del sistema"));
    }

    /** 400 — IllegalStateException e IllegalArgumentException heredadas */
    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> estadoInvalido(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_STATE", ex.getMessage()));
    }

    /** Respeta el código HTTP de ResponseStatusException (409 por DNI/correo duplicado, etc.) */
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> estadoHttp(org.springframework.web.server.ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        String codigo = status != null ? status.name() : "ERROR";
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(new ErrorResponse(codigo, ex.getReason() != null ? ex.getReason() : codigo));
    }

    /** 400 — JSON mal formado, fechas o enumerados inválidos */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cuerpoIlegible(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("MALFORMED_REQUEST",
                        "El cuerpo de la solicitud es inválido (revise fechas, números y valores permitidos)"));
    }

    /** 400 — Parámetro de ruta con tipo incorrecto */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> tipoIncorrecto(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_PARAMETER", "Parámetro inválido: " + ex.getName()));
    }

    /** 404 — Ruta inexistente */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> rutaInexistente(
            org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", "Recurso no encontrado"));
    }

    /** 500 — Error interno no controlado */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> general(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "INTERNAL_ERROR",
                        "Error interno del servidor. Contacte al administrador."));
    }
}
