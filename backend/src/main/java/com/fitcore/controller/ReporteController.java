package com.fitcore.controller;

import com.fitcore.service.ReporteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador de indicadores operativos para el Dashboard.
 * Delega la lógica en {@link ReporteService} respetando la arquitectura por capas.
 */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/resumen")
    public Map<String, Long> obtenerResumen() {
        return reporteService.obtenerResumenDashboard();
    }
}
