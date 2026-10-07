const API_MEMBRESIAS = "http://localhost:8080/api/membresias";

const formMembresia = document.getElementById("formMembresia");

// ================================
// LISTAR MEMBRESÍAS
// ================================
async function cargarMembresias() {

    const tabla = document.getElementById("tablaMembresias");

    try {

        const respuesta = await fetch(API_MEMBRESIAS);

        if (!respuesta.ok) {
            throw new Error("No se pudieron obtener las membresías");
        }

        const membresias = await respuesta.json();
    // ================================
// CONTADORES
// ================================

const contadorMembresias =
    document.getElementById("contadorMembresias");

const contadorActivas =
    document.getElementById("contadorActivas");

if (contadorMembresias) {
    contadorMembresias.textContent = membresias.length;
}

if (contadorActivas) {

    const activas = membresias.filter(
        membresia => membresia.activo === true
    );

    contadorActivas.textContent = activas.length;
}

        tabla.innerHTML = "";

        if (membresias.length === 0) {

            tabla.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center">
                        No hay membresías registradas.
                    </td>
                </tr>
            `;

            return;
        }

        membresias.forEach(membresia => {

            const fila = document.createElement("tr");

            fila.innerHTML = `
                <td>${membresia.id}</td>

                <td>
                    <strong>${membresia.nombre}</strong>
                </td>

                <td>${membresia.descripcion ?? "-"}</td>

                <td>
                    ${membresia.duracionDias} días
                </td>

                <td>
                    S/ ${Number(membresia.precio).toFixed(2)}
                </td>

                <td>
                    ${
                        membresia.activo
                            ? '<span class="badge bg-success">Activa</span>'
                            : '<span class="badge bg-secondary">Inactiva</span>'
                    }
                </td>

                <td>
                    <button class="btn btn-sm btn-warning"
                            onclick="editarMembresia(${membresia.id})">
                        <i class="fa-solid fa-pen"></i>
                    </button>

                    <button class="btn btn-sm btn-danger"
        onclick="eliminarMembresia(${membresia.id})">
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
                <td colspan="7"
                    class="text-center text-danger">
                    Error al cargar las membresías.
                </td>
            </tr>
        `;
    }
}


// ================================
// REGISTRAR / ACTUALIZAR
// ================================
formMembresia.addEventListener("submit", async function(event) {

    event.preventDefault();

    const membresia = {

        nombre:
            document.getElementById("nombre").value.trim(),

        descripcion:
            document.getElementById("descripcion").value.trim(),

        duracionDias:
            parseInt(
                document.getElementById("duracionDias").value
            ),

        precio:
            parseFloat(
                document.getElementById("precio").value
            ),

        activo:
            document.getElementById("activo").checked
    };

    // Saber si estamos creando o editando
    const membresiaId =
        formMembresia.dataset.membresiaId;

    const url = membresiaId
        ? `${API_MEMBRESIAS}/${membresiaId}`
        : API_MEMBRESIAS;

    const metodo =
        membresiaId ? "PUT" : "POST";

    try {

        const respuesta = await fetch(url, {

            method: metodo,

            headers: {
                "Content-Type": "application/json"
            },

            body: JSON.stringify(membresia)
        });

        if (!respuesta.ok) {
            throw new Error(
                "No se pudo guardar la membresía"
            );
        }

        formMembresia.reset();

        // Salir del modo edición
        delete formMembresia.dataset.membresiaId;

        // Activa por defecto
        document.getElementById("activo").checked = true;

        // Restaurar título
        document.querySelector(
            "#modalMembresia .modal-title"
        ).textContent = "Registrar nueva membresía";

        // Cerrar modal
        const modalElemento =
            document.getElementById("modalMembresia");

        const modal =
            bootstrap.Modal.getInstance(modalElemento);

        modal.hide();

        // Actualizar tabla
        await cargarMembresias();

        alert(
            membresiaId
                ? "Membresía actualizada correctamente"
                : "Membresía registrada correctamente"
        );

    } catch (error) {

        console.error(error);

        alert("Error al guardar la membresía");
    }
});


// ================================
// EDITAR MEMBRESÍA
// ================================
async function editarMembresia(id) {

    try {

        const respuesta =
            await fetch(`${API_MEMBRESIAS}/${id}`);

        if (!respuesta.ok) {
            throw new Error(
                "No se pudo obtener la membresía"
            );
        }

        const membresia =
            await respuesta.json();

        document.getElementById("nombre").value =
            membresia.nombre;

        document.getElementById("descripcion").value =
            membresia.descripcion ?? "";

        document.getElementById("duracionDias").value =
            membresia.duracionDias;

        document.getElementById("precio").value =
            membresia.precio;

        document.getElementById("activo").checked =
            membresia.activo;

        // Guardar ID para posteriormente hacer PUT
        formMembresia.dataset.membresiaId =
            membresia.id;

        document.querySelector(
            "#modalMembresia .modal-title"
        ).textContent = "Editar membresía";

        const modal =
            new bootstrap.Modal(
                document.getElementById("modalMembresia")
            );

        modal.show();

    } catch (error) {

        console.error(error);

        alert("Error al cargar la membresía");
    }
}


// ================================
// NUEVA MEMBRESÍA
// ================================
function nuevaMembresia() {

    formMembresia.reset();

    delete formMembresia.dataset.membresiaId;

    document.getElementById("activo").checked = true;

    document.querySelector(
        "#modalMembresia .modal-title"
    ).textContent = "Registrar nueva membresía";
}
async function eliminarMembresia(id) {

    const confirmar = confirm(
        "¿Estás seguro de que deseas eliminar esta membresía?"
    );

    if (!confirmar) {
        return;
    }

    try {

        const respuesta = await fetch(
            `${API_MEMBRESIAS}/${id}`,
            {
                method: "DELETE"
            }
        );

        if (!respuesta.ok) {
            throw new Error(
                "No se pudo eliminar la membresía"
            );
        }

        await cargarMembresias();

        alert("Membresía eliminada correctamente");

    } catch (error) {

        console.error(error);

        alert(
            "No se pudo eliminar la membresía. Puede estar asociada a una inscripción."
        );
    }
}

// ================================
// INICIAR
// ================================
document.addEventListener(
    "DOMContentLoaded",
    cargarMembresias
);
// ================================
// BUSCADOR DE MEMBRESÍAS
// ================================

const buscarMembresia =
    document.getElementById("buscarMembresia");

if (buscarMembresia) {

    buscarMembresia.addEventListener(
        "input",
        function() {

            const texto =
                this.value.toLowerCase().trim();

            const filas =
                document.querySelectorAll(
                    "#tablaMembresias tr"
                );

            filas.forEach(fila => {

                const contenido =
                    fila.textContent.toLowerCase();

                fila.style.display =
                    contenido.includes(texto)
                        ? ""
                        : "none";
            });
        }
    );
}