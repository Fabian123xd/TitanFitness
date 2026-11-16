package com.fitcore.repository;

import com.fitcore.model.Membresia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MembresiaRepository extends JpaRepository<Membresia, Long> {

    boolean existsByNombre(String nombre);

    /** Lista solo los planes activos, útil para el selector de inscripciones. */
    List<Membresia> findByActivoTrue();
}