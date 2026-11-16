package com.fitcore.security;

import com.fitcore.model.Usuario;
import com.fitcore.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea el administrador inicial SOLO si no existe ningún usuario.
 * Las credenciales vienen de variables de entorno (ADMIN_USERNAME / ADMIN_PASSWORD);
 * nunca están en el código. La contraseña se guarda con hash BCrypt.
 */
@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public AdminInitializer(UsuarioRepository usuarioRepository,
                            PasswordEncoder passwordEncoder,
                            @Value("${fitcore.admin.username:admin}") String username,
                            @Value("${fitcore.admin.password:}") String password) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        if (password == null || password.length() < 8) {
            log.warn("No hay usuarios y ADMIN_PASSWORD no está definida (mínimo 8 caracteres). "
                    + "Defina ADMIN_USERNAME y ADMIN_PASSWORD en el entorno para crear el administrador inicial.");
            return;
        }
        Usuario admin = new Usuario();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRol("ADMIN");
        admin.setActivo(true);
        usuarioRepository.save(admin);
        log.info("Administrador inicial '{}' creado.", username);
    }
}
