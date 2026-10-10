
const API_INSCRIPCIONES = "http://localhost:8080/api/inscripciones";
const API_CLIENTES = "http://localhost:8080/api/clientes";
const API_MEMBRESIAS = "http://localhost:8080/api/membresias";

let inscripcionesRegistradas = [];

const formInscripcion = document.getElementById("formInscripcion");
const modalInscripcion = document.getElementById("modalInscripcion");
const clienteSelect = document.getElementById("clienteId");
const membresiaSelect = document.getElementById("membresiaId");
const mensajeInscripcion = document.getElementById("mensajeInscripcion");

// INICIAR PÁGINA
document.addEventListener("DOMContentLoaded", () => {
    cargarInscripciones();

    document.getElementById("buscarInscripcion")
        .addEventListener("input", event => {
            mostrarInscripciones(event.target.value);
        });
});

// CONSULTAR INSCRIPCIONES
async function cargarInscripciones() {
    const tabla = document.getElementById("tablaInscripciones");

    try {
        const respuesta = await fetch(API_INSCRIPCIONES);

        if (!respuesta.ok) {
            throw new Error("No se pudieron consultar las inscripciones");
        }

        inscripcionesRegistradas = await respuesta.json();

        mostrarInscripciones(
            document.getElementById("buscarInscripcion").value
        );

        actualizarResumen();

    } catch (error) {
        console.error(error);

        tabla.innerHTML = `
            <tr>
                <td colspan="7" class="text-center text-danger py-4">
                    No se pudieron cargar las inscripciones.
                    Verifica que Spring Boot esté funcionando.
                </td>
            </tr>
        `;
    }
}

// DETERMINAR ESTADO VISIBLE
function obtenerEstado(inscripcion) {
    const estado = inscripcion.estado || "PENDIENTE";

    if (estado === "ACTIVA" && inscripcion.fechaFin) {
        const hoy = new Date();
        const fechaActual = [
            hoy.getFullYear(),
            String(hoy.getMonth() + 1).padStart(2, "0"),
            String(hoy.getDate()).padStart(2, "0")
        ].join("-");

        if (inscripcion.fechaFin < fechaActual) {
            return "VENCIDA";
        }
    }

    return estado;
}

// MOSTRAR INSCRIPCIONES
function mostrarInscripciones(busqueda = "") {
    const tabla = document.getElementById("tablaInscripciones");
    const texto = busqueda.toLowerCase().trim();

    const filtradas = inscripcionesRegistradas.filter(inscripcion => {
        const cliente = inscripcion.cliente;
        const membresia = inscripcion.membresia;

        const nombreCliente = cliente
            ? `${cliente.nombres || ""} ${cliente.apellidos || ""}`
            : "";

        return [
            inscripcion.id,
            nombreCliente,
            membresia?.nombre,
            obtenerEstado(inscripcion)
        ].some(valor =>
            String(valor ?? "").toLowerCase().includes(texto)
        );
    });

    if (filtradas.length === 0) {
        tabla.innerHTML = `
            <tr>
                <td colspan="7" class="text-center py-4">
                    No se encontraron inscripciones.
                </td>
            </tr>
        `;
        return;
    }

    tabla.innerHTML = "";

    filtradas.forEach(inscripcion => {
        const cliente = inscripcion.cliente;

        const nombreCliente = cliente
            ? `${cliente.nombres || ""} ${cliente.apellidos || ""}`
            : "No disponible";

        const nombreMembresia =
            inscripcion.membresia?.nombre || "No disponible";

        const estado = obtenerEstado(inscripcion);

        const fila = document.createElement("tr");

        const valores = [
            inscripcion.id,
            nombreCliente,
            nombreMembresia,
            inscripcion.fechaInicio || "-",
            inscripcion.fechaFin || "-",
            estado
        ];

        valores.forEach(valor => {
            const celda = document.createElement("td");
            celda.textContent = valor;
            fila.appendChild(celda);
        });

        // COLUMNA DE ACCIÓN
        const celdaAccion = document.createElement("td");

        if (estado === "PENDIENTE") {
            const enlace = document.createElement("a");
            enlace.href = "pagos.html";
            enlace.className = "btn btn-sm btn-success";
            enlace.textContent = "Registrar pago";

            celdaAccion.appendChild(enlace);
        } else {
            celdaAccion.textContent = "-";
        }

        fila.appendChild(celdaAccion);
        tabla.appendChild(fila);
    });
}

// ACTUALIZAR CONTADORES
function actualizarResumen() {
    const total = inscripcionesRegistradas.length;

    const activas = inscripcionesRegistradas.filter(
        inscripcion => obtenerEstado(inscripcion) === "ACTIVA"
    ).length;

    const pendientes = inscripcionesRegistradas.filter(
        inscripcion => obtenerEstado(inscripcion) === "PENDIENTE"
    ).length;

    const vencidas = inscripcionesRegistradas.filter(
        inscripcion => obtenerEstado(inscripcion) === "VENCIDA"
    ).length;

    document.getElementById("totalInscripciones").textContent = total;
    document.getElementById("inscripcionesActivas").textContent = activas;
    document.getElementById("inscripcionesPendientes").textContent = pendientes;
    document.getElementById("inscripcionesVencidas").textContent = vencidas;
}

// CARGAR CLIENTES
async function cargarClientes() {
    clienteSelect.innerHTML =
        '<option value="">Cargando clientes...</option>';

    try {
        const respuesta = await fetch(API_CLIENTES);

        if (!respuesta.ok) {
            throw new Error("No se pudieron cargar los clientes");
        }

        const clientes = await respuesta.json();

        clienteSelect.innerHTML =
            '<option value="">Seleccionar cliente</option>';

        clientes.forEach(cliente => {
            const opcion = document.createElement("option");

            opcion.value = cliente.id;
            opcion.textContent =
                `${cliente.nombres} ${cliente.apellidos} - DNI: ${cliente.dni}`;

            clienteSelect.appendChild(opcion);
        });

    } catch (error) {
        console.error(error);

        clienteSelect.innerHTML =
            '<option value="">Error al cargar clientes</option>';
    }
}

// CARGAR MEMBRESÍAS ACTIVAS
async function cargarMembresias() {
    membresiaSelect.innerHTML =
        '<option value="">Cargando membresías...</option>';

    try {
        const respuesta = await fetch(API_MEMBRESIAS);

        if (!respuesta.ok) {
            throw new Error("No se pudieron cargar las membresías");
        }

        const membresias = await respuesta.json();

        membresiaSelect.innerHTML =
            '<option value="">Seleccionar membresía</option>';

        membresias
            .filter(membresia => membresia.activo === true)
            .forEach(membresia => {
                const opcion = document.createElement("option");

                opcion.value = membresia.id;
                opcion.textContent =
                    `${membresia.nombre} - S/ ${Number(membresia.precio).toFixed(2)} - ${membresia.duracionDias} días`;

                membresiaSelect.appendChild(opcion);
            });

    } catch (error) {
        console.error(error);

        membresiaSelect.innerHTML =
            '<option value="">Error al cargar membresías</option>';
    }
}

// ABRIR FORMULARIO
modalInscripcion.addEventListener("show.bs.modal", () => {
    formInscripcion.reset();

    mensajeInscripcion.textContent = "";

    cargarClientes();
    cargarMembresias();
});

// REGISTRAR INSCRIPCIÓN
formInscripcion.addEventListener("submit", async event => {
    event.preventDefault();

    const clienteId = clienteSelect.value;
    const membresiaId = membresiaSelect.value;

    const boton = formInscripcion.querySelector(
        'button[type="submit"]'
    );

    if (!clienteId || !membresiaId) {
        mensajeInscripcion.className = "small text-danger";
        mensajeInscripcion.textContent =
            "Selecciona un cliente y una membresía.";
        return;
    }

    // Evitar duplicados desde la interfaz
    const tieneInscripcionVigente = inscripcionesRegistradas.some(
        inscripcion =>
            String(inscripcion.cliente?.id) === clienteId &&
            ["ACTIVA", "PENDIENTE"].includes(
                obtenerEstado(inscripcion)
            )
    );

    if (tieneInscripcionVigente) {
        mensajeInscripcion.className = "small text-danger";
        mensajeInscripcion.textContent =
            "Este cliente ya tiene una inscripción activa o pendiente.";
        return;
    }

    const parametros = new URLSearchParams({
        clienteId,
        membresiaId
    });

    boton.disabled = true;
    mensajeInscripcion.className = "small text-muted";
    mensajeInscripcion.textContent = "Registrando inscripción...";

    try {
        const respuesta = await fetch(
            `${API_INSCRIPCIONES}?${parametros.toString()}`,
            { method: "POST" }
        );

        if (!respuesta.ok) {
            throw new Error("No se pudo registrar la inscripción.");
        }

        bootstrap.Modal.getInstance(modalInscripcion).hide();

        await cargarInscripciones();

        alert(
            "Inscripción registrada correctamente.\n" +
            "Estado: PENDIENTE.\n" +
            "Ahora puedes registrar el pago."
        );

    } catch (error) {
        console.error(error);

        mensajeInscripcion.className = "small text-danger";
        mensajeInscripcion.textContent = error.message;

    } finally {
        boton.disabled = false;
    }
});