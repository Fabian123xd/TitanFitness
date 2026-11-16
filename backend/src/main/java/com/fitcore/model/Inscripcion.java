package com.fitcore.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Entidad que representa la contratación (inscripción) de un plan de membresía
 * por parte de un cliente.
 *
 * DIFERENCIA CONCEPTUAL IMPORTANTE:
 *   - Membresia (plan): define las condiciones del plan (precio, duración, nombre).
 *   - Inscripcion: registra la CONTRATACIÓN de ese plan por un cliente específico,
 *     con sus fechas de vigencia y estado actual.
 *
 * Un cliente puede tener múltiples inscripciones a lo largo del tiempo (historial).
 * Solo puede tener UNA inscripción en estado ACTIVA o PENDIENTE simultáneamente.
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

    /**
     * Cliente al que pertenece esta inscripción.
     * FetchType.LAZY para evitar N+1 en listados.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Cliente cliente;

    /**
     * Plan de membresía contratado en esta inscripción.
     * El plan puede seguir activo o inactivarse sin afectar inscripciones vigentes.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membresia_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Membresia membresia;

    /** Fecha en que inicia el período contratado. */
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    /**
     * Fecha en que vence el período contratado.
     * Calculada automáticamente: fechaInicio + membresia.duracionDias.
     */
    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    /**
     * Estado actual de la inscripción.
     *
     * Almacenado como STRING en BD para compatibilidad con datos históricos.
     * Flujo: PENDIENTE → ACTIVA → VENCIDA (automático) / CANCELADA (manual).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoInscripcion estado;

    /**
     * Fecha en que se registró la inscripción en el sistema.
     * Nullable para compatibilidad con inscripciones previas (sin esta columna).
     */
    @Column(name = "fecha_registro")
    private LocalDate fechaRegistro;

    /** Notas adicionales del administrador sobre esta inscripción. */
    @Column(length = 300)
    private String observaciones;

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
