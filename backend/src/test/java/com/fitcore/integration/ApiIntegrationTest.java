package com.fitcore.integration;

import com.fitcore.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración de punta a punta (HTTP → seguridad → controlador → servicio → JPA)
 * sobre una base H2 en memoria. NO usa Neon. Cada prueba se revierte al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiIntegrationTest {

    private static final AtomicInteger SECUENCIA = new AtomicInteger(10_000_000);

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarioRepository;
    @PersistenceContext EntityManager em;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private RequestPostProcessor admin() {
        return httpBasic("admin", "ClaveSoloParaPruebas2026");
    }

    private String json(org.springframework.test.web.servlet.ResultActions r) throws Exception {
        return r.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String enviarPost(String url, String cuerpo, int estado) throws Exception {
        return json(mvc.perform(post(url).with(admin()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().is(estado)));
    }

    private String enviarPut(String url, String cuerpo, int estado) throws Exception {
        var req = put(url).with(admin());
        if (cuerpo != null) {
            req = req.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        return json(mvc.perform(req).andExpect(status().is(estado)));
    }

    private String enviarGet(String url) throws Exception {
        return json(mvc.perform(get(url).with(admin())).andExpect(status().isOk()));
    }

    private void recargarDesdeBD() {
        em.flush();
        em.clear();
    }

    private long crearCliente() throws Exception {
        int n = SECUENCIA.incrementAndGet();
        String cuerpo = """
                {"nombres":"Ana","apellidos":"Perez","dni":"%d","telefono":"999111222","correo":"ana%d@test.com"}
                """.formatted(n, n);
        return ((Number) JsonPath.read(enviarPost("/api/clientes", cuerpo, 201), "$.id")).longValue();
    }

    private long crearPlan(String nombre, int dias) throws Exception {
        String cuerpo = """
                {"nombre":"%s","descripcion":"plan de prueba","duracionDias":%d,"precio":80.50,"activo":true}
                """.formatted(nombre + SECUENCIA.incrementAndGet(), dias);
        return ((Number) JsonPath.read(enviarPost("/api/membresias", cuerpo, 201), "$.id")).longValue();
    }

    private long crearEntrenador() throws Exception {
        int n = SECUENCIA.incrementAndGet();
        String cuerpo = """
                {"nombres":"Carla","apellidos":"Diaz","especialidad":"Fuerza","telefono":"988777666","correo":"carla%d@test.com"}
                """.formatted(n);
        return ((Number) JsonPath.read(enviarPost("/api/entrenadores", cuerpo, 200), "$.id")).longValue();
    }

    private long asignar(long clienteId, long planId, int estado) throws Exception {
        String r = enviarPost("/api/inscripciones",
                "{\"clienteId\":%d,\"membresiaId\":%d}".formatted(clienteId, planId), estado);
        return estado == 201 ? ((Number) JsonPath.read(r, "$.id")).longValue() : -1;
    }

    // ── Seguridad / autenticación ─────────────────────────────────────────────

    @Test
    @DisplayName("Sin credenciales todos los endpoints responden 401")
    void sinCredenciales() throws Exception {
        for (String url : new String[]{"/api/clientes", "/api/membresias", "/api/inscripciones",
                "/api/rutinas", "/api/entrenadores", "/api/reportes/resumen"}) {
            mvc.perform(get(url)).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo").value("UNAUTHORIZED"));
        }
    }

    @Test
    @DisplayName("Credenciales incorrectas → 401; correctas → 200")
    void login() throws Exception {
        mvc.perform(get("/api/auth/me").with(httpBasic("admin", "incorrecta")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").with(httpBasic("noexiste", "ClaveSoloParaPruebas2026")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    @DisplayName("La contraseña se almacena con hash BCrypt, nunca en texto plano")
    void contrasenaConHash() {
        var admin = usuarioRepository.findByUsername("admin").orElseThrow();
        assertTrue(admin.getPasswordHash().startsWith("$2"));
        assertNotEquals("ClaveSoloParaPruebas2026", admin.getPasswordHash());
    }

    // ── Membresías e inscripciones ────────────────────────────────────────────

    @Test
    @DisplayName("Flujo completo: asignar → duplicado rechazado → activar → renovar → historial")
    void flujoInscripcion() throws Exception {
        long clienteId = crearCliente();
        long planId = crearPlan("Mensual", 30);
        LocalDate hoy = LocalDate.now();

        // Asignación: PENDIENTE, vencimiento calculado
        long insId = asignar(clienteId, planId, 201);
        mvcGet("/api/inscripciones/" + insId, "$.estado", "PENDIENTE");
        mvcGet("/api/inscripciones/" + insId, "$.fechaFin", hoy.plusDays(30).toString());

        // Duplicado / período superpuesto
        enviarPost("/api/inscripciones", "{\"clienteId\":%d,\"membresiaId\":%d}".formatted(clienteId, planId), 400);

        // Activación sin pagos
        enviarPut("/api/inscripciones/" + insId + "/activar", null, 200);
        mvcGet("/api/inscripciones/" + insId, "$.estado", "ACTIVA");
        enviarPut("/api/inscripciones/" + insId + "/activar", null, 400); // ya activa

        // Renovación: nueva inscripción desde el día siguiente al fin
        String nueva = enviarPost("/api/inscripciones/" + insId + "/renovar", "", 201);
        assertEquals("PENDIENTE", JsonPath.read(nueva, "$.estado"));
        assertEquals(hoy.plusDays(31).toString(), JsonPath.read(nueva, "$.fechaInicio"));
        assertEquals(hoy.plusDays(61).toString(), JsonPath.read(nueva, "$.fechaFin"));

        // Segunda renovación de la misma base: se superpone con la anterior → rechazada
        enviarPost("/api/inscripciones/" + insId + "/renovar", "", 400);

        // Historial: la inscripción original se conserva
        String historial = enviarGet("/api/inscripciones/cliente/" + clienteId);
        assertEquals(2, ((java.util.List<?>) JsonPath.read(historial, "$")).size());
        mvcGet("/api/inscripciones/" + insId, "$.estado", "ACTIVA");

        // Cancelar y volver a cancelar
        enviarPut("/api/inscripciones/" + insId + "/cancelar", null, 200);
        enviarPut("/api/inscripciones/" + insId + "/cancelar", null, 400);
    }

    private void mvcGet(String url, String path, String esperado) throws Exception {
        mvc.perform(get(url).with(admin())).andExpect(status().isOk())
                .andExpect(jsonPath(path).value(esperado));
    }

    @Test
    @DisplayName("Plan inactivo no se asigna; protege integridad al eliminar plan o cliente con historial")
    void integridadYPlanInactivo() throws Exception {
        long clienteId = crearCliente();
        long otroCliente = crearCliente();
        long planId = crearPlan("Trimestral", 90);
        asignar(clienteId, planId, 201);

        // Eliminar plan o cliente con historial → regla de negocio (400), no error 500
        mvc.perform(delete("/api/membresias/" + planId).with(admin())).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/clientes/" + clienteId).with(admin())).andExpect(status().isBadRequest());

        // Desactivar plan: ya no se puede asignar a nuevos clientes
        enviarPut("/api/membresias/" + planId + "/toggle", null, 200);
        asignar(otroCliente, planId, 400);

        // Cliente / plan inexistentes → 404
        enviarPost("/api/inscripciones", "{\"clienteId\":999999,\"membresiaId\":%d}".formatted(planId), 404);
        enviarPost("/api/inscripciones", "{\"clienteId\":%d,\"membresiaId\":999999}".formatted(otroCliente), 404);
    }

    @Test
    @DisplayName("Alertas: la inscripción que vence en pocos días aparece; la de 60 días no")
    void alertasVencimiento() throws Exception {
        long c1 = crearCliente();
        long c2 = crearCliente();
        long planCorto = crearPlan("Semanal", 5);
        long planLargo = crearPlan("Bimestral", 60);

        long corto = asignar(c1, planCorto, 201);
        long largo = asignar(c2, planLargo, 201);
        enviarPut("/api/inscripciones/" + corto + "/activar", null, 200);
        enviarPut("/api/inscripciones/" + largo + "/activar", null, 200);

        String alertas = enviarGet("/api/inscripciones/proximas-vencer");
        java.util.List<Integer> ids = JsonPath.read(alertas, "$[*].id");
        assertTrue(ids.stream().anyMatch(i -> i.longValue() == corto));
        assertFalse(ids.stream().anyMatch(i -> i.longValue() == largo));

        String resumen = enviarGet("/api/inscripciones/resumen");
        assertTrue(((Number) JsonPath.read(resumen, "$.activas")).longValue() >= 2);
        assertTrue(((Number) JsonPath.read(resumen, "$.proximasAVencer")).longValue() >= 1);
    }

    @Test
    @DisplayName("Validaciones Jakarta devuelven 400 con código VALIDATION_ERROR")
    void validaciones() throws Exception {
        enviarPost("/api/inscripciones", "{\"membresiaId\":1}", 400);
        String r = enviarPost("/api/clientes",
                "{\"nombres\":\"\",\"apellidos\":\"X\",\"dni\":\"abc\"}", 400);
        assertEquals("VALIDATION_ERROR", JsonPath.read(r, "$.codigo"));
        enviarPost("/api/membresias",
                "{\"nombre\":\"Malo\",\"duracionDias\":30,\"precio\":0,\"activo\":true}", 400);
        enviarPost("/api/membresias",
                "{\"nombre\":\"Malo2\",\"duracionDias\":0,\"precio\":10,\"activo\":true}", 400);
        enviarPost("/api/inscripciones", "esto no es json", 400);
    }

    @Test
    @DisplayName("Plan duplicado → 400; DNI duplicado → 409")
    void duplicados() throws Exception {
        String plan = """
                {"nombre":"PlanUnico%d","duracionDias":30,"precio":10,"activo":true}
                """.formatted(SECUENCIA.incrementAndGet());
        enviarPost("/api/membresias", plan, 201);
        enviarPost("/api/membresias", plan, 400);

        int n = SECUENCIA.incrementAndGet();
        String c = """
                {"nombres":"A","apellidos":"B","dni":"%d","correo":"dup%d@test.com"}
                """.formatted(n, n);
        enviarPost("/api/clientes", c, 201);
        enviarPost("/api/clientes", c, 409);
    }

    // ── Rutinas ───────────────────────────────────────────────────────────────

    private String rutinaJson(long clienteId, long entrenadorId, String dias) {
        return """
                {"nombre":"Fuerza 4 semanas","objetivo":"Ganar masa muscular","descripcion":"Rutina base",
                 "clienteId":%d,"entrenadorId":%d,"duracionSemanas":4%s}
                """.formatted(clienteId, entrenadorId, dias == null ? "" : ",\"dias\":" + dias);
    }

    private static final String DIAS_COMPLETOS = """
            [{"diaSemana":"LUNES","nombreDia":"Pecho","ejercicios":[
                {"nombre":"Press banca","series":4,"repeticiones":12,"descansoSeg":60},
                {"nombre":"Fondos","series":3,"repeticiones":15,"descansoSeg":45}]},
             {"diaSemana":"MIERCOLES","nombreDia":"Espalda","ejercicios":[
                {"nombre":"Dominadas","series":4,"repeticiones":8,"descansoSeg":90}]}]
            """;

    @Test
    @DisplayName("Rutinas: registrar con días y ejercicios, consultar, actualizar, desactivar")
    void flujoRutinas() throws Exception {
        long clienteId = crearCliente();
        long entrenadorId = crearEntrenador();

        String creada = enviarPost("/api/rutinas", rutinaJson(clienteId, entrenadorId, DIAS_COMPLETOS), 201);
        long rutinaId = ((Number) JsonPath.read(creada, "$.id")).longValue();
        assertEquals("Ganar masa muscular", JsonPath.read(creada, "$.objetivo"));
        assertEquals(LocalDate.now().plusWeeks(4).toString(), JsonPath.read(creada, "$.fechaFin"));
        assertEquals(3, (int) JsonPath.read(creada, "$.totalEjercicios"));

        // Se persiste la jerarquía rutina → días → ejercicios
        recargarDesdeBD();
        String detalle = enviarGet("/api/rutinas/" + rutinaId);
        assertEquals(2, (int) JsonPath.read(detalle, "$.totalDias"));
        assertEquals("LUNES", JsonPath.read(detalle, "$.dias[0].diaSemana"));
        assertEquals("Press banca", JsonPath.read(detalle, "$.dias[0].ejercicios[0].nombre"));
        assertEquals(4, (int) JsonPath.read(detalle, "$.dias[0].ejercicios[0].series"));
        assertEquals(12, (int) JsonPath.read(detalle, "$.dias[0].ejercicios[0].repeticiones"));
        assertEquals(60, (int) JsonPath.read(detalle, "$.dias[0].ejercicios[0].descansoSeg"));

        // Consultas por cliente y por entrenador
        assertEquals(1, ((java.util.List<?>) JsonPath.read(enviarGet("/api/rutinas/cliente/" + clienteId), "$")).size());
        assertEquals(1, ((java.util.List<?>) JsonPath.read(enviarGet("/api/rutinas/entrenador/" + entrenadorId), "$")).size());

        // Actualizar: queda solo un día (los demás se eliminan)
        String dias = """
                [{"diaSemana":"VIERNES","nombreDia":"Cardio","ejercicios":[
                    {"nombre":"Trote","series":1,"repeticiones":1,"descansoSeg":0}]}]
                """;
        String cuerpo = rutinaJson(clienteId, entrenadorId, dias).replace("Fuerza 4 semanas", "Cardio 4 semanas");
        enviarPut("/api/rutinas/" + rutinaId, cuerpo, 200);
        recargarDesdeBD();
        detalle = enviarGet("/api/rutinas/" + rutinaId);
        assertEquals("Cardio 4 semanas", JsonPath.read(detalle, "$.nombre"));
        assertEquals(1, (int) JsonPath.read(detalle, "$.totalDias"));
        assertEquals("VIERNES", JsonPath.read(detalle, "$.dias[0].diaSemana"));

        // Desactivar conserva la rutina (historial)
        enviarPut("/api/rutinas/" + rutinaId + "/desactivar", null, 200);
        assertEquals(1, ((java.util.List<?>) JsonPath.read(enviarGet("/api/rutinas/cliente/" + clienteId), "$")).size());
        assertEquals(0, ((java.util.List<?>) JsonPath.read(enviarGet("/api/rutinas/cliente/" + clienteId + "/activas"), "$")).size());
        enviarPut("/api/rutinas/" + rutinaId + "/reactivar", null, 200);
    }

    @Test
    @DisplayName("Actualizar rutina conservando los mismos días (LUNES/MIERCOLES) y editando ejercicios NO provoca SQLState 23505")
    void actualizarRutinaConMismosDiasSinError23505() throws Exception {
        long clienteId = crearCliente();
        long entrenadorId = crearEntrenador();

        // 1. Crear rutina con LUNES y MIERCOLES y forzar flush en BD
        String creada = enviarPost("/api/rutinas", rutinaJson(clienteId, entrenadorId, DIAS_COMPLETOS), 201);
        long rutinaId = ((Number) JsonPath.read(creada, "$.id")).longValue();
        recargarDesdeBD();

        // 2. Actualizar la rutina manteniendo LUNES (con nuevos ejercicios), quitando MIERCOLES y agregando VIERNES
        String diasEditados = """
                [{"diaSemana":"LUNES","nombreDia":"Pecho Pesado","ejercicios":[
                    {"nombre":"Press inclinado","series":5,"repeticiones":8,"descansoSeg":90}]},
                 {"diaSemana":"VIERNES","nombreDia":"Pierna Completa","ejercicios":[
                    {"nombre":"Sentadilla libre","series":4,"repeticiones":10,"descansoSeg":120},
                    {"nombre":"Prensa","series":4,"repeticiones":12,"descansoSeg":90}]}]
                """;
        enviarPut("/api/rutinas/" + rutinaId, rutinaJson(clienteId, entrenadorId, diasEditados), 200);
        recargarDesdeBD();

        String detalle = enviarGet("/api/rutinas/" + rutinaId);
        assertEquals(2, (int) JsonPath.read(detalle, "$.totalDias"));
        assertEquals(3, (int) JsonPath.read(detalle, "$.totalEjercicios"));
        assertEquals("LUNES", JsonPath.read(detalle, "$.dias[0].diaSemana"));
        assertEquals("Pecho Pesado", JsonPath.read(detalle, "$.dias[0].nombreDia"));
        assertEquals("Press inclinado", JsonPath.read(detalle, "$.dias[0].ejercicios[0].nombre"));
        assertEquals("VIERNES", JsonPath.read(detalle, "$.dias[1].diaSemana"));
    }

    @Test
    @DisplayName("Historial: un cliente puede tener varias rutinas a lo largo del tiempo")
    void variasRutinasPorCliente() throws Exception {
        long clienteId = crearCliente();
        long entrenadorId = crearEntrenador();
        enviarPost("/api/rutinas", rutinaJson(clienteId, entrenadorId, null), 201);
        enviarPost("/api/rutinas", rutinaJson(clienteId, entrenadorId, null), 201);

        assertEquals(2, ((java.util.List<?>) JsonPath.read(enviarGet("/api/rutinas/cliente/" + clienteId), "$")).size());
    }

    @Test
    @DisplayName("Entrenador inactivo: no recibe rutinas y no se reactivan las suyas")
    void entrenadorInactivo() throws Exception {
        long clienteId = crearCliente();
        long entrenadorId = crearEntrenador();
        long rutinaId = ((Number) JsonPath.read(
                enviarPost("/api/rutinas", rutinaJson(clienteId, entrenadorId, null), 201), "$.id")).longValue();
        enviarPut("/api/rutinas/" + rutinaId + "/desactivar", null, 200);

        enviarPut("/api/entrenadores/" + entrenadorId + "/desactivar", null, 200);

        enviarPost("/api/rutinas", rutinaJson(clienteId, entrenadorId, null), 400);
        enviarPut("/api/rutinas/" + rutinaId + "/reactivar", null, 400);
    }

    @Test
    @DisplayName("Validaciones de rutina: objetivo, series, día repetido, referencias inexistentes")
    void validacionesRutina() throws Exception {
        long clienteId = crearCliente();
        long entrenadorId = crearEntrenador();

        // sin objetivo
        enviarPost("/api/rutinas", """
                {"nombre":"X","objetivo":"","clienteId":%d,"entrenadorId":%d}
                """.formatted(clienteId, entrenadorId), 400);

        // series = 0
        String seriesCero = """
                [{"diaSemana":"LUNES","ejercicios":[{"nombre":"Sentadilla","series":0,"repeticiones":10}]}]
                """;
        String r = enviarPost("/api/rutinas", rutinaJson(clienteId, entrenadorId, seriesCero), 400);
        assertEquals("VALIDATION_ERROR", JsonPath.read(r, "$.codigo"));

        // día repetido
        String repetido = """
                [{"diaSemana":"LUNES","ejercicios":[]},{"diaSemana":"LUNES","ejercicios":[]}]
                """;
        enviarPost("/api/rutinas", rutinaJson(clienteId, entrenadorId, repetido), 400);

        // día inválido
        enviarPost("/api/rutinas", rutinaJson(clienteId, entrenadorId,
                "[{\"diaSemana\":\"FUNDAY\",\"ejercicios\":[]}]"), 400);

        // cliente / entrenador inexistentes → 404
        enviarPost("/api/rutinas", rutinaJson(999999, entrenadorId, null), 404);
        enviarPost("/api/rutinas", rutinaJson(clienteId, 999999, null), 404);
        mvc.perform(get("/api/rutinas/999999").with(admin())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Dashboard: indicadores de membresías y rutinas (sin pagos)")
    void dashboard() throws Exception {
        long clienteId = crearCliente();
        long entrenadorId = crearEntrenador();
        enviarPost("/api/rutinas", rutinaJson(clienteId, entrenadorId, null), 201);

        String resumen = enviarGet("/api/reportes/resumen");
        assertTrue(((Number) JsonPath.read(resumen, "$.totalClientes")).longValue() >= 1);
        assertTrue(((Number) JsonPath.read(resumen, "$.rutinasActivas")).longValue() >= 1);
        assertTrue(((Number) JsonPath.read(resumen, "$.entrenadoresActivos")).longValue() >= 1);
        assertFalse(resumen.toLowerCase().contains("pago"));
    }

    @Test
    @DisplayName("Progreso de rutina: PATCH toggle-completado, registro de peso y validaciones")
    void progresoEjerciciosRutina() throws Exception {
        long clienteId = crearCliente();
        long entrenadorId = crearEntrenador();

        String creada = enviarPost("/api/rutinas", rutinaJson(clienteId, entrenadorId, DIAS_COMPLETOS), 201);
        long rutinaId = ((Number) JsonPath.read(creada, "$.id")).longValue();
        long ej1Id = ((Number) JsonPath.read(creada, "$.dias[0].ejercicios[0].id")).longValue();
        assertEquals(0, (int) JsonPath.read(creada, "$.ejerciciosCompletados"));
        assertEquals(0, (int) JsonPath.read(creada, "$.porcentajeProgreso"));

        // 1. Marcar ejercicio como completado (1 de 3 = 33%)
        String r1 = json(mvc.perform(patch("/api/rutinas/" + rutinaId + "/ejercicios/" + ej1Id + "/toggle-completado")
                .with(admin())).andExpect(status().isOk()));
        assertEquals(1, (int) JsonPath.read(r1, "$.ejerciciosCompletados"));
        assertEquals(33, (int) JsonPath.read(r1, "$.porcentajeProgreso"));
        assertTrue((Boolean) JsonPath.read(r1, "$.dias[0].ejercicios[0].completado"));

        // 2. Registrar peso utilizado (80.5 kg)
        String rPeso = json(mvc.perform(patch("/api/rutinas/" + rutinaId + "/ejercicios/" + ej1Id + "/peso")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pesoKg\":80.5}"))
                .andExpect(status().isOk()));
        assertEquals(80.5, ((Number) JsonPath.read(rPeso, "$.dias[0].ejercicios[0].pesoKg")).doubleValue());

        // 3. Peso negativo o mayor a 1000 kg -> 400
        mvc.perform(patch("/api/rutinas/" + rutinaId + "/ejercicios/" + ej1Id + "/peso")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pesoKg\":-5}"))
                .andExpect(status().isBadRequest());

        mvc.perform(patch("/api/rutinas/" + rutinaId + "/ejercicios/" + ej1Id + "/peso")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pesoKg\":1250.0}"))
                .andExpect(status().isBadRequest());

        // 4. Ejercicio inexistente o ajeno -> 404
        mvc.perform(patch("/api/rutinas/" + rutinaId + "/ejercicios/999999/toggle-completado")
                .with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Trazabilidad en inscripciones: fechaActivacion, inscripcionOrigenId, fechaCancelacion y motivo")
    void trazabilidadInscripcionesEndToEnd() throws Exception {
        long clienteId = crearCliente();
        long planId = crearPlan("PlanTrazabilidad", 30);

        long insId = asignar(clienteId, planId, 201);
        String activada = enviarPut("/api/inscripciones/" + insId + "/activar", null, 200);
        assertTrue(JsonPath.read(activada, "$.fechaActivacion") != null);

        String renovada = enviarPost("/api/inscripciones/" + insId + "/renovar?observaciones=Continuidad", "", 201);
        assertEquals(insId, ((Number) JsonPath.read(renovada, "$.inscripcionOrigenId")).longValue());

        String cancelada = enviarPut("/api/inscripciones/" + insId + "/cancelar",
                "{\"motivo\":\"Mudanza de ciudad\"}", 200);
        assertEquals("CANCELADA", JsonPath.read(cancelada, "$.estado"));
        assertEquals("Mudanza de ciudad", JsonPath.read(cancelada, "$.motivoCancelacion"));
        assertTrue(JsonPath.read(cancelada, "$.fechaCancelacion") != null);
    }

    @Test
    @DisplayName("Seguridad: cambio de contraseña valida clave actual con BCrypt y actualiza hash")
    void cambioPasswordSeguro() throws Exception {
        // 1. Sin autenticación -> 401
        mvc.perform(put("/api/auth/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"passwordActual\":\"ClaveSoloParaPruebas2026\",\"passwordNueva\":\"NuevaClaveSegura2026\"}"))
                .andExpect(status().isUnauthorized());

        // 2. Contraseña actual incorrecta -> 400
        enviarPut("/api/auth/password",
                "{\"passwordActual\":\"Incorrecta123\",\"passwordNueva\":\"NuevaClaveSegura2026\"}", 400);

        // 3. Nueva contraseña igual a la actual -> 400
        enviarPut("/api/auth/password",
                "{\"passwordActual\":\"ClaveSoloParaPruebas2026\",\"passwordNueva\":\"ClaveSoloParaPruebas2026\"}", 400);

        // 4. Cambio exitoso -> 200 y el usuario autentica con la nueva clave
        enviarPut("/api/auth/password",
                "{\"passwordActual\":\"ClaveSoloParaPruebas2026\",\"passwordNueva\":\"NuevaClaveSegura2026\"}", 200);

        mvc.perform(get("/api/auth/me").with(httpBasic("admin", "NuevaClaveSegura2026")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
    }
}
