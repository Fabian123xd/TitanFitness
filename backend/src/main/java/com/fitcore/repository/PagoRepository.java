package com.fitcore.repository;

import com.fitcore.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    // Buscar pagos de una inscripción
    List<Pago> findByInscripcionId(Long inscripcionId);

    // Buscar pagos de un cliente
    List<Pago> findByInscripcionClienteId(Long clienteId);
}