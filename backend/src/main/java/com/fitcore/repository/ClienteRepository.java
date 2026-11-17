package com.fitcore.repository;

import com.fitcore.model.Cliente;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByDni(String dni);

    boolean existsByCorreo(String correo);

    /**
     * Obtiene el cliente adquiriendo un bloqueo de escritura pesimista (SELECT ... FOR UPDATE)
     * para serializar la creación o renovación concurrente de inscripciones de un mismo socio.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cliente c WHERE c.id = :id")
    Optional<Cliente> findByIdForUpdate(@Param("id") Long id);
}
