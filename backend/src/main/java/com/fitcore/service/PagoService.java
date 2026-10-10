
package com.fitcore.service;

import com.fitcore.model.Inscripcion;
import com.fitcore.model.Pago;
import com.fitcore.repository.InscripcionRepository;
import com.fitcore.repository.PagoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class PagoService {

    private final PagoRepository pagoRepository;
    private final InscripcionRepository inscripcionRepository;

    public PagoService(
            PagoRepository pagoRepository,
            InscripcionRepository inscripcionRepository) {

        this.pagoRepository = pagoRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    // Listar todos los pagos
    public List<Pago> listarTodos() {
        return pagoRepository.findAll();
    }

    // Buscar pago por ID
    public Optional<Pago> buscarPorId(Long id) {
        return pagoRepository.findById(id);
    }

    // Listar pagos de una inscripción
    public List<Pago> listarPorInscripcion(Long inscripcionId) {
        return pagoRepository.findByInscripcionId(inscripcionId);
    }

    // Listar pagos de un cliente
    public List<Pago> listarPorCliente(Long clienteId) {
        return pagoRepository.findByInscripcionClienteId(clienteId);
    }

    // Registrar un pago
    @Transactional
    public Pago registrar(
            Long inscripcionId,
            BigDecimal monto,
            String metodoPago,
            BigDecimal montoRecibido) {

        // Buscar inscripción
        Inscripcion inscripcion = inscripcionRepository
                .findById(inscripcionId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Inscripción no encontrada"
                        ));

        // Validar estado de la inscripción
        if (!"PENDIENTE".equals(inscripcion.getEstado())) {
            throw new IllegalStateException(
                    "Solo se pueden pagar inscripciones pendientes"
            );
        }

        // Obtener precio de la membresía
        BigDecimal precio = inscripcion.getMembresia().getPrecio();

        // Validar monto
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El monto debe ser mayor a cero"
            );
        }

        if (monto.compareTo(precio) != 0) {
            throw new IllegalArgumentException(
                    "El monto debe ser exactamente S/ " + precio
            );
        }

        // Validar método de pago
        if (metodoPago == null || metodoPago.isBlank()) {
            throw new IllegalArgumentException(
                    "El método de pago es obligatorio"
            );
        }

        String metodo = metodoPago.trim().toUpperCase(Locale.ROOT);

        if (!List.of(
                "EFECTIVO",
                "YAPE",
                "PLIN",
                "TRANSFERENCIA",
                "TARJETA"
        ).contains(metodo)) {
            throw new IllegalArgumentException(
                    "Método de pago no permitido"
            );
        }

        // Calcular vuelto
        BigDecimal vuelto = BigDecimal.ZERO;

        if ("EFECTIVO".equals(metodo)) {

            if (montoRecibido == null ||
                    montoRecibido.compareTo(precio) < 0) {
                throw new IllegalArgumentException(
                        "El efectivo recibido es insuficiente"
                );
            }

            vuelto = montoRecibido.subtract(precio);

        } else {

            // Pagos digitales: importe exacto
            if (montoRecibido != null &&
                    montoRecibido.compareTo(precio) != 0) {
                throw new IllegalArgumentException(
                        "El monto recibido debe coincidir con el precio"
                );
            }

            montoRecibido = precio;
        }

        // Crear pago
        Pago pago = new Pago();
        pago.setInscripcion(inscripcion);
        pago.setMonto(precio);
        pago.setMontoRecibido(montoRecibido);
        pago.setVuelto(vuelto);
        pago.setMetodoPago(metodo);
        pago.setEstado("PAGADO");

        // Guardar pago
        Pago pagoGuardado = pagoRepository.save(pago);

        // Activar inscripción
        inscripcion.setEstado("ACTIVA");
        inscripcionRepository.save(inscripcion);

        return pagoGuardado;
    }

    // Eliminar pago
    public void eliminar(Long id) {

        if (!pagoRepository.existsById(id)) {
            throw new IllegalArgumentException(
                    "Pago no encontrado"
            );
        }

        pagoRepository.deleteById(id);
    }
}
