package com.fitcore.controller;

import com.fitcore.model.Asistencia;
import com.fitcore.service.AsistenciaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asistencias")
@CrossOrigin(origins = "*")
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    public AsistenciaController(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    // GET - Listar todas las asistencias
    @GetMapping
    public List<Asistencia> listarTodas() {
        return asistenciaService.listarTodas();
    }

    // GET - Buscar asistencia por ID
    @GetMapping("/{id}")
    public ResponseEntity<Asistencia> buscarPorId(@PathVariable Long id) {
        return asistenciaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET - Historial de asistencias de un cliente
    @GetMapping("/cliente/{clienteId}")
    public List<Asistencia> listarPorCliente(
            @PathVariable Long clienteId) {

        return asistenciaService.listarPorCliente(clienteId);
    }

    // GET - Asistencias de una inscripción
    @GetMapping("/inscripcion/{inscripcionId}")
    public List<Asistencia> listarPorInscripcion(
            @PathVariable Long inscripcionId) {

        return asistenciaService.listarPorInscripcion(inscripcionId);
    }

    // POST - Registrar ingreso
    @PostMapping
    public ResponseEntity<Asistencia> registrar(
            @RequestParam Long inscripcionId) {

        Asistencia asistencia =
                asistenciaService.registrar(inscripcionId);

        return ResponseEntity.ok(asistencia);
    }

    // DELETE - Eliminar asistencia
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {

        asistenciaService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}