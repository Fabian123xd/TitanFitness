const API_CLIENTES = "http://localhost:8080/api/clientes";

async function cargarClientes() {

    const tabla = document.getElementById("tablaClientes");

    try {

        const respuesta = await fetch(API_CLIENTES);

        if (!respuesta.ok) {
            throw new Error("No se pudieron obtener los clientes");
        }

        const clientes = await respuesta.json();
        // Actualizar contador de clientes
const contador = document.getElementById("contadorClientes");

if (contador) {
    contador.textContent = clientes.length;
}

        tabla.innerHTML = "";

        if (clientes.length === 0) {

            tabla.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center">
                        No hay clientes registrados.
                    </td>
                </tr>
            `;

            return;
        }

        clientes.forEach(cliente => {

            const fila = document.createElement("tr");

            fila.innerHTML = `
                <td>${cliente.id}</td>
                <td>${cliente.nombres}</td>
                <td>${cliente.apellidos}</td>
                <td>${cliente.dni}</td>
                <td>${cliente.telefono ?? "-"}</td>
                <td>${cliente.correo ?? "-"}</td>
                <td>${cliente.fechaRegistro ?? "-"}</td>

                <td>
                    <button class="btn btn-sm btn-warning"
        onclick="editarCliente(${cliente.id})">
    <i class="fa-solid fa-pen"></i>
</button>

                  <button class="btn btn-sm btn-danger"
        onclick="eliminarCliente(${cliente.id})">
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
                <td colspan="8" class="text-center text-danger">
                    Error al cargar los clientes.
                </td>
            </tr>
        `;
    }
}

document.addEventListener("DOMContentLoaded", cargarClientes);
const formCliente = document.getElementById("formCliente");

formCliente.addEventListener("submit", async function(event) {

    event.preventDefault();

    const cliente = {
        nombres: document.getElementById("nombres").value.trim(),
        apellidos: document.getElementById("apellidos").value.trim(),
        dni: document.getElementById("dni").value.trim(),
        telefono: document.getElementById("telefono").value.trim(),
        correo: document.getElementById("correo").value.trim()
    };

    // Si existe este ID, estamos editando
    const clienteId = formCliente.dataset.clienteId;

    const url = clienteId
        ? `${API_CLIENTES}/${clienteId}`
        : API_CLIENTES;

    const metodo = clienteId ? "PUT" : "POST";

    try {

        const respuesta = await fetch(url, {
            method: metodo,
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(cliente)
        });

        if (!respuesta.ok) {
            throw new Error("No se pudo guardar el cliente");
        }

        formCliente.reset();

        // Quitar modo edición
        delete formCliente.dataset.clienteId;

        document.querySelector("#modalCliente .modal-title").textContent =
            "Registrar nuevo cliente";

        const modalElemento = document.getElementById("modalCliente");
        const modal = bootstrap.Modal.getInstance(modalElemento);

        modal.hide();

        await cargarClientes();

        alert(
            clienteId
                ? "Cliente actualizado correctamente"
                : "Cliente registrado correctamente"
        );

    } catch (error) {

        console.error(error);
        alert("Error al guardar el cliente");
    }
});
function nuevoCliente() {

    formCliente.reset();

    delete formCliente.dataset.clienteId;

    document.querySelector("#modalCliente .modal-title").textContent =
        "Registrar nuevo cliente";
}
async function editarCliente(id) {

    try {
        const respuesta = await fetch(`${API_CLIENTES}/${id}`);

        if (!respuesta.ok) {
            throw new Error("No se pudo obtener el cliente");
        }

        const cliente = await respuesta.json();

        document.getElementById("nombres").value = cliente.nombres;
        document.getElementById("apellidos").value = cliente.apellidos;
        document.getElementById("dni").value = cliente.dni;
        document.getElementById("telefono").value = cliente.telefono ?? "";
        document.getElementById("correo").value = cliente.correo ?? "";

        // Guardar el ID del cliente que estamos editando
        formCliente.dataset.clienteId = cliente.id;

        document.querySelector("#modalCliente .modal-title").textContent =
            "Editar cliente";

        const modal = new bootstrap.Modal(
            document.getElementById("modalCliente")
        );

        modal.show();

    } catch (error) {
        console.error(error);
        alert("Error al cargar el cliente");
    }
}
async function eliminarCliente(id) {

    const confirmar = confirm(
        "¿Estás seguro de que deseas eliminar este cliente?"
    );

    if (!confirmar) {
        return;
    }

    try {

        const respuesta = await fetch(`${API_CLIENTES}/${id}`, {
            method: "DELETE"
        });

        if (!respuesta.ok) {
            throw new Error("No se pudo eliminar el cliente");
        }

        await cargarClientes();

        alert("Cliente eliminado correctamente");

    } catch (error) {

        console.error(error);
        alert("Error al eliminar el cliente");
    }
}
// ================================
// BUSCADOR DE CLIENTES
// ================================

const buscarCliente = document.getElementById("buscarCliente");

if (buscarCliente) {

    buscarCliente.addEventListener("input", function() {

        const texto = this.value
            .toLowerCase()
            .trim();

        const filas = document.querySelectorAll(
            "#tablaClientes tr"
        );

        filas.forEach(fila => {

            const contenido =
                fila.textContent.toLowerCase();

            fila.style.display =
                contenido.includes(texto)
                    ? ""
                    : "none";
        });

    });
}