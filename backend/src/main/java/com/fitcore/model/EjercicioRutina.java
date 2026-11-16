package com.fitcore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ejercicio de un día de entrenamiento (series, repeticiones y descanso). */
@Entity
@Table(name = "ejercicios_rutina")
@Getter
@Setter
@NoArgsConstructor
public class EjercicioRutina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dia_id", nullable = false)
    private DiaRutina dia;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private Integer series;

    @Column(nullable = false)
    private Integer repeticiones;

    @Column(name = "descanso_seg")
    private Integer descansoSeg;

    @Column(length = 300)
    private String notas;

    @Column(nullable = false)
    private Integer orden = 1;
}
