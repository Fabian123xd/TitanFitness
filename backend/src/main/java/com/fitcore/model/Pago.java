package com.fitcore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private Inscripcion inscripcion;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;
    @Column(name = "monto_recibido", precision = 10, scale = 2)
private BigDecimal montoRecibido;

@Column(name = "vuelto", precision = 10, scale = 2)
private BigDecimal vuelto;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago;

    @Column(name = "metodo_pago", nullable = false, length = 30)
    private String metodoPago;

    @Column(nullable = false, length = 20)
    private String estado;

    public Pago() {
    }

    @PrePersist
    public void prePersist() {
        if (fechaPago == null) {
            fechaPago = LocalDateTime.now();
        }

        if (estado == null) {
            estado = "PAGADO";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Inscripcion getInscripcion() {
        return inscripcion;
    }

    public void setInscripcion(Inscripcion inscripcion) {
        this.inscripcion = inscripcion;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
    public BigDecimal getMontoRecibido() {
    return montoRecibido;
}

public void setMontoRecibido(BigDecimal montoRecibido) {
    this.montoRecibido = montoRecibido;
}

public BigDecimal getVuelto() {
    return vuelto;
}

public void setVuelto(BigDecimal vuelto) {
    this.vuelto = vuelto;
}
}