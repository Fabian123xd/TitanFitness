package com.fitcore.controller;

import com.fitcore.model.Cliente;
import com.fitcore.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    // Listar todos
    @GetMapping
    public List<Cliente> listarTodos() {
        return clienteService.listarTodos();
    }

    // Buscar por ID
    @GetMapping("/{id}")
    public ResponseEntity<Cliente> buscarPorId(@PathVariable Long id) {
        return clienteService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Registrar cliente
    @PostMapping
    public ResponseEntity<Cliente> registrar(
            @Valid @RequestBody Cliente cliente) {

        Cliente nuevoCliente = clienteService.registrar(cliente);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(nuevoCliente);
    }
    // Actualizar cliente
@PutMapping("/{id}")
public ResponseEntity<Cliente> actualizar(
        @PathVariable Long id,
        @Valid @RequestBody Cliente cliente) {

    Cliente clienteActualizado =
            clienteService.actualizar(id, cliente);

    return ResponseEntity.ok(clienteActualizado);
}

    // Eliminar cliente
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}