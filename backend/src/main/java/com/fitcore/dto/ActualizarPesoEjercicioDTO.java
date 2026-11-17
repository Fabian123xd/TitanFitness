package com.fitcore.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.PositiveOrZero;

/** DTO para actualizar el peso registrado en un ejercicio de rutina (0 a 1000 kg). */
public record ActualizarPesoEjercicioDTO(
        @PositiveOrZero(message = "El peso no puede ser negativo")
        @DecimalMax(value = "1000.0", message = "El peso no puede superar 1000 kg")
        Double pesoKg
) {}
