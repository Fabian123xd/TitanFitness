package com.fitcore.controller;

import com.fitcore.dto.RutinaRequestDTO;
import com.fitcore.dto.RutinaResponseDTO;
import com.fitcore.service.RutinaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de rutinas. Una rutina se guarda junto con sus días y ejercicios
 * (POST/PUT con cuerpo JSON).
 */
@RestController
@RequestMapping("/api/rutinas")
public class RutinaController {

    private final RutinaService rutinaService;

    public RutinaController(RutinaService rutinaService) {
        this.rutinaService = rutinaService;
    }

    @GetMapping
    public List<RutinaResponseDTO> listarTodas() {
        return rutinaService.listarTodas();
    }

    /** Detalle completo: días de entrenamiento y ejercicios. */
    @GetMapping("/{id}")
    public RutinaResponseDTO buscarPorId(@PathVariable Long id) {
        return rutinaService.buscarPorId(id);
    }

    /** Historial de rutinas de un cliente. */
    @GetMapping("/cliente/{clienteId}")
    public List<RutinaResponseDTO> listarPorCliente(@PathVariable Long clienteId) {
        return rutinaService.listarPorCliente(clienteId);
    }

    @GetMapping("/cliente/{clienteId}/activas")
    public List<RutinaResponseDTO> listarActivasPorCliente(@PathVariable Long clienteId) {
        return rutinaService.listarActivasPorCliente(clienteId);
    }

    @GetMapping("/entrenador/{entrenadorId}")
    public List<RutinaResponseDTO> listarPorEntrenador(@PathVariable Long entrenadorId) {
        return rutinaService.listarPorEntrenador(entrenadorId);
    }

    @PostMapping
    public ResponseEntity<RutinaResponseDTO> registrar(@Valid @RequestBody RutinaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rutinaService.registrar(request));
    }

    @PutMapping("/{id}")
    public RutinaResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody RutinaRequestDTO request) {
        return rutinaService.actualizar(id, request);
    }

    @PutMapping("/{id}/desactivar")
    public RutinaResponseDTO desactivar(@PathVariable Long id) {
        return rutinaService.desactivar(id);
    }

    @PutMapping("/{id}/reactivar")
    public RutinaResponseDTO reactivar(@PathVariable Long id) {
        return rutinaService.reactivar(id);
    }
}
