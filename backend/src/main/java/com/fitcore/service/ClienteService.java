package com.fitcore.service;

import com.fitcore.model.Cliente;
import com.fitcore.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    // Listar todos los clientes
    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    // Buscar cliente por ID
    public Optional<Cliente> buscarPorId(Long id) {
        return clienteRepository.findById(id);
    }

    // Registrar cliente
    public Cliente registrar(Cliente cliente) {

        if (clienteRepository.existsByDni(cliente.getDni())) {
            throw new RuntimeException("Ya existe un cliente con ese DNI");
        }

        if (cliente.getCorreo() != null &&
                clienteRepository.existsByCorreo(cliente.getCorreo())) {
            throw new RuntimeException("Ya existe un cliente con ese correo");
        }

        return clienteRepository.save(cliente);
    }

    // Eliminar cliente
    public void eliminar(Long id) {

        if (!clienteRepository.existsById(id)) {
            throw new RuntimeException("Cliente no encontrado");
        }

        clienteRepository.deleteById(id);
    }
    public Cliente actualizar(Long id, Cliente datos) {

    Cliente cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

    cliente.setNombres(datos.getNombres());
    cliente.setApellidos(datos.getApellidos());
    cliente.setDni(datos.getDni());
    cliente.setTelefono(datos.getTelefono());
    cliente.setCorreo(datos.getCorreo());

    return clienteRepository.save(cliente);
}
}