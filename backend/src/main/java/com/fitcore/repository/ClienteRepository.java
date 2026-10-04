package com.fitcore.repository;

import com.fitcore.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByDni(String dni);

    boolean existsByCorreo(String correo);
}