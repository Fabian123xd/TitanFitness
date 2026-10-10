const API_PAGOS = "http://localhost:8080/api/pagos";
const API_INSCRIPCIONES = "http://localhost:8080/api/inscripciones";

let pagosRegistrados = [];
let inscripcionesPendientes = [];

const formPago = document.getElementById("formPago");
const modalPago = document.getElementById("modalPago");
const inscripcionSelect = document.getElementById("inscripcionId");
const montoInput = document.getElementById("monto");
const metodoPagoSelect = document.getElementById("metodoPago");
const montoRecibidoInput = document.getElementById("montoRecibido");
const grupoMontoRecibido = document.getElementById("grupoMontoRecibido");
const resultadoVuelto = document.getElementById("resultadoVuelto");
const vueltoCalculado = document.getElementById("vueltoCalculado");
const mensajePago = document.getElementById("mensajePago");

// INICIALIZACIÓN
document.addEventListener("DOMContentLoaded", () => {
    cargarPagos();

    document.getElementById("buscarPago")
        .addEventListener("input", event => {
            mostrarPagos(event.target.value);
        });
});

// LISTAR PAGOS
async function cargarPagos() {
    const tabla = document.getElementById("tablaPagos");

    try {
        const respuesta = await fetch(API_PAGOS);

        if (!respuesta.ok) {
            throw new Error("No se pudieron consultar los pagos");
        }

        pagosRegistrados = await respuesta.json();

        mostrarPagos(
            document.getElementById("buscarPago").value
        );
        actualizarResumen();

    } catch (error) {
        console.error(error);

        tabla.innerHTML = `
            <tr>
                <td colspan="7" class="text-center text-danger py-4">
                    No se pudieron cargar los pagos.
                </td>
            </tr>
        `;
    }
}

// MOSTRAR PAGOS EN LA TABLA
function mostrarPagos(busqueda = "") {
    const tabla = document.getElementById("tablaPagos");
    const texto = busqueda.toLowerCase().trim();

    const filtrados = pagosRegistrados.filter(pago => {
        const cliente = pago.inscripcion?.cliente;

        const nombre = cliente
            ? `${cliente.nombres || ""} ${cliente.apellidos || ""}`
            : "";

        return [
            pago.id,
            pago.inscripcion?.id,
            nombre,
            pago.metodoPago,
            pago.estado
        ].some(valor =>
            String(valor ?? "").toLowerCase().includes(texto)
        );
    });

    if (filtrados.length === 0) {
        tabla.innerHTML = `
            <tr>
                <td colspan="7" class="text-center py-4">
                    No se encontraron pagos.
                </td>
            </tr>
        `;
        return;
    }

    tabla.innerHTML = "";

    filtrados.forEach(pago => {
        const cliente = pago.inscripcion?.cliente;

        const nombreCliente = cliente
            ? `${cliente.nombres || ""} ${cliente.apellidos || ""}`
            : "No disponible";

        const fecha = pago.fechaPago
            ? new Date(pago.fechaPago).toLocaleString("es-PE")
            : "-";

        const fila = document.createElement("tr");

        const valores = [
            pago.id,
            nombreCliente,
            pago.inscripcion?.id ?? "-",
            `S/ ${Number(pago.monto).toFixed(2)}`,
            pago.metodoPago ?? "-",
            fecha,
            pago.estado ?? "-"
        ];

        valores.forEach(valor => {
            const celda = document.createElement("td");
            celda.textContent = valor;
            fila.appendChild(celda);
        });

        tabla.appendChild(fila);
    });
}

// ACTUALIZAR RESUMEN
function actualizarResumen() {
    document.getElementById("contadorPagos").textContent =
        pagosRegistrados.length;

    const total = pagosRegistrados
        .filter(pago => pago.estado === "PAGADO")
        .reduce(
            (suma, pago) => suma + Number(pago.monto || 0),
            0
        );

    document.getElementById("totalRecaudado").textContent =
        `S/ ${total.toFixed(2)}`;
}

// CARGAR INSCRIPCIONES PENDIENTES
async function cargarInscripciones() {
    inscripcionSelect.innerHTML =
        '<option value="">Cargando inscripciones...</option>';

    montoInput.value = "";
    montoRecibidoInput.value = "";
    resultadoVuelto.classList.add("d-none");

    try {
        const respuesta = await fetch(API_INSCRIPCIONES);

        if (!respuesta.ok) {
            throw new Error("No se pudieron cargar las inscripciones");
        }

        const inscripciones = await respuesta.json();

        inscripcionesPendientes = inscripciones.filter(
            inscripcion => inscripcion.estado === "PENDIENTE"
        );

        inscripcionSelect.innerHTML =
            '<option value="">Seleccionar inscripción</option>';

        inscripcionesPendientes.forEach(inscripcion => {
            const cliente = inscripcion.cliente;

            const nombre = cliente
                ? `${cliente.nombres} ${cliente.apellidos}`
                : "Cliente no disponible";

            const precio = Number(
                inscripcion.membresia?.precio ?? 0
            );

            const opcion = document.createElement("option");

            opcion.value = inscripcion.id;
            opcion.textContent =
                `${nombre} - Inscripción #${inscripcion.id} - S/ ${precio.toFixed(2)}`;

            inscripcionSelect.appendChild(opcion);
        });

        if (inscripcionesPendientes.length === 0) {
            inscripcionSelect.innerHTML =
                '<option value="">No hay inscripciones pendientes</option>';
        }

    } catch (error) {
        console.error(error);

        inscripcionSelect.innerHTML =
            '<option value="">Error al cargar inscripciones</option>';
    }
}

// SELECCIONAR INSCRIPCIÓN
inscripcionSelect.addEventListener("change", () => {
    const inscripcion = inscripcionesPendientes.find(
        item => String(item.id) === inscripcionSelect.value
    );

    montoInput.value = inscripcion
        ? Number(inscripcion.membresia.precio).toFixed(2)
        : "";

    montoRecibidoInput.value = "";
    calcularVuelto();
});

// MOSTRAR MONTO RECIBIDO SOLO PARA EFECTIVO
function actualizarMetodoPago() {
    const esEfectivo = metodoPagoSelect.value === "EFECTIVO";

    grupoMontoRecibido.classList.toggle("d-none", !esEfectivo);
    montoRecibidoInput.required = esEfectivo;

    if (!esEfectivo) {
        montoRecibidoInput.value = "";
    }

    calcularVuelto();
}

// CALCULAR VUELTO
function calcularVuelto() {
    resultadoVuelto.classList.add("d-none");

    if (metodoPagoSelect.value !== "EFECTIVO") {
        return;
    }

    if (!montoInput.value || !montoRecibidoInput.value) {
        return;
    }

    const monto = Number(montoInput.value);
    const recibido = Number(montoRecibidoInput.value);

    if (
        !Number.isFinite(monto) ||
        !Number.isFinite(recibido) ||
        monto <= 0 ||
        recibido < monto
    ) {
        return;
    }

    const vuelto = recibido - monto;

    vueltoCalculado.textContent = `S/ ${vuelto.toFixed(2)}`;
    resultadoVuelto.classList.remove("d-none");
}

// EVENTOS DEL FORMULARIO
modalPago.addEventListener("show.bs.modal", () => {
    formPago.reset();
    mensajePago.textContent = "";
    actualizarMetodoPago();
    cargarInscripciones();
});

metodoPagoSelect.addEventListener("change", actualizarMetodoPago);
montoInput.addEventListener("input", calcularVuelto);
montoRecibidoInput.addEventListener("input", calcularVuelto);

// REGISTRAR PAGO
formPago.addEventListener("submit", async event => {
    event.preventDefault();

    const inscripcionId = inscripcionSelect.value;
    const monto = montoInput.value;
    const metodoPago = metodoPagoSelect.value;
    const montoRecibido = montoRecibidoInput.value;

    const boton = formPago.querySelector('button[type="submit"]');

    if (!inscripcionId || !monto || !metodoPago) {
        mensajePago.className = "small text-danger";
        mensajePago.textContent =
            "Completa todos los campos obligatorios.";
        return;
    }

    const inscripcion = inscripcionesPendientes.find(
        item => String(item.id) === inscripcionId
    );

    if (!inscripcion) {
        mensajePago.className = "small text-danger";
        mensajePago.textContent =
            "Selecciona una inscripción pendiente válida.";
        return;
    }

    const precio = Number(inscripcion.membresia.precio);

    if (Number(monto) !== precio) {
        mensajePago.className = "small text-danger";
        mensajePago.textContent =
            `El monto de la membresía debe ser S/ ${precio.toFixed(2)}.`;
        return;
    }

    const parametros = new URLSearchParams({
        inscripcionId,
        monto,
        metodoPago
    });

    if (metodoPago === "EFECTIVO") {
        if (
            !montoRecibido ||
            !Number.isFinite(Number(montoRecibido)) ||
            Number(montoRecibido) < precio
        ) {
            mensajePago.className = "small text-danger";
            mensajePago.textContent =
                "El efectivo recibido es insuficiente.";
            return;
        }

        parametros.append("montoRecibido", montoRecibido);
    }

    boton.disabled = true;
    mensajePago.className = "small text-muted";
    mensajePago.textContent = "Registrando pago...";

    try {
        const respuesta = await fetch(
            `${API_PAGOS}?${parametros.toString()}`,
            { method: "POST" }
        );

        if (!respuesta.ok) {
            throw new Error(
                "No se pudo registrar el pago. Verifica que la inscripción siga pendiente."
            );
        }

        const pagoGuardado = await respuesta.json();

        bootstrap.Modal.getInstance(modalPago).hide();

        await cargarPagos();

        const vuelto = Number(pagoGuardado.vuelto ?? 0);

        alert(
            `Pago registrado correctamente.\n` +
            `Monto: S/ ${Number(pagoGuardado.monto).toFixed(2)}\n` +
            `Vuelto: S/ ${vuelto.toFixed(2)}`
        );

    } catch (error) {
        console.error(error);

        mensajePago.className = "small text-danger";
        mensajePago.textContent = error.message;

    } finally {
        boton.disabled = false;
    }
});

// ESTADO INICIAL
actualizarMetodoPago();