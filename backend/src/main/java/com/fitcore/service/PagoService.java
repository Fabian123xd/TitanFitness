package com.fitcore.service;

import com.fitcore.model.Inscripcion;
import com.fitcore.model.Pago;
import com.fitcore.repository.InscripcionRepository;
import com.fitcore.repository.PagoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
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
    public Pago registrar(
            Long inscripcionId,
            BigDecimal monto,
            String metodoPago) {

        Inscripcion inscripcion = inscripcionRepository
                .findById(inscripcionId)
                .orElseThrow(() ->
                        new RuntimeException("Inscripción no encontrada"));

        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El monto debe ser mayor a 0");
        }

        if (metodoPago == null || metodoPago.isBlank()) {
            throw new RuntimeException("El método de pago es obligatorio");
        }

        Pago pago = new Pago();

        pago.setInscripcion(inscripcion);
        pago.setMonto(monto);
        pago.setMetodoPago(metodoPago.toUpperCase());
        pago.setEstado("PAGADO");

        return pagoRepository.save(pago);
    }

    // Eliminar pago
    public void eliminar(Long id) {

        if (!pagoRepository.existsById(id)) {
            throw new RuntimeException("Pago no encontrado");
        }

        pagoRepository.deleteById(id);
    }
}