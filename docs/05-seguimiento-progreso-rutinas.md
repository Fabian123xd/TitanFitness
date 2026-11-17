# 📈 Seguimiento del Progreso de Rutinas y Ejercicios — Titan Fitness Club

## 1. Propósito Funcional

El módulo de **Gestión de Rutinas** incorpora el seguimiento del avance real de los ejercicios asignados a cada socio dentro de su rutina vigente, permitiendo al entrenador o administrador:
- Marcar y desmarcar cada ejercicio como **Completado** (`completado = true / false`).
- Registrar o actualizar el **peso utilizado en kilogramos** (`pesoKg >= 0`).
- Visualizar en tiempo real el **porcentaje de progreso (`porcentajeProgreso`)** de la rutina tanto en la tabla principal como en el modal de ficha técnica.

> **Alcance del modelo**: El progreso representa el dominio/cumplimiento de los ejercicios prescritos dentro de la rutina asignada, sin simular sesiones diarias independientes.

---

## 2. Atributos y Cálculo en Backend

### Entidad `EjercicioRutina` (`ejercicios_rutina`)
- `completado` (`BOOLEAN NOT NULL DEFAULT FALSE`): Estado de cumplimiento del ejercicio.
- `pesoKg` (`DOUBLE PRECISION`, opcional, `>= 0`): Carga utilizada de referencia en kilogramos.

### Cálculo en `RutinaService.toDTO(...)`
El porcentaje siempre se calcula en el servidor y se incluye en `RutinaResponseDTO`:

$$\text{porcentajeProgreso} = \begin{cases} \text{round}\left(\dfrac{\text{ejerciciosCompletados} \times 100.0}{\text{totalEjercicios}}\right) & \text{si } \text{totalEjercicios} > 0 \\ 0 & \text{si } \text{totalEjercicios} = 0 \end{cases}$$

---

## 3. Endpoints REST de Seguimiento

| Método | Ruta | Cuerpo JSON | Descripción |
|---|---|---|---|
| `PATCH` / `PUT` | `/api/rutinas/{rutinaId}/ejercicios/{ejercicioId}/toggle-completado` | *(vacío)* | Alterna el estado `completado` del ejercicio verificando que pertenezca a `rutinaId` y retorna el `RutinaResponseDTO` actualizado. |
| `PATCH` / `PUT` | `/api/rutinas/{rutinaId}/ejercicios/{ejercicioId}/peso` | `{"pesoKg": 62.5}` | Actualiza el peso en kg del ejercicio (`>= 0`) verificando pertenencia a `rutinaId` y retorna el `RutinaResponseDTO` actualizado. |
