package com.fitcore.controller;

import com.fitcore.model.Membresia;
import com.fitcore.service.MembresiaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de planes de membresía.
 *
 * Base URL: /api/membresias
 *
 * NOTA: "Membresía" en este contexto es el PLAN (mensual, trimestral, etc.),
 * no la contratación individual de un cliente (eso es "Inscripción").
 */
@RestController
@RequestMapping("/api/membresias")
public class MembresiaController {

    private final MembresiaService membresiaService;

    public MembresiaController(MembresiaService membresiaService) {
        this.membresiaService = membresiaService;
    }

    /** Lista todos los planes (activos e inactivos). */
    @GetMapping
    public List<Membresia> listarTodas() {
        return membresiaService.listarTodas();
    }

    /** Lista solo los planes activos (para selector en nueva inscripción). */
    @GetMapping("/activas")
    public List<Membresia> listarActivas() {
        return membresiaService.listarActivas();
    }

    /** Busca un plan por ID. */
    @GetMapping("/{id}")
    public ResponseEntity<Membresia> buscarPorId(@PathVariable Long id) {
        return membresiaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Registra un nuevo plan de membresía. */
    @PostMapping
    public ResponseEntity<Membresia> registrar(@Valid @RequestBody Membresia membresia) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(membresiaService.registrar(membresia));
    }

    /** Actualiza un plan existente. */
    @PutMapping("/{id}")
    public ResponseEntity<Membresia> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody Membresia membresia) {
        return ResponseEntity.ok(membresiaService.actualizar(id, membresia));
    }

    /**
     * Activa o desactiva un plan (toggle).
     * Desactivar no afecta inscripciones vigentes, solo impide nuevas.
     */
    @PutMapping("/{id}/toggle")
    public ResponseEntity<Membresia> toggleActivo(@PathVariable Long id) {
        return ResponseEntity.ok(membresiaService.toggleActivo(id));
    }

    /** Elimina un plan. Falla si tiene inscripciones asociadas. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        membresiaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
