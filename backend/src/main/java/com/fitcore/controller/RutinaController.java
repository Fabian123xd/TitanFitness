package com.fitcore.controller;

import com.fitcore.dto.ActualizarPesoEjercicioDTO;
import com.fitcore.dto.RutinaRequestDTO;
import com.fitcore.dto.RutinaResponseDTO;
import com.fitcore.service.RutinaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API REST de rutinas de entrenamiento y seguimiento del progreso de ejercicios.
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

    /** Detalle completo: días de entrenamiento, ejercicios y avance. */
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

    /**
     * Marca o desmarca un ejercicio como completado dentro de la rutina indicada
     * y devuelve la rutina actualizada con su nuevo porcentaje de progreso.
     */
    @RequestMapping(
            path = "/{rutinaId}/ejercicios/{ejercicioId}/toggle-completado",
            method = {RequestMethod.PATCH, RequestMethod.PUT}
    )
    public RutinaResponseDTO toggleEjercicioCompletado(
            @PathVariable Long rutinaId,
            @PathVariable Long ejercicioId) {
        return rutinaService.toggleEjercicioCompletado(rutinaId, ejercicioId);
    }

    /**
     * Actualiza el peso utilizado (kg) en un ejercicio de la rutina.
     */
    @RequestMapping(
            path = "/{rutinaId}/ejercicios/{ejercicioId}/peso",
            method = {RequestMethod.PATCH, RequestMethod.PUT}
    )
    public RutinaResponseDTO actualizarPesoEjercicio(
            @PathVariable Long rutinaId,
            @PathVariable Long ejercicioId,
            @Valid @RequestBody ActualizarPesoEjercicioDTO body) {
        return rutinaService.actualizarPesoEjercicio(
                rutinaId,
                ejercicioId,
                body != null ? body.pesoKg() : null
        );
    }
}
