package com.fitcore.controller;

import com.fitcore.model.Pago;
import com.fitcore.service.PagoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/pagos")
@CrossOrigin(origins = "*")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    // GET - Listar todos los pagos
    @GetMapping
    public List<Pago> listarTodos() {
        return pagoService.listarTodos();
    }

    // GET - Buscar pago por ID
    @GetMapping("/{id}")
    public ResponseEntity<Pago> buscarPorId(@PathVariable Long id) {
        return pagoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET - Pagos de una inscripción
    @GetMapping("/inscripcion/{inscripcionId}")
    public List<Pago> listarPorInscripcion(
            @PathVariable Long inscripcionId) {

        return pagoService.listarPorInscripcion(inscripcionId);
    }

    // GET - Historial de pagos de un cliente
    @GetMapping("/cliente/{clienteId}")
    public List<Pago> listarPorCliente(
            @PathVariable Long clienteId) {

        return pagoService.listarPorCliente(clienteId);
    }

    // POST - Registrar pago
    @PostMapping
    public ResponseEntity<Pago> registrar(
            @RequestParam Long inscripcionId,
            @RequestParam BigDecimal monto,
            @RequestParam String metodoPago) {

        Pago pago = pagoService.registrar(
                inscripcionId,
                monto,
                metodoPago
        );

        return ResponseEntity.ok(pago);
    }

    // DELETE - Eliminar pago
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {

        pagoService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}