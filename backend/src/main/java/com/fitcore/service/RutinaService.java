package com.fitcore.service;

import com.fitcore.model.Cliente;
import com.fitcore.model.Entrenador;
import com.fitcore.model.Rutina;
import com.fitcore.repository.ClienteRepository;
import com.fitcore.repository.EntrenadorRepository;
import com.fitcore.repository.RutinaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RutinaService {

    private final RutinaRepository rutinaRepository;
    private final ClienteRepository clienteRepository;
    private final EntrenadorRepository entrenadorRepository;

    public RutinaService(
            RutinaRepository rutinaRepository,
            ClienteRepository clienteRepository,
            EntrenadorRepository entrenadorRepository) {

        this.rutinaRepository = rutinaRepository;
        this.clienteRepository = clienteRepository;
        this.entrenadorRepository = entrenadorRepository;
    }

    public List<Rutina> listarTodas() {
        return rutinaRepository.findAll();
    }

    public Optional<Rutina> buscarPorId(Long id) {
        return rutinaRepository.findById(id);
    }

    public List<Rutina> listarPorCliente(Long clienteId) {
        return rutinaRepository.findByClienteId(clienteId);
    }

    public List<Rutina> listarPorEntrenador(Long entrenadorId) {
        return rutinaRepository.findByEntrenadorId(entrenadorId);
    }

    public List<Rutina> listarActivasPorCliente(Long clienteId) {
        return rutinaRepository.findByClienteIdAndActivaTrue(clienteId);
    }

    public Rutina registrar(
            Long clienteId,
            Long entrenadorId,
            String nombre,
            String descripcion) {

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() ->
                        new RuntimeException("Cliente no encontrado"));

        Entrenador entrenador = entrenadorRepository.findById(entrenadorId)
                .orElseThrow(() ->
                        new RuntimeException("Entrenador no encontrado"));

        if (!entrenador.getActivo()) {
            throw new RuntimeException(
                    "No se puede asignar una rutina con un entrenador inactivo");
        }

        Rutina rutina = new Rutina();
        rutina.setCliente(cliente);
        rutina.setEntrenador(entrenador);
        rutina.setNombre(nombre);
        rutina.setDescripcion(descripcion);
        rutina.setActiva(true);

        return rutinaRepository.save(rutina);
    }

    public Rutina desactivar(Long id) {

        Rutina rutina = rutinaRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Rutina no encontrada"));

        rutina.setActiva(false);

        return rutinaRepository.save(rutina);
    }
}