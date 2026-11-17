package com.fitcore.repository;

import com.fitcore.model.Entrenador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntrenadorRepository extends JpaRepository<Entrenador, Long> {

    List<Entrenador> findByActivoTrue();

    long countByActivoTrue();

    boolean existsByCorreo(String correo);
}
