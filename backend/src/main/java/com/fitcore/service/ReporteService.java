package com.fitcore.service;

import com.fitcore.repository.ClienteRepository;
import com.fitcore.repository.EntrenadorRepository;
import com.fitcore.repository.MembresiaRepository;
import com.fitcore.repository.RutinaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Servicio que consolida los indicadores operativos para el Dashboard.
 *
 * Respeta la arquitectura por capas (Controller -> Service -> Repository),
 * evitando que ReporteController acceda directamente a los repositorios JPA.
 */
@Service
@Transactional(readOnly = true)
public class ReporteService {

    private final ClienteRepository clienteRepository;
    private final MembresiaRepository membresiaRepository;
    private final EntrenadorRepository entrenadorRepository;
    private final RutinaRepository rutinaRepository;
    private final InscripcionService inscripcionService;

    public ReporteService(ClienteRepository clienteRepository,
                          MembresiaRepository membresiaRepository,
                          EntrenadorRepository entrenadorRepository,
                          RutinaRepository rutinaRepository,
                          InscripcionService inscripcionService) {
        this.clienteRepository = clienteRepository;
        this.membresiaRepository = membresiaRepository;
        this.entrenadorRepository = entrenadorRepository;
        this.rutinaRepository = rutinaRepository;
        this.inscripcionService = inscripcionService;
    }

    public Map<String, Long> obtenerResumenDashboard() {
        var inscripciones = inscripcionService.obtenerResumen();
        Map<String, Long> resumen = new LinkedHashMap<>();
        resumen.put("totalClientes", clienteRepository.count());
        resumen.put("totalPlanes", membresiaRepository.count());
        resumen.put("planesActivos", (long) membresiaRepository.findByActivoTrue().size());
        resumen.put("inscripcionesActivas", inscripciones.activas());
        resumen.put("inscripcionesPendientes", inscripciones.pendientes());
        resumen.put("inscripcionesProximasAVencer", inscripciones.proximasAVencer());
        resumen.put("inscripcionesVencidas", inscripciones.vencidas());
        resumen.put("inscripcionesCanceladas", inscripciones.canceladas());
        resumen.put("totalEntrenadores", entrenadorRepository.count());
        resumen.put("entrenadoresActivos", (long) entrenadorRepository.findByActivoTrue().size());
        resumen.put("totalRutinas", rutinaRepository.count());
        resumen.put("rutinasActivas", rutinaRepository.countByActivaTrue());
        resumen.put("rutinasInactivas", rutinaRepository.countByActivaFalse());
        return resumen;
    }
}
