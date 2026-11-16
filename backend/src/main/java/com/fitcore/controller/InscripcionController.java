package com.fitcore.controller;

import com.fitcore.dto.InscripcionRequestDTO;
import com.fitcore.dto.InscripcionResponseDTO;
import com.fitcore.dto.ResumenInscripcionesDTO;
import com.fitcore.service.InscripcionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de inscripciones de membresías.
 *
 * Base URL: /api/inscripciones
 *
 * FLUJO DE ESTADOS:
 *   POST /          → crea inscripción (PENDIENTE)
 *   PUT /{id}/activar  → admin activa (ACTIVA)   ← Reemplaza flujo de pago
 *   PUT /{id}/cancelar → cancela (CANCELADA)
 *   POST /{id}/renovar → crea nueva inscripción (nueva PENDIENTE)
 */
@RestController
@RequestMapping("/api/inscripciones")
public class InscripcionController {

    private final InscripcionService inscripcionService;

    public InscripcionController(InscripcionService inscripcionService) {
        this.inscripcionService = inscripcionService;
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    /** Lista todas las inscripciones. */
    @GetMapping
    public List<InscripcionResponseDTO> listarTodas() {
        return inscripcionService.listarTodas();
    }

    /** Busca inscripción por ID. */
    @GetMapping("/{id}")
    public ResponseEntity<InscripcionResponseDTO> buscarPorId(@PathVariable Long id) {
        return inscripcionService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Historial de inscripciones de un cliente específico. */
    @GetMapping("/cliente/{clienteId}")
    public List<InscripcionResponseDTO> listarPorCliente(@PathVariable Long clienteId) {
        return inscripcionService.listarPorCliente(clienteId);
    }

    /** Solo inscripciones vigentes (ACTIVA y fechaFin >= hoy). */
    @GetMapping("/activas")
    public List<InscripcionResponseDTO> listarActivas() {
        return inscripcionService.listarActivas();
    }

    /** Inscripciones vencidas (ACTIVA con fechaFin < hoy, o estado VENCIDA). */
    @GetMapping("/vencidas")
    public List<InscripcionResponseDTO> listarVencidas() {
        return inscripcionService.listarVencidas();
    }

    /**
     * Inscripciones ACTIVAS que vencen en los próximos 7 días hábiles.
     * Usado para las alertas del dashboard.
     */
    @GetMapping("/proximas-vencer")
    public List<InscripcionResponseDTO> listarProximasAVencer() {
        return inscripcionService.listarProximasAVencer();
    }

    /** Contadores consolidados para el dashboard. */
    @GetMapping("/resumen")
    public ResumenInscripcionesDTO obtenerResumen() {
        return inscripcionService.obtenerResumen();
    }

    // ── Operaciones de escritura ──────────────────────────────────────────────

    /**
     * Crea una nueva inscripción (en estado PENDIENTE).
     * Requiere clienteId y membresiaId. La fechaFin se calcula automáticamente.
     */
    @PostMapping
    public ResponseEntity<InscripcionResponseDTO> registrar(
            @Valid @RequestBody InscripcionRequestDTO request) {
        InscripcionResponseDTO inscripcion = inscripcionService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(inscripcion);
    }

    /**
     * Activa una inscripción en estado PENDIENTE.
     *
     * IMPORTANTE: Este endpoint reemplaza la dependencia del módulo de pagos.
     * El administrador activa la inscripción directamente desde la interfaz.
     */
    @PutMapping("/{id}/activar")
    public ResponseEntity<InscripcionResponseDTO> activar(@PathVariable Long id) {
        return ResponseEntity.ok(inscripcionService.activar(id));
    }

    /** Cancela una inscripción PENDIENTE o ACTIVA. */
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<InscripcionResponseDTO> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(inscripcionService.cancelar(id));
    }

    /**
     * Renueva una inscripción creando una nueva a continuación.
     * La inscripción original se conserva (historial intacto).
     */
    @PostMapping("/{id}/renovar")
    public ResponseEntity<InscripcionResponseDTO> renovar(
            @PathVariable Long id,
            @RequestParam(required = false) String observaciones) {
        InscripcionResponseDTO nueva = inscripcionService.renovar(id, observaciones);
        return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
    }

    /** Elimina una inscripción por ID. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        inscripcionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
