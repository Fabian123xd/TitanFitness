package com.fitcore.service;

import com.fitcore.model.Cliente;
import com.fitcore.model.Inscripcion;
import com.fitcore.model.Membresia;
import com.fitcore.repository.ClienteRepository;
import com.fitcore.repository.InscripcionRepository;
import com.fitcore.repository.MembresiaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class InscripcionService {

    private final InscripcionRepository inscripcionRepository;
    private final ClienteRepository clienteRepository;
    private final MembresiaRepository membresiaRepository;

    public InscripcionService(
            InscripcionRepository inscripcionRepository,
            ClienteRepository clienteRepository,
            MembresiaRepository membresiaRepository) {

        this.inscripcionRepository = inscripcionRepository;
        this.clienteRepository = clienteRepository;
        this.membresiaRepository = membresiaRepository;
    }

    // Listar todas las inscripciones
    public List<Inscripcion> listarTodas() {
        return inscripcionRepository.findAll();
    }

    // Buscar inscripción por ID
    public Optional<Inscripcion> buscarPorId(Long id) {
        return inscripcionRepository.findById(id);
    }

    // Buscar inscripciones de un cliente
    public List<Inscripcion> listarPorCliente(Long clienteId) {
        return inscripcionRepository.findByClienteId(clienteId);
    }

    // Crear una inscripción
    public Inscripcion registrar(Long clienteId, Long membresiaId) {

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() ->
                        new RuntimeException("Cliente no encontrado"));

        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() ->
                        new RuntimeException("Membresía no encontrada"));

        if (!membresia.getActivo()) {
            throw new RuntimeException("La membresía seleccionada no está activa");
        }

        LocalDate fechaInicio = LocalDate.now();

        LocalDate fechaFin = fechaInicio.plusDays(
                membresia.getDuracionDias()
        );

        Inscripcion inscripcion = new Inscripcion();

        inscripcion.setCliente(cliente);
        inscripcion.setMembresia(membresia);
        inscripcion.setFechaInicio(fechaInicio);
        inscripcion.setFechaFin(fechaFin);
        inscripcion.setEstado("ACTIVA");

        return inscripcionRepository.save(inscripcion);
    }

    // Eliminar inscripción
    public void eliminar(Long id) {

        if (!inscripcionRepository.existsById(id)) {
            throw new RuntimeException("Inscripción no encontrada");
        }

        inscripcionRepository.deleteById(id);
    }
}