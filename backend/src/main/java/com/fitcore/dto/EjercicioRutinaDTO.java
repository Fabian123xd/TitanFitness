package com.fitcore.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Ejercicio de un día de rutina (entrada y salida). */
public record EjercicioRutinaDTO(
        Long id,

        @NotBlank(message = "El nombre del ejercicio es obligatorio")
        @Size(max = 100, message = "El nombre del ejercicio no puede superar 100 caracteres")
        String nombre,

        @NotNull(message = "Las series son obligatorias")
        @Min(value = 1, message = "Las series deben ser al menos 1")
        @Max(value = 20, message = "Las series no pueden superar 20")
        Integer series,

        @NotNull(message = "Las repeticiones son obligatorias")
        @Min(value = 1, message = "Las repeticiones deben ser al menos 1")
        @Max(value = 200, message = "Las repeticiones no pueden superar 200")
        Integer repeticiones,

        @Min(value = 0, message = "El descanso no puede ser negativo")
        @Max(value = 3600, message = "El descanso no puede superar 3600 segundos")
        Integer descansoSeg,

        @Size(max = 300, message = "Las notas no pueden superar 300 caracteres")
        String notas,

        Integer orden
) {}
