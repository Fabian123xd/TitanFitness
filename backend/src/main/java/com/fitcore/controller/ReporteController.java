
package com.fitcore.controller;

import com.fitcore.repository.ClienteRepository;
import com.fitcore.repository.InscripcionRepository;
import com.fitcore.repository.PagoRepository;
import com.fitcore.repository.AsistenciaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
@CrossOrigin(origins = "*")
public class ReporteController {

    private final ClienteRepository clienteRepository;
    private final InscripcionRepository inscripcionRepository;
    private final PagoRepository pagoRepository;
    private final AsistenciaRepository asistenciaRepository;

    public ReporteController(
            ClienteRepository clienteRepository,
            InscripcionRepository inscripcionRepository,
            PagoRepository pagoRepository,
            AsistenciaRepository asistenciaRepository) {

        this.clienteRepository = clienteRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.pagoRepository = pagoRepository;
        this.asistenciaRepository = asistenciaRepository;
    }
     // Reporte de ingresos por rango de fechas
    @GetMapping("/ingresos/fechas")
    public Map<String, Object> obtenerIngresosPorFechas(
            @RequestParam LocalDate fechaInicio,
            @RequestParam LocalDate fechaFin) {

        if (fechaInicio.isAfter(fechaFin)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "La fecha inicial no puede ser posterior a la fecha final"
            );
        }

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.plusDays(1).atStartOfDay();

        BigDecimal ingresos = pagoRepository.calcularIngresosPorFechas(
                inicio, fin
        );

        return Map.of(
                "fechaInicio", fechaInicio,
                "fechaFin", fechaFin,
                "ingresosTotales", ingresos
        );
    }

    // Resumen general del gimnasio
    @GetMapping("/resumen")
    public Map<String, Long> obtenerResumen() {
        return Map.of(
                "totalClientes", clienteRepository.count(),
                "totalInscripciones", inscripcionRepository.count(),
                "totalPagos", pagoRepository.count(),
                "totalAsistencias", asistenciaRepository.count()
        );
    }

    // Reporte de ingresos totales
    @GetMapping("/ingresos")
    public Map<String, BigDecimal> obtenerIngresos() {
        BigDecimal total = pagoRepository.calcularIngresosTotales();

        return Map.of("ingresosTotales", total);
    }
}