package com.fitcore.service;

import com.fitcore.model.Cliente;
import com.fitcore.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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

    // Validar DNI duplicado
    if (clienteRepository.existsByDni(cliente.getDni())) {

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ya existe un cliente con ese DNI"
        );
    }

    // Validar correo duplicado
    if (cliente.getCorreo() != null &&
            !cliente.getCorreo().isBlank() &&
            clienteRepository.existsByCorreo(cliente.getCorreo())) {

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ya existe un cliente con ese correo"
        );
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
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Cliente no encontrado"
            ));

    // Verificar si el DNI pertenece a otro cliente
    if (!cliente.getDni().equals(datos.getDni())
            && clienteRepository.existsByDni(datos.getDni())) {

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El DNI ya pertenece a otro cliente"
        );
    }

    // Verificar si el correo pertenece a otro cliente
    if (datos.getCorreo() != null
            && !datos.getCorreo().isBlank()
            && !datos.getCorreo().equals(cliente.getCorreo())
            && clienteRepository.existsByCorreo(datos.getCorreo())) {

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El correo ya pertenece a otro cliente"
        );
    }

    cliente.setNombres(datos.getNombres());
    cliente.setApellidos(datos.getApellidos());
    cliente.setDni(datos.getDni());
    cliente.setTelefono(datos.getTelefono());
    cliente.setCorreo(datos.getCorreo());

    return clienteRepository.save(cliente);
}
}