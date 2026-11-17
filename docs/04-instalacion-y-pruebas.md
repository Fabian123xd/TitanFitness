# ⚙️ Instalación, Ejecución y Pruebas Automatizadas

## 1. Ejecución Rápida en Modo Aislado (Sin conectarse a Neon Cloud)

Para demostraciones locales o validación sin modificar la base de datos en la nube, el proyecto incluye el perfil `isolated` (`application-isolated.properties`), que inicializa una base H2 en modo PostgreSQL con `spring.jpa.hibernate.ddl-auto=validate`:

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=isolated"
```

- **URL del Backend**: `http://localhost:8080`
- **Credenciales por defecto del perfil aislado**:
  - Usuario: `admin`
  - Contraseña: `AdminTitan2026`

---

## 2. Ejecución Conectada a PostgreSQL (Neon Cloud)

1. Revisar y aplicar en Neon Cloud el script transaccional no destructivo [`backend/src/main/resources/db/migration/V1__membresias_rutinas_auth.sql`](../backend/src/main/resources/db/migration/V1__membresias_rutinas_auth.sql).
2. Configurar las variables de entorno en PowerShell (o mediante `.env`):

```powershell
$env:DB_URL="jdbc:postgresql://<host-neon>/<base-datos>?sslmode=require"
$env:DB_USERNAME="<usuario-neon>"
$env:DB_PASSWORD="<password-neon>"
$env:ADMIN_USERNAME="admin"
$env:ADMIN_PASSWORD="<clave-segura-min-8-caracteres>"
$env:FITCORE_FERIADOS="2026-12-25,2027-01-01"

cd backend
.\mvnw.cmd spring-boot:run
```

---

## 3. Ejecución del Frontend

Abrir la carpeta `frontend` con **Live Server** en Visual Studio Code (por ejemplo en `http://localhost:5500/login.html` o `http://127.0.0.1:5500/frontend/login.html`).

---

## 4. Suite de 71 Pruebas Automatizadas

Ejecutar desde la carpeta `backend`:

```powershell
.\mvnw.cmd clean test
```

| Clase de Prueba | Tipo | Pruebas | Cobertura Principal |
|---|---|---|---|
| `BackendApplicationTests` | Contexto Spring | 1 | Arranque completo del contexto Spring Boot |
| `CalendarioLaboralTest` | Unitaria | 5 | Exclusión de sábados, domingos y feriados configurados |
| `InscripcionServiceTest` | Unitaria (Mockito) | 27 | Asignación, cálculo de vencimiento, activación (`fechaActivacion`), cancelación (`fechaCancelacion` y motivo), renovación (`inscripcionOrigenId`), prevención de solapamientos y alertas de 7 días hábiles |
| `MembresiaServiceTest` | Unitaria (Mockito) | 7 | Unicidad de nombres, edición, `/toggle` y protección de eliminación con historial |
| `RutinaServiceTest` | Unitaria (Mockito) | 13 | Registro con días y ejercicios, bloqueo de entrenadores inactivos, días sin repetir, actualización, progreso de ejercicios (`porcentajeProgreso`) y validación de peso (`pesoKg`) |
| `ApiIntegrationTest` | Integración (`@SpringBootTest` + `MockMvc`) | 17 | Seguridad 401/200, BCrypt, cambio de contraseña (`PUT /api/auth/password`), clientes, membresías, inscripciones y trazabilidad, entrenadores, rutinas, progreso (`PATCH`) y ausencia de `SQLState: 23505` |
| `MigracionValidateIntegrationTest` | Integración de Migración SQL | 1 | Crea esquema legacy con datos históricos, ejecuta migraciones V1 + V2 y valida con `spring.jpa.hibernate.ddl-auto=validate` |
| **Total** | | **71** | **0 Fallos, 0 Errores (`BUILD SUCCESS`)** |
