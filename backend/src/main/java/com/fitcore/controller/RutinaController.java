package com.fitcore.controller;

import com.fitcore.model.Rutina;
import com.fitcore.service.RutinaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rutinas")
@CrossOrigin(origins = "*")
public class RutinaController {

    private final RutinaService rutinaService;

    public RutinaController(RutinaService rutinaService) {
        this.rutinaService = rutinaService;
    }

    // Listar todas las rutinas
    @GetMapping
    public List<Rutina> listarTodas() {
        return rutinaService.listarTodas();
    }

    // Buscar rutina por ID
    @GetMapping("/{id}")
    public ResponseEntity<Rutina> buscarPorId(@PathVariable Long id) {
        return rutinaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Rutinas de un cliente
    @GetMapping("/cliente/{clienteId}")
    public List<Rutina> listarPorCliente(
            @PathVariable Long clienteId) {

        return rutinaService.listarPorCliente(clienteId);
    }

    // Rutinas activas de un cliente
    @GetMapping("/cliente/{clienteId}/activas")
    public List<Rutina> listarActivasPorCliente(
            @PathVariable Long clienteId) {

        return rutinaService.listarActivasPorCliente(clienteId);
    }

    // Rutinas asignadas por un entrenador
    @GetMapping("/entrenador/{entrenadorId}")
    public List<Rutina> listarPorEntrenador(
            @PathVariable Long entrenadorId) {

        return rutinaService.listarPorEntrenador(entrenadorId);
    }

    // Registrar rutina
    @PostMapping
    public ResponseEntity<Rutina> registrar(
            @RequestParam Long clienteId,
            @RequestParam Long entrenadorId,
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion) {

        Rutina rutina = rutinaService.registrar(
                clienteId,
                entrenadorId,
                nombre,
                descripcion
        );

        return ResponseEntity.ok(rutina);
    }

    // Desactivar rutina
    @PutMapping("/{id}/desactivar")
    public ResponseEntity<Rutina> desactivar(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                rutinaService.desactivar(id)
        );
    }
}