package com.fitcore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Día de entrenamiento dentro de una rutina; agrupa sus ejercicios.
 *
 * Incluye la restricción de unicidad (rutina_id, dia_semana) para impedir que
 * un mismo día de la semana se duplique dentro de una misma rutina.
 */
@Entity
@Table(
        name = "dias_rutina",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_dias_rutina_rutina_dia",
                columnNames = {"rutina_id", "dia_semana"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class DiaRutina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rutina_id", nullable = false)
    private Rutina rutina;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 10)
    private DiaSemana diaSemana;

    @Column(name = "nombre_dia", length = 100)
    private String nombreDia;

    @Column(nullable = false)
    private Integer orden = 1;

    @OneToMany(mappedBy = "dia", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<EjercicioRutina> ejercicios = new ArrayList<>();

    public void agregarEjercicio(EjercicioRutina ejercicio) {
        ejercicio.setDia(this);
        ejercicios.add(ejercicio);
    }
}
