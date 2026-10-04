package com.fitcore.repository;

import com.fitcore.model.Rutina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RutinaRepository extends JpaRepository<Rutina, Long> {

    // Rutinas de un cliente
    List<Rutina> findByClienteId(Long clienteId);

    // Rutinas asignadas por un entrenador
    List<Rutina> findByEntrenadorId(Long entrenadorId);

    // Rutinas activas de un cliente
    List<Rutina> findByClienteIdAndActivaTrue(Long clienteId);
}