package com.fitcore.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO de entrada para crear una nueva inscripción de membresía.
 *
 * @param clienteId     ID del cliente (obligatorio)
 * @param membresiaId   ID del plan de membresía (obligatorio)
 * @param fechaInicio   Fecha de inicio; si es null se usa la fecha actual
 * @param observaciones Notas opcionales del administrador
 */
public record InscripcionRequestDTO(

        @NotNull(message = "El ID del cliente es obligatorio")
        Long clienteId,

        @NotNull(message = "El ID de la membresía es obligatorio")
        Long membresiaId,

        LocalDate fechaInicio,

        @Size(max = 300, message = "Las observaciones no pueden superar los 300 caracteres")
        String observaciones
) {}
