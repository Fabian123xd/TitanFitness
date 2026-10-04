package com.fitcore.service;

import com.fitcore.model.Membresia;
import com.fitcore.repository.MembresiaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MembresiaService {

    private final MembresiaRepository membresiaRepository;

    public MembresiaService(MembresiaRepository membresiaRepository) {
        this.membresiaRepository = membresiaRepository;
    }

    // Listar todas las membresías
    public List<Membresia> listarTodas() {
        return membresiaRepository.findAll();
    }

    // Buscar membresía por ID
    public Optional<Membresia> buscarPorId(Long id) {
        return membresiaRepository.findById(id);
    }

    // Registrar una membresía
    public Membresia registrar(Membresia membresia) {

        if (membresiaRepository.existsByNombre(membresia.getNombre())) {
            throw new RuntimeException("Ya existe una membresía con ese nombre");
        }

        return membresiaRepository.save(membresia);
    }

    // Actualizar membresía
    public Membresia actualizar(Long id, Membresia datos) {

        Membresia membresia = membresiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Membresía no encontrada"));

        membresia.setNombre(datos.getNombre());
        membresia.setDescripcion(datos.getDescripcion());
        membresia.setDuracionDias(datos.getDuracionDias());
        membresia.setPrecio(datos.getPrecio());
        membresia.setActivo(datos.getActivo());

        return membresiaRepository.save(membresia);
    }

    // Eliminar membresía
    public void eliminar(Long id) {

        if (!membresiaRepository.existsById(id)) {
            throw new RuntimeException("Membresía no encontrada");
        }

        membresiaRepository.deleteById(id);
    }
}