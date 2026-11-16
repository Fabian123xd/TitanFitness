package com.fitcore.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Datos para registrar o actualizar una rutina (incluye sus días y ejercicios). */
public record RutinaRequestDTO(
        @NotBlank(message = "El nombre de la rutina es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @NotBlank(message = "El objetivo de la rutina es obligatorio")
        @Size(max = 200, message = "El objetivo no puede superar 200 caracteres")
        String objetivo,

        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String descripcion,

        @NotNull(message = "El cliente es obligatorio")
        Long clienteId,

        @NotNull(message = "El entrenador es obligatorio")
        Long entrenadorId,

        @Min(value = 1, message = "La duración mínima es 1 semana")
        @Max(value = 52, message = "La duración máxima es 52 semanas")
        Integer duracionSemanas,

        LocalDate fechaInicio,

        List<@Valid DiaRutinaDTO> dias
) {}
