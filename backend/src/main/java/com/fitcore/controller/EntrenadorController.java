package com.fitcore.controller;

import com.fitcore.model.Entrenador;
import com.fitcore.service.EntrenadorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entrenadores")
@CrossOrigin(origins = "*")
public class EntrenadorController {

    private final EntrenadorService entrenadorService;

    public EntrenadorController(EntrenadorService entrenadorService) {
        this.entrenadorService = entrenadorService;
    }

    @GetMapping
    public List<Entrenador> listarTodos() {
        return entrenadorService.listarTodos();
    }

    @GetMapping("/activos")
    public List<Entrenador> listarActivos() {
        return entrenadorService.listarActivos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Entrenador> buscarPorId(@PathVariable Long id) {
        return entrenadorService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Entrenador> registrar(
            @Valid @RequestBody Entrenador entrenador) {

        return ResponseEntity.ok(
                entrenadorService.registrar(entrenador)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Entrenador> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody Entrenador entrenador) {

        return ResponseEntity.ok(
                entrenadorService.actualizar(id, entrenador)
        );
    }

    @PutMapping("/{id}/desactivar")
    public ResponseEntity<Entrenador> desactivar(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                entrenadorService.desactivar(id)
        );
    }
}