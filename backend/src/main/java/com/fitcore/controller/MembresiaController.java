package com.fitcore.controller;

import com.fitcore.model.Membresia;
import com.fitcore.service.MembresiaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/membresias")
@CrossOrigin(origins = "*")
public class MembresiaController {

    private final MembresiaService membresiaService;

    public MembresiaController(MembresiaService membresiaService) {
        this.membresiaService = membresiaService;
    }

    // GET - Listar todas las membresías
    @GetMapping
    public List<Membresia> listarTodas() {
        return membresiaService.listarTodas();
    }

    // GET - Buscar membresía por ID
    @GetMapping("/{id}")
    public ResponseEntity<Membresia> buscarPorId(@PathVariable Long id) {
        return membresiaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST - Registrar membresía
    @PostMapping
    public ResponseEntity<Membresia> registrar(
            @Valid @RequestBody Membresia membresia) {

        Membresia nuevaMembresia = membresiaService.registrar(membresia);

        return ResponseEntity.ok(nuevaMembresia);
    }

    // PUT - Actualizar membresía
    @PutMapping("/{id}")
    public ResponseEntity<Membresia> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody Membresia membresia) {

        Membresia actualizada = membresiaService.actualizar(id, membresia);

        return ResponseEntity.ok(actualizada);
    }

    // DELETE - Eliminar membresía
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {

        membresiaService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}