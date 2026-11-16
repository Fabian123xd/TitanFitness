# 📏 Reglas de Negocio y Referencia de Endpoints REST

## 1. Reglas de Negocio Implementadas

### A. Planes de Membresía (`MembresiaService`)
- **MB-01**: El nombre del plan debe ser único tanto al registrar como al actualizar.
- **MB-02 / MB-03**: La duración en días (`>= 1`) y el precio (`> 0`) son obligatorios y positivos.
- **MB-04**: Un plan inactivo (`activo = false`) no puede asignarse a nuevas inscripciones ni renovaciones.
- **MB-05**: Un plan con inscripciones asociadas en el historial no puede eliminarse; debe desactivarse con `/toggle`.
- **MB-06**: Desactivar un plan no altera las inscripciones que ya están en curso.

### B. Inscripciones, Renovaciones y Alertas (`InscripcionService` & `CalendarioLaboral`)
- **IN-01 (Prevención de duplicados y solapamientos)**: Un cliente no puede tener dos inscripciones en estado `PENDIENTE` o `ACTIVA` cuyos rangos `[fechaInicio, fechaFin]` se superpongan (`InscripcionRepository.existsSolapamiento`).
- **IN-02 / IN-03**: `fechaInicio` por defecto es hoy (no se permiten fechas en el pasado) y `fechaFin` se calcula automáticamente como `fechaInicio + membresia.duracionDias`.
- **IN-04 (Activación desacoplada de pagos)**: El administrador activa directamente una inscripción `PENDIENTE` mediante `PUT /api/inscripciones/{id}/activar`.
- **IN-05 / IN-06 / IN-07 (Renovación con historial intacto)**: Solo se renueva desde una inscripción `ACTIVA` o `VENCIDA` cuyo plan siga activo. La renovación crea un **nuevo registro** `PENDIENTE` que inicia en `fechaFin + 1 día` (o `hoy` si la anterior ya estaba vencida), conservando intacta la inscripción base.
- **IN-08**: Toda inscripción `ACTIVA` con `fechaFin < hoy` se evalúa y sincroniza automáticamente como `VENCIDA` (`VencimientoScheduler`).
- **IN-09 / IN-10 (Alertas de 7 días hábiles)**: Una inscripción `ACTIVA` entra en alerta (`proximaAVencer = true`) cuando restan **7 o menos días hábiles** entre `hoy` (exclusivo) y `fechaFin` (inclusivo), excluyendo sábados, domingos y los feriados configurados en `fitcore.feriados`.

### C. Rutinas, Días y Ejercicios (`RutinaService`)
- **RU-01 / RU-02**: No se puede asignar ni reactivar una rutina si el entrenador está inactivo (`activo = false`).
- **RU-03**: Desactivar una rutina cambia `activa = false` conservando la rutina y sus ejercicios en el historial del cliente.
- **RU-04**: Si se indica `duracionSemanas`, `fechaFin = fechaInicio.plusWeeks(duracionSemanas)`.
- **RU-05**: No se permite repetir el mismo `DiaSemana` dentro de una rutina.
- **RU-06**: Cada ejercicio exige `series >= 1`, `repeticiones >= 1` y `descansoSeg >= 0`.
- **RU-07**: Al editar una rutina existente, los días se sincronizan en sitio por `DiaSemana` evitando colisiones con la restricción `uk_dias_rutina_rutina_dia`.

---

## 2. Catálogo de Endpoints REST

| Recurso | Método | Ruta | Código Éxito | Descripción |
|---|---|---|---|---|
| **Auth** | `GET` | `/api/auth/me` | `200 OK` | Valida credenciales HTTP Basic y retorna usuario y roles |
| **Dashboard** | `GET` | `/api/reportes/resumen` | `200 OK` | Totales de clientes, planes, inscripciones por estado, entrenadores y rutinas |
| **Clientes** | `GET` | `/api/clientes` | `200 OK` | Lista todos los clientes |
| | `GET` | `/api/clientes/{id}` | `200 OK` | Obtiene un cliente por ID |
| | `POST` | `/api/clientes` | `201 Created` | Registra un cliente (valida DNI de 8 dígitos y correo únicos) |
| | `PUT` | `/api/clientes/{id}` | `200 OK` | Actualiza datos de un cliente |
| | `DELETE` | `/api/clientes/{id}` | `204 No Content` | Elimina un cliente solo si no posee historial de inscripciones o rutinas |
| **Membresías** | `GET` | `/api/membresias` | `200 OK` | Lista todos los planes de membresía |
| | `GET` | `/api/membresias/activas` | `200 OK` | Lista únicamente los planes activos |
| | `GET` | `/api/membresias/{id}` | `200 OK` | Obtiene un plan por ID |
| | `POST` | `/api/membresias` | `201 Created` | Crea un nuevo plan de membresía |
| | `PUT` | `/api/membresias/{id}` | `200 OK` | Actualiza las características de un plan |
| | `PUT` | `/api/membresias/{id}/toggle` | `200 OK` | Activa o desactiva la disponibilidad de un plan |
| | `DELETE` | `/api/membresias/{id}` | `204 No Content` | Elimina un plan sin inscripciones históricas |
| **Inscripciones** | `GET` | `/api/inscripciones` | `200 OK` | Lista todas las inscripciones con días restantes y estado efectivo |
| | `GET` | `/api/inscripciones/activas` | `200 OK` | Lista inscripciones activas vigentes |
| | `GET` | `/api/inscripciones/vencidas` | `200 OK` | Lista inscripciones vencidas |
| | `GET` | `/api/inscripciones/proximas-vencer` | `200 OK` | Lista inscripciones activas con ≤ 7 días hábiles restantes |
| | `GET` | `/api/inscripciones/resumen` | `200 OK` | Contadores por estado de inscripción |
| | `GET` | `/api/inscripciones/cliente/{clienteId}` | `200 OK` | Historial completo de inscripciones de un cliente |
| | `POST` | `/api/inscripciones` | `201 Created` | Asigna un plan activo a un cliente (nace en estado `PENDIENTE`) |
| | `PUT` | `/api/inscripciones/{id}/activar` | `200 OK` | Activa una inscripción `PENDIENTE` → `ACTIVA` |
| | `PUT` | `/api/inscripciones/{id}/cancelar` | `200 OK` | Cancela una inscripción `PENDIENTE` o `ACTIVA` |
| | `POST` | `/api/inscripciones/{id}/renovar` | `201 Created` | Crea una nueva inscripción consecutiva conservando la anterior |
| **Entrenadores** | `GET` | `/api/entrenadores` | `200 OK` | Lista todos los entrenadores |
| | `GET` | `/api/entrenadores/activos` | `200 OK` | Lista solo los entrenadores activos |
| | `POST` | `/api/entrenadores` | `200 OK` | Registra un nuevo entrenador |
| | `PUT` | `/api/entrenadores/{id}` | `200 OK` | Actualiza datos y estado de un entrenador |
| | `PUT` | `/api/entrenadores/{id}/desactivar` | `200 OK` | Desactiva un entrenador |
| **Rutinas** | `GET` | `/api/rutinas` | `200 OK` | Lista todas las rutinas con conteo de días y ejercicios |
| | `GET` | `/api/rutinas/{id}` | `200 OK` | Obtiene una rutina con su árbol completo de días y ejercicios |
| | `GET` | `/api/rutinas/cliente/{clienteId}` | `200 OK` | Historial de rutinas de un cliente |
| | `GET` | `/api/rutinas/cliente/{clienteId}/activas` | `200 OK` | Rutinas activas de un cliente |
| | `GET` | `/api/rutinas/entrenador/{entrenadorId}` | `200 OK` | Rutinas supervisadas por un entrenador |
| | `POST` | `/api/rutinas` | `201 Created` | Crea una rutina con sus días y ejercicios |
| | `PUT` | `/api/rutinas/{id}` | `200 OK` | Actualiza una rutina sincronizando sus días y ejercicios |
| | `PUT` | `/api/rutinas/{id}/desactivar` | `200 OK` | Desactiva una rutina conservándola en el historial |
| | `PUT` | `/api/rutinas/{id}/reactivar` | `200 OK` | Reactiva una rutina si su entrenador está activo |
