package com.fitcore.controller;

import com.fitcore.dto.CambioPasswordRequestDTO;
import com.fitcore.exception.RecursoNoEncontradoException;
import com.fitcore.exception.ReglaDeNegocioException;
import com.fitcore.model.Usuario;
import com.fitcore.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador de autenticación y gestión segura de la contraseña del administrador.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        return Map.of(
                "username", authentication.getName(),
                "roles", authentication.getAuthorities().stream().map(a -> a.getAuthority()).toList());
    }

    /**
     * Actualiza la contraseña del usuario autenticado tras verificar su contraseña actual con BCrypt.
     */
    @PutMapping("/password")
    @Transactional
    public Map<String, String> cambiarPassword(
            Authentication authentication,
            @Valid @RequestBody CambioPasswordRequestDTO request) {
        Usuario usuario = usuarioRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.passwordActual(), usuario.getPasswordHash())) {
            throw new ReglaDeNegocioException("La contraseña actual es incorrecta.");
        }

        if (request.passwordActual().equals(request.passwordNueva())) {
            throw new ReglaDeNegocioException("La nueva contraseña debe ser diferente a la actual.");
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.passwordNueva()));
        usuarioRepository.save(usuario);
        return Map.of("mensaje", "Contraseña actualizada correctamente");
    }
}
