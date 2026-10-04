package com.fitcore.service;

import com.fitcore.model.Entrenador;
import com.fitcore.repository.EntrenadorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EntrenadorService {

    private final EntrenadorRepository entrenadorRepository;

    public EntrenadorService(EntrenadorRepository entrenadorRepository) {
        this.entrenadorRepository = entrenadorRepository;
    }

    // Listar todos
    public List<Entrenador> listarTodos() {
        return entrenadorRepository.findAll();
    }

    // Listar solamente activos
    public List<Entrenador> listarActivos() {
        return entrenadorRepository.findByActivoTrue();
    }

    // Buscar por ID
    public Optional<Entrenador> buscarPorId(Long id) {
        return entrenadorRepository.findById(id);
    }

    // Registrar entrenador
    public Entrenador registrar(Entrenador entrenador) {

        if (entrenador.getCorreo() != null &&
                !entrenador.getCorreo().isBlank() &&
                entrenadorRepository.existsByCorreo(entrenador.getCorreo())) {

            throw new RuntimeException(
                    "Ya existe un entrenador con ese correo");
        }

        entrenador.setActivo(true);

        return entrenadorRepository.save(entrenador);
    }

    // Actualizar entrenador
    public Entrenador actualizar(Long id, Entrenador datos) {

        Entrenador entrenador = entrenadorRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Entrenador no encontrado"));

        entrenador.setNombres(datos.getNombres());
        entrenador.setApellidos(datos.getApellidos());
        entrenador.setEspecialidad(datos.getEspecialidad());
        entrenador.setTelefono(datos.getTelefono());
        entrenador.setCorreo(datos.getCorreo());

        return entrenadorRepository.save(entrenador);
    }

    // Desactivar entrenador
    public Entrenador desactivar(Long id) {

        Entrenador entrenador = entrenadorRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Entrenador no encontrado"));

        entrenador.setActivo(false);

        return entrenadorRepository.save(entrenador);
    }
}