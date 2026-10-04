package com.fitcore.repository;

import com.fitcore.model.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AsistenciaRepository
        extends JpaRepository<Asistencia, Long> {

    // Historial de asistencias de un cliente
    List<Asistencia> findByClienteId(Long clienteId);

    // Asistencias correspondientes a una inscripción
    List<Asistencia> findByInscripcionId(Long inscripcionId);
}