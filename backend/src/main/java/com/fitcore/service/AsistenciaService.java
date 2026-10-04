package com.fitcore.service;

import com.fitcore.model.Asistencia;
import com.fitcore.model.Inscripcion;
import com.fitcore.repository.AsistenciaRepository;
import com.fitcore.repository.InscripcionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;
    private final InscripcionRepository inscripcionRepository;

    public AsistenciaService(
            AsistenciaRepository asistenciaRepository,
            InscripcionRepository inscripcionRepository) {

        this.asistenciaRepository = asistenciaRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    // Listar todas las asistencias
    public List<Asistencia> listarTodas() {
        return asistenciaRepository.findAll();
    }

    // Buscar asistencia por ID
    public Optional<Asistencia> buscarPorId(Long id) {
        return asistenciaRepository.findById(id);
    }

    // Historial de asistencias de un cliente
    public List<Asistencia> listarPorCliente(Long clienteId) {
        return asistenciaRepository.findByClienteId(clienteId);
    }

    // Asistencias de una inscripción
    public List<Asistencia> listarPorInscripcion(Long inscripcionId) {
        return asistenciaRepository.findByInscripcionId(inscripcionId);
    }

    // Registrar ingreso al gimnasio
    public Asistencia registrar(Long inscripcionId) {

        Inscripcion inscripcion = inscripcionRepository
                .findById(inscripcionId)
                .orElseThrow(() ->
                        new RuntimeException("Inscripción no encontrada"));

        // Validar que la inscripción esté activa
        if (!"ACTIVA".equalsIgnoreCase(inscripcion.getEstado())) {
            throw new RuntimeException(
                    "Ingreso denegado: la inscripción no está activa");
        }

        // Validar fecha de vencimiento
        if (LocalDate.now().isAfter(inscripcion.getFechaFin())) {

            inscripcion.setEstado("VENCIDA");
            inscripcionRepository.save(inscripcion);

            throw new RuntimeException(
                    "Ingreso denegado: la membresía está vencida");
        }

        Asistencia asistencia = new Asistencia();

        asistencia.setCliente(inscripcion.getCliente());
        asistencia.setInscripcion(inscripcion);
        asistencia.setEstado("INGRESO");

        return asistenciaRepository.save(asistencia);
    }

    // Eliminar asistencia
    public void eliminar(Long id) {

        if (!asistenciaRepository.existsById(id)) {
            throw new RuntimeException("Asistencia no encontrada");
        }

        asistenciaRepository.deleteById(id);
    }
}