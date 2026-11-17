# 🔗 Trazabilidad Completa de Membresías, Renovaciones yConcurrencia

## 1. Atributos de Auditoría y Trazabilidad en `Inscripcion`

La entidad `Inscripcion` (`inscripciones`) incorpora cuatro campos de trazabilidad compatibles con registros históricos (`NULL` permitido en datos previos):

| Columna SQL | Atributo Java | Tipo | Propósito |
|---|---|---|---|
| `inscripcion_origen_id` | `inscripcionOrigenId` | `Long` (`BIGINT`, FK opcional a `inscripciones.id`) | Identifica la inscripción anterior que dio origen a una renovación. `null` cuando es una inscripción inicial. |
| `fecha_activacion` | `fechaActivacion` | `LocalDateTime` (`TIMESTAMP`) | Fecha y hora exactas en que el administrador activó la inscripción (`PENDIENTE → ACTIVA`). |
| `fecha_cancelacion` | `fechaCancelacion` | `LocalDateTime` (`TIMESTAMP`) | Fecha y hora exactas en que se canceló la inscripción. |
| `motivo_cancelacion` | `motivoCancelacion` | `String` (`VARCHAR(300)`) | Motivo registrado al cancelar la suscripción. |

---

## 2. Reglas de Negocio y Cadena de Renovación

1. **Activación auditada (`PUT /api/inscripciones/{id}/activar`)**:
   - Solo procede desde `PENDIENTE` y si el período no ha expirado.
   - Cambia el estado a `ACTIVA` y registra `fechaActivacion = LocalDateTime.now(clock)`.
2. **Cancelación auditada (`PUT /api/inscripciones/{id}/cancelar?motivo=...`)**:
   - Solo procede desde `PENDIENTE` o `ACTIVA`.
   - Cambia el estado a `CANCELADA`, registra `fechaCancelacion = LocalDateTime.now(clock)` y almacena `motivoCancelacion`.
3. **Renovación vinculada (`POST /api/inscripciones/{id}/renovar`)**:
   - Verifica que no exista ya otra renovación activa/pendiente originada en el mismo `id` (`existsRenovacionDeOrigen(id)`).
   - Crea una nueva inscripción `PENDIENTE` con `inscripcionOrigenId = id`, conservando intacta la inscripción anterior.
   - En el frontend (`inscripciones.html` y modal de **Expediente Histórico** en `clientes.html`), se construye la línea de tiempo cronológica real:
     `Inscripción inicial #1 → Renovada en #2 (desde #1) → Renovada en #3 (desde #2)`.

---

## 3. Control de Concurrencia (`PESSIMISTIC_WRITE`)

En `ClienteRepository.findByIdForUpdate(Long id)` se utiliza `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`SELECT ... FOR UPDATE`). Tanto `InscripcionService.registrar(...)` como `InscripcionService.renovar(...)` adquieren este bloqueo transaccional sobre el registro del `Cliente`, impidiendo que un doble clic o dos solicitudes concurrentes inserten inscripciones superpuestas.
