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
import java.util.Map;

/**
 * Controlador REST para la gestión de inscripciones de membresías, activaciones,
 * cancelaciones y renovaciones con trazabilidad completa.
 */
@RestController
@RequestMapping("/api/inscripciones")
public class InscripcionController {

    private final InscripcionService inscripcionService;

    public InscripcionController(InscripcionService inscripcionService) {
        this.inscripcionService = inscripcionService;
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @GetMapping
    public List<InscripcionResponseDTO> listarTodas() {
        return inscripcionService.listarTodas();
    }

    @GetMapping("/{id}")
    public ResponseEntity<InscripcionResponseDTO> buscarPorId(@PathVariable Long id) {
        return inscripcionService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cliente/{clienteId}")
    public List<InscripcionResponseDTO> listarPorCliente(@PathVariable Long clienteId) {
        return inscripcionService.listarPorCliente(clienteId);
    }

    @GetMapping("/activas")
    public List<InscripcionResponseDTO> listarActivas() {
        return inscripcionService.listarActivas();
    }

    @GetMapping("/vencidas")
    public List<InscripcionResponseDTO> listarVencidas() {
        return inscripcionService.listarVencidas();
    }

    @GetMapping("/proximas-vencer")
    public List<InscripcionResponseDTO> listarProximasAVencer() {
        return inscripcionService.listarProximasAVencer();
    }

    @GetMapping("/resumen")
    public ResumenInscripcionesDTO obtenerResumen() {
        return inscripcionService.obtenerResumen();
    }

    // ── Operaciones de escritura ──────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<InscripcionResponseDTO> registrar(
            @Valid @RequestBody InscripcionRequestDTO request) {
        InscripcionResponseDTO inscripcion = inscripcionService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(inscripcion);
    }

    @PutMapping("/{id}/activar")
    public ResponseEntity<InscripcionResponseDTO> activar(@PathVariable Long id) {
        return ResponseEntity.ok(inscripcionService.activar(id));
    }

    /**
     * Cancela una inscripción PENDIENTE o ACTIVA, permitiendo especificar el motivo
     * por query param (?motivo=...) o cuerpo JSON ({"motivo": "..."}).
     */
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<InscripcionResponseDTO> cancelar(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo,
            @RequestBody(required = false) Map<String, String> body) {
        String motivoEfectivo = (motivo != null && !motivo.isBlank())
                ? motivo
                : (body != null ? body.get("motivo") : null);
        return ResponseEntity.ok(inscripcionService.cancelar(id, motivoEfectivo));
    }

    @PostMapping("/{id}/renovar")
    public ResponseEntity<InscripcionResponseDTO> renovar(
            @PathVariable Long id,
            @RequestParam(required = false) String observaciones) {
        InscripcionResponseDTO nueva = inscripcionService.renovar(id, observaciones);
        return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        inscripcionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
