package com.fitcore.repository;

import com.fitcore.model.Membresia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MembresiaRepository extends JpaRepository<Membresia, Long> {

    boolean existsByNombre(String nombre);
}