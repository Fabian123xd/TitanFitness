package com.fitcore.repository;

import com.fitcore.model.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    // Buscar todas las inscripciones de un cliente
    List<Inscripcion> findByClienteId(Long clienteId);
}