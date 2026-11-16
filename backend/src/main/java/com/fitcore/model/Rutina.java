package com.fitcore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Rutina de entrenamiento asignada a un cliente por un entrenador.
 * Un cliente puede tener muchas rutinas a lo largo del tiempo (historial) y
 * un entrenador puede ser responsable de muchas rutinas.
 */
@Entity
@Table(name = "rutinas")
@Getter
@Setter
@NoArgsConstructor
public class Rutina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 200)
    private String objetivo;

    @Column(length = 500)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrenador_id", nullable = false)
    private Entrenador entrenador;

    @Column(name = "duracion_semanas")
    private Integer duracionSemanas;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDate fechaAsignacion;

    @Column(nullable = false)
    private Boolean activa = true;

    @OneToMany(mappedBy = "rutina", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<DiaRutina> dias = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (fechaAsignacion == null) {
            fechaAsignacion = LocalDate.now();
        }
        if (activa == null) {
            activa = true;
        }
    }

    public void agregarDia(DiaRutina dia) {
        dia.setRutina(this);
        dias.add(dia);
    }
}
