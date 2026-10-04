package com.fitcore.controller;

import com.fitcore.model.Inscripcion;
import com.fitcore.service.InscripcionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inscripciones")
@CrossOrigin(origins = "*")
public class InscripcionController {

    private final InscripcionService inscripcionService;

    public InscripcionController(InscripcionService inscripcionService) {
        this.inscripcionService = inscripcionService;
    }

    // GET - Listar todas las inscripciones
    @GetMapping
    public List<Inscripcion> listarTodas() {
        return inscripcionService.listarTodas();
    }

    // GET - Buscar inscripción por ID
    @GetMapping("/{id}")
    public ResponseEntity<Inscripcion> buscarPorId(@PathVariable Long id) {
        return inscripcionService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET - Listar inscripciones de un cliente
    @GetMapping("/cliente/{clienteId}")
    public List<Inscripcion> listarPorCliente(
            @PathVariable Long clienteId) {

        return inscripcionService.listarPorCliente(clienteId);
    }

    // POST - Asignar una membresía a un cliente
    @PostMapping
    public ResponseEntity<Inscripcion> registrar(
            @RequestParam Long clienteId,
            @RequestParam Long membresiaId) {

        Inscripcion inscripcion =
                inscripcionService.registrar(clienteId, membresiaId);

        return ResponseEntity.ok(inscripcion);
    }

    // DELETE - Eliminar inscripción
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {

        inscripcionService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}