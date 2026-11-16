const API_MEMBRESIAS = ENDPOINTS.MEMBRESIAS;

const formMembresia = document.getElementById("formMembresia");

function mostrarAlertaPlan(mensaje, tipo = "success") {
    mostrarAlertaUI("alertaMembresias", mensaje, tipo, 4500);
}

function renderizarCatalogoVisual(membresias) {
    const contenedor = document.getElementById("catalogoVisualPlanes");
    if (!contenedor) return;
    contenedor.innerHTML = "";

    membresias.forEach(m => {
        const card = document.createElement("div");
        card.className = `plan-card-cobalt ${m.activo ? "" : "plan-inactive"}`;
        card.innerHTML = `
            <div>
                <div class="d-flex justify-content-between align-items-start gap-2 mb-2">
                    <span class="badge ${m.activo ? "bg-success" : "bg-secondary"}">
                        ${m.activo ? "Disponible" : "Inactivo"}
                    </span>
                    <span class="badge bg-primary">${m.duracionDias} días</span>
                </div>
                <h5 class="fw-bold mb-1" style="color: var(--navy-900);">${m.nombre}</h5>
                <p class="text-muted small mb-3">${m.descripcion || "Acceso a entrenamiento y seguimiento personalizado."}</p>
            </div>
            <div class="pt-3 border-top d-flex justify-content-between align-items-end">
                <div class="plan-price-tag">
                    S/ ${Number(m.precio).toFixed(2)} <span>/ ${m.duracionDias}d</span>
                </div>
                <div class="d-flex gap-1">
                    <button type="button" class="btn btn-sm btn-outline-primary" onclick="editarMembresia(${m.id})" title="Editar plan">
                        <i class="fa-solid fa-pen"></i>
                    </button>
                    <button type="button" class="btn btn-sm ${m.activo ? "btn-outline-secondary" : "btn-outline-success"}"
                            onclick="cambiarEstadoMembresia(${m.id})"
                            title="${m.activo ? "Desactivar" : "Activar"}">
                        <i class="fa-solid ${m.activo ? "fa-ban" : "fa-check"}"></i>
                    </button>
                </div>
            </div>
        `;
        contenedor.appendChild(card);
    });
}

async function cargarMembresias() {
    const tabla = document.getElementById("tablaMembresias");

    try {
        const respuesta = await fetch(API_MEMBRESIAS);
        if (!respuesta.ok) {
            throw new Error("No se pudieron obtener las membresías");
        }

        const membresias = await respuesta.json();

        const contadorMembresias = document.getElementById("contadorMembresias");
        const contadorActivas = document.getElementById("contadorActivas");
        const contadorInactivas = document.getElementById("contadorInactivas");

        const totalActivas = membresias.filter(m => m.activo === true).length;
        if (contadorMembresias) {
            contadorMembresias.textContent = membresias.length;
        }
        if (contadorActivas) {
            contadorActivas.textContent = totalActivas;
        }
        if (contadorInactivas) {
            contadorInactivas.textContent = membresias.length - totalActivas;
        }

        renderizarCatalogoVisual(membresias);
        tabla.innerHTML = "";

        if (membresias.length === 0) {
            tabla.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center text-muted py-4">
                        No hay planes de membresía registrados.
                    </td>
                </tr>
            `;
            return;
        }

        membresias.forEach(membresia => {
            const fila = document.createElement("tr");

            const btnToggleIcon = membresia.activo ? "fa-ban" : "fa-check";
            const btnToggleClass = membresia.activo ? "btn-outline-secondary" : "btn-outline-success";
            const btnToggleTitle = membresia.activo ? "Desactivar plan" : "Activar plan";

            fila.innerHTML = `
                <td class="fw-semibold text-muted">#${membresia.id}</td>
                <td><strong>${membresia.nombre}</strong></td>
                <td>${membresia.descripcion || "-"}</td>
                <td><span class="badge bg-primary">${membresia.duracionDias} días</span></td>
                <td class="fw-bold">S/ ${Number(membresia.precio).toFixed(2)}</td>
                <td>
                    ${
                        membresia.activo
                            ? '<span class="badge bg-success">Plan habilitado</span>'
                            : '<span class="badge bg-secondary">Plan inactivo</span>'
                    }
                </td>
                <td class="text-center text-nowrap">
                    <button class="btn btn-sm btn-warning me-1"
                            onclick="editarMembresia(${membresia.id})"
                            title="Editar características del plan">
                        <i class="fa-solid fa-pen"></i>
                    </button>
                    <button class="btn btn-sm ${btnToggleClass} me-1"
                            onclick="cambiarEstadoMembresia(${membresia.id})"
                            title="${btnToggleTitle}">
                        <i class="fa-solid ${btnToggleIcon}"></i>
                    </button>
                    <button class="btn btn-sm btn-danger"
                            onclick="eliminarMembresia(${membresia.id})"
                            title="Eliminar plan (solo si no tiene inscripciones)">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </td>
            `;

            tabla.appendChild(fila);
        });
    } catch (error) {
        console.error(error);
        tabla.innerHTML = `
            <tr>
                <td colspan="7" class="text-center text-danger py-4">
                    Error al cargar los planes de membresía.
                </td>
            </tr>
        `;
    }
}

formMembresia.addEventListener("submit", async function (event) {
    event.preventDefault();

    const membresia = {
        nombre: document.getElementById("nombre").value.trim(),
        descripcion: document.getElementById("descripcion").value.trim(),
        duracionDias: parseInt(document.getElementById("duracionDias").value, 10),
        precio: parseFloat(document.getElementById("precio").value),
        activo: document.getElementById("activo").checked
    };

    const membresiaId = formMembresia.dataset.membresiaId;
    const url = membresiaId ? `${API_MEMBRESIAS}/${membresiaId}` : API_MEMBRESIAS;
    const metodo = membresiaId ? "PUT" : "POST";

    try {
        const respuesta = await fetch(url, {
            method: metodo,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(membresia)
        });

        if (!respuesta.ok) {
            const msg = await extraerMensajeError(respuesta, "No se pudo guardar el plan de membresía");
            throw new Error(msg);
        }

        formMembresia.reset();
        delete formMembresia.dataset.membresiaId;
        document.getElementById("activo").checked = true;
        document.querySelector("#modalMembresia .modal-title").textContent = "Registrar nuevo plan de membresía";

        const modalElemento = document.getElementById("modalMembresia");
        const modal = bootstrap.Modal.getInstance(modalElemento);
        if (modal) modal.hide();

        await cargarMembresias();
        mostrarAlertaPlan(
            membresiaId ? "Plan actualizado correctamente." : "Plan registrado correctamente.",
            "success"
        );
    } catch (error) {
        console.error(error);
        mostrarAlertaPlan(error.message, "danger");
    }
});

async function editarMembresia(id) {
    try {
        const respuesta = await fetch(`${API_MEMBRESIAS}/${id}`);
        if (!respuesta.ok) {
            throw new Error("No se pudo obtener el plan");
        }

        const membresia = await respuesta.json();
        document.getElementById("nombre").value = membresia.nombre;
        document.getElementById("descripcion").value = membresia.descripcion ?? "";
        document.getElementById("duracionDias").value = membresia.duracionDias;
        document.getElementById("precio").value = membresia.precio;
        document.getElementById("activo").checked = membresia.activo;

        formMembresia.dataset.membresiaId = membresia.id;
        document.querySelector("#modalMembresia .modal-title").textContent = "Editar plan de membresía";

        const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById("modalMembresia"));
        modal.show();
    } catch (error) {
        console.error(error);
        mostrarAlertaPlan("Error al cargar el plan de membresía", "danger");
    }
}

async function cambiarEstadoMembresia(id) {
    try {
        const respuesta = await fetch(`${API_MEMBRESIAS}/${id}/toggle`, { method: "PUT" });
        if (!respuesta.ok) {
            const msg = await extraerMensajeError(respuesta, "No se pudo cambiar el estado del plan");
            throw new Error(msg);
        }
        await cargarMembresias();
        mostrarAlertaPlan("Disponibilidad del plan actualizada.", "info");
    } catch (error) {
        console.error(error);
        mostrarAlertaPlan(error.message, "danger");
    }
}

function nuevaMembresia() {
    formMembresia.reset();
    delete formMembresia.dataset.membresiaId;
    document.getElementById("activo").checked = true;
    document.querySelector("#modalMembresia .modal-title").textContent = "Registrar nuevo plan de membresía";
}

async function eliminarMembresia(id) {
    if (!confirm("¿Estás seguro de que deseas eliminar este plan?")) {
        return;
    }

    try {
        const respuesta = await fetch(`${API_MEMBRESIAS}/${id}`, { method: "DELETE" });
        if (!respuesta.ok) {
            const msg = await extraerMensajeError(
                respuesta,
                "No se puede eliminar un plan con inscripciones asociadas. Desactívelo en su lugar."
            );
            throw new Error(msg);
        }

        await cargarMembresias();
        mostrarAlertaPlan("Plan eliminado correctamente.", "success");
    } catch (error) {
        console.error(error);
        mostrarAlertaPlan(error.message, "danger");
    }
}

document.addEventListener("DOMContentLoaded", cargarMembresias);

const buscarMembresia = document.getElementById("buscarMembresia");
if (buscarMembresia) {
    buscarMembresia.addEventListener("input", function () {
        const texto = this.value.toLowerCase().trim();
        const filas = document.querySelectorAll("#tablaMembresias tr");
        filas.forEach(fila => {
            const contenido = fila.textContent.toLowerCase();
            fila.style.display = contenido.includes(texto) ? "" : "none";
        });
    });
}
