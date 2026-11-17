package com.fitcore.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad que representa la contratación (inscripción) de un plan de membresía
 * por parte de un cliente, incluyendo trazabilidad de renovaciones, activaciones
 * y cancelaciones.
 */
@Entity
@Table(name = "inscripciones")
@Getter
@Setter
@NoArgsConstructor
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membresia_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Membresia membresia;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoInscripcion estado;

    @Column(name = "fecha_registro")
    private LocalDate fechaRegistro;

    @Column(length = 300)
    private String observaciones;

    /** ID de la inscripción anterior que dio origen a esta renovación (null si es inscripción inicial). */
    @Column(name = "inscripcion_origen_id")
    private Long inscripcionOrigenId;

    /** Fecha y hora exacta en que el administrador activó la inscripción. */
    @Column(name = "fecha_activacion")
    private LocalDateTime fechaActivacion;

    /** Fecha y hora exacta en que se canceló la inscripción. */
    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    /** Motivo registrado al cancelar la inscripción. */
    @Column(name = "motivo_cancelacion", length = 300)
    private String motivoCancelacion;

    @PrePersist
    public void prePersist() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDate.now();
        }
        if (estado == null) {
            estado = EstadoInscripcion.PENDIENTE;
        }
    }
}
