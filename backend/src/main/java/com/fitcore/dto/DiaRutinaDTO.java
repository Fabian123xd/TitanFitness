package com.fitcore.dto;

import com.fitcore.model.DiaSemana;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Día de entrenamiento con sus ejercicios (entrada y salida). */
public record DiaRutinaDTO(
        Long id,

        @NotNull(message = "El día de la semana es obligatorio")
        DiaSemana diaSemana,

        @Size(max = 100, message = "El nombre del día no puede superar 100 caracteres")
        String nombreDia,

        Integer orden,

        List<@Valid EjercicioRutinaDTO> ejercicios
) {}
