
const API_RUTINAS = "http://localhost:8080/api/rutinas";
const API_CLIENTES = "http://localhost:8080/api/clientes";
const API_ENTRENADORES = "http://localhost:8080/api/entrenadores";

const formRutina = document.getElementById("formRutina");
const tablaRutinas = document.getElementById("tablaRutinas");
const selectCliente = document.getElementById("clienteId");
const selectEntrenador = document.getElementById("entrenadorId");
const mensajeRutinas = document.getElementById("mensajeRutinas");
const btnGuardar = document.getElementById("btnGuardarRutina");
const rutinaIdInput = document.getElementById("rutinaId");
const btnCancelarEdicion = document.getElementById("btnCancelarEdicion");

let rutinasCargadas = [];

function mostrarMensaje(texto, tipo = "success") {
    mensajeRutinas.className = `alert alert-${tipo}`;
    mensajeRutinas.textContent = texto;
}

function obtenerNombre(persona) {
    if (!persona) return "No disponible";

    const nombres = persona.nombres || persona.nombre || "";
    const apellidos = persona.apellidos || persona.apellido || "";

    return `${nombres} ${apellidos}`.trim() || `ID ${persona.id}`;
}

function crearCelda(texto) {
    const td = document.createElement("td");
    td.textContent = texto ?? "";
    return td;
}

// CARGAR CLIENTES
async function cargarClientes() {
    try {
        const respuesta = await fetch(API_CLIENTES);

        if (!respuesta.ok) {
            throw new Error("No se pudieron cargar los clientes");
        }

        const clientes = await respuesta.json();

        selectCliente.innerHTML =
            '<option value="">Seleccione un cliente</option>';

        clientes.forEach(cliente => {
            const opcion = document.createElement("option");
            opcion.value = cliente.id;
            opcion.textContent = obtenerNombre(cliente);
            selectCliente.appendChild(opcion);
        });

    } catch (error) {
        console.error(error);
        mostrarMensaje(error.message, "danger");
    }
}

// CARGAR ENTRENADORES ACTIVOS
async function cargarEntrenadores() {
    try {
        const respuesta = await fetch(`${API_ENTRENADORES}/activos`);

        if (!respuesta.ok) {
            throw new Error("No se pudieron cargar los entrenadores");
        }

        const entrenadores = await respuesta.json();

        selectEntrenador.innerHTML =
            '<option value="">Seleccione un entrenador</option>';

        entrenadores.forEach(entrenador => {
            const opcion = document.createElement("option");
            opcion.value = entrenador.id;
            opcion.textContent = obtenerNombre(entrenador);
            selectEntrenador.appendChild(opcion);
        });

    } catch (error) {
        console.error(error);
        mostrarMensaje(error.message, "danger");
    }
}

// LISTAR RUTINAS
async function cargarRutinas() {
    tablaRutinas.innerHTML = `
        <tr>
            <td colspan="8" class="text-center py-4">
                Cargando rutinas...
            </td>
        </tr>
    `;

    try {
        const respuesta = await fetch(API_RUTINAS);

        if (!respuesta.ok) {
            throw new Error("No se pudieron cargar las rutinas");
        }

        const rutinas = await respuesta.json();
        rutinasCargadas = rutinas;

        tablaRutinas.innerHTML = "";

        if (rutinas.length === 0) {
            tablaRutinas.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center text-muted py-4">
                        No hay rutinas registradas.
                    </td>
                </tr>
            `;
            return;
        }

        rutinas.forEach(rutina => {
            const fila = document.createElement("tr");

            fila.appendChild(crearCelda(rutina.id));
            fila.appendChild(crearCelda(rutina.nombre));

            const descripcion = crearCelda(
                rutina.descripcion || "Sin descripción"
            );
            descripcion.className = "descripcion-col";
            fila.appendChild(descripcion);

            fila.appendChild(crearCelda(obtenerNombre(rutina.cliente)));
            fila.appendChild(crearCelda(obtenerNombre(rutina.entrenador)));
            fila.appendChild(crearCelda(rutina.fechaAsignacion || "-"));

            // ESTADO
            const celdaEstado = document.createElement("td");
            const estado = document.createElement("span");

            estado.className = rutina.activa
                ? "badge bg-success"
                : "badge bg-secondary";

            estado.textContent = rutina.activa ? "Activa" : "Inactiva";

            celdaEstado.appendChild(estado);
            fila.appendChild(celdaEstado);

            // ACCIONES
            const celdaAcciones = document.createElement("td");
            celdaAcciones.className = "text-nowrap";

            // BOTÓN EDITAR
            const botonEditar = document.createElement("button");
            botonEditar.type = "button";
            botonEditar.className =
                "btn btn-outline-primary btn-sm me-1 mb-1";
            botonEditar.innerHTML =
                '<i class="fa-solid fa-pen me-1"></i> Editar';

            botonEditar.addEventListener("click", () => {
                editarRutina(rutina.id);
            });

            celdaAcciones.appendChild(botonEditar);

            // BOTÓN DESACTIVAR / REACTIVAR
            const botonEstado = document.createElement("button");
            botonEstado.type = "button";

            if (rutina.activa) {
                botonEstado.className =
                    "btn btn-outline-danger btn-sm mb-1";
                botonEstado.innerHTML =
                    '<i class="fa-solid fa-ban me-1"></i> Desactivar';

                botonEstado.addEventListener("click", () => {
                    desactivarRutina(rutina.id);
                });

            } else {
                botonEstado.className =
                    "btn btn-outline-success btn-sm mb-1";
                botonEstado.innerHTML =
                    '<i class="fa-solid fa-check me-1"></i> Reactivar';

                botonEstado.addEventListener("click", () => {
                    reactivarRutina(rutina.id);
                });
            }

            celdaAcciones.appendChild(botonEstado);

            fila.appendChild(celdaAcciones);
            tablaRutinas.appendChild(fila);
        });

    } catch (error) {
        console.error(error);

        tablaRutinas.innerHTML = `
            <tr>
                <td colspan="8" class="text-center text-danger py-4">
                    Error al cargar las rutinas.
                </td>
            </tr>
        `;

        mostrarMensaje(error.message, "danger");
    }
}

// REGISTRAR O ACTUALIZAR RUTINA
formRutina.addEventListener("submit", async (evento) => {
    evento.preventDefault();

    const id = rutinaIdInput.value;
    const clienteId = selectCliente.value;
    const entrenadorId = selectEntrenador.value;
    const nombre = document.getElementById("nombre").value.trim();
    const descripcion = document.getElementById("descripcion").value.trim();

    if (!clienteId || !entrenadorId || !nombre) {
        mostrarMensaje(
            "Complete los campos obligatorios.",
            "warning"
        );
        return;
    }

    const parametros = new URLSearchParams({
        clienteId,
        entrenadorId,
        nombre,
        descripcion
    });

    const url = id
        ? `${API_RUTINAS}/${id}?${parametros}`
        : `${API_RUTINAS}?${parametros}`;

    btnGuardar.disabled = true;

    try {
        const respuesta = await fetch(url, {
            method: id ? "PUT" : "POST"
        });

        if (!respuesta.ok) {
            throw new Error(
                "No se pudo guardar la rutina. Verifique los datos y el entrenador."
            );
        }

        cancelarEdicion();
        await cargarRutinas();

        mostrarMensaje(
            id
                ? "Rutina actualizada correctamente."
                : "Rutina registrada correctamente."
        );

    } catch (error) {
        console.error(error);
        mostrarMensaje(error.message, "danger");

    } finally {
        btnGuardar.disabled = false;
    }
});

// EDITAR RUTINA
async function editarRutina(id) {
    const rutina = rutinasCargadas.find(
        r => Number(r.id) === Number(id)
    );

    if (!rutina) {
        mostrarMensaje("No se encontró la rutina.", "danger");
        return;
    }

    // Actualizar opciones antes de editar
    await Promise.all([
        cargarClientes(),
        cargarEntrenadores()
    ]);

    rutinaIdInput.value = rutina.id;
    document.getElementById("nombre").value = rutina.nombre || "";
    document.getElementById("descripcion").value =
        rutina.descripcion || "";

    selectCliente.value = rutina.cliente?.id || "";

    const entrenadorId = String(rutina.entrenador?.id || "");

    // Mostrar también el entrenador asignado si está inactivo
    if (
        entrenadorId &&
        !Array.from(selectEntrenador.options)
            .some(opcion => opcion.value === entrenadorId)
    ) {
        const opcion = document.createElement("option");
        opcion.value = entrenadorId;
        opcion.textContent =
            `${obtenerNombre(rutina.entrenador)} (Inactivo)`;
        opcion.disabled = true;
        selectEntrenador.appendChild(opcion);
    }

    selectEntrenador.value = entrenadorId;

    btnGuardar.innerHTML =
        '<i class="fa-solid fa-floppy-disk me-2"></i> Guardar cambios';

    btnCancelarEdicion.classList.remove("d-none");

    mostrarMensaje(`Editando rutina #${id}`, "info");

    window.scrollTo({
        top: 0,
        behavior: "smooth"
    });
}

// CANCELAR EDICIÓN
function cancelarEdicion() {
    formRutina.reset();
    rutinaIdInput.value = "";

    btnGuardar.innerHTML =
        '<i class="fa-solid fa-floppy-disk me-2"></i> Registrar rutina';

    btnCancelarEdicion.classList.add("d-none");
}

btnCancelarEdicion.addEventListener("click", cancelarEdicion);

// DESACTIVAR RUTINA
async function desactivarRutina(id) {
    if (!confirm("¿Desea desactivar esta rutina?")) return;

    try {
        const respuesta = await fetch(
            `${API_RUTINAS}/${id}/desactivar`,
            { method: "PUT" }
        );

        if (!respuesta.ok) {
            throw new Error("No se pudo desactivar la rutina.");
        }

        await cargarRutinas();
        mostrarMensaje("Rutina desactivada correctamente.");

    } catch (error) {
        console.error(error);
        mostrarMensaje(error.message, "danger");
    }
}

// REACTIVAR RUTINA
async function reactivarRutina(id) {
    if (!confirm("¿Desea reactivar esta rutina?")) return;

    try {
        const respuesta = await fetch(
            `${API_RUTINAS}/${id}/reactivar`,
            { method: "PUT" }
        );

        if (!respuesta.ok) {
            throw new Error(
                "No se pudo reactivar. Verifique que el entrenador esté activo."
            );
        }

        await cargarRutinas();
        mostrarMensaje("Rutina reactivada correctamente.");

    } catch (error) {
        console.error(error);
        mostrarMensaje(error.message, "danger");
    }
}

// LIMPIAR FORMULARIO
formRutina.addEventListener("reset", () => {
    rutinaIdInput.value = "";

    btnGuardar.innerHTML =
        '<i class="fa-solid fa-floppy-disk me-2"></i> Registrar rutina';

    btnCancelarEdicion.classList.add("d-none");
});

// ACTUALIZAR TABLA
document.getElementById("btnActualizarRutinas")
    .addEventListener("click", async () => {
        await Promise.all([
            cargarRutinas(),
            cargarClientes(),
            cargarEntrenadores()
        ]);
    });

// INICIALIZAR
document.addEventListener("DOMContentLoaded", async () => {
    await Promise.all([
        cargarClientes(),
        cargarEntrenadores(),
        cargarRutinas()
    ]);
});
