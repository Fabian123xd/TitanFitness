const API_ENTRENADORES = ENDPOINTS.ENTRENADORES;

let entrenadoresActuales = [];

const formulario = document.getElementById("formEntrenador");
const tabla = document.getElementById("tablaEntrenadores");
const mensajeRegistro = document.getElementById("mensajeRegistro");
const mensajeListado = document.getElementById("mensajeListado");

function mostrarMensaje(texto, esError = false) {
  mensajeRegistro.className = "small fw-semibold mt-3 " + (esError ? "text-danger" : "text-success");
  mensajeRegistro.textContent = texto;
}

async function cargarEntrenadores() {
  mensajeListado.textContent = "";

  try {
    const respuesta = await fetch(API_ENTRENADORES);
    if (!respuesta.ok) {
      throw new Error("Error al consultar entrenadores");
    }

    const datos = await respuesta.json();
    if (!Array.isArray(datos)) {
      throw new Error("Respuesta inesperada del servidor");
    }

    entrenadoresActuales = datos;

    const total = datos.length;
    const activos = datos.filter(e => e.activo === true).length;
    const inactivos = total - activos;

    const elTotal = document.getElementById("totalEntrenadores");
    const elActivos = document.getElementById("entrenadoresActivosCount");
    const elInactivos = document.getElementById("entrenadoresInactivosCount");

    if (elTotal) elTotal.textContent = total;
    if (elActivos) elActivos.textContent = activos;
    if (elInactivos) elInactivos.textContent = inactivos;

    renderizarTablaEntrenadores();
  } catch (error) {
    console.error("Error:", error);
    entrenadoresActuales = [];
    tabla.innerHTML = "";
    document.getElementById("totalEntrenadores").textContent = "—";
    mensajeListado.textContent = "No se pudieron cargar los entrenadores. Verifica que Spring Boot esté encendido.";
  }
}

function renderizarTablaEntrenadores() {
  const inputBusqueda = document.getElementById("buscarEntrenador");
  const filtroTexto = inputBusqueda ? inputBusqueda.value.toLowerCase().trim() : "";

  const lista = entrenadoresActuales.filter(e => {
    if (!filtroTexto) return true;
    const cadena = `${e.nombres || ""} ${e.apellidos || ""} ${e.especialidad || ""} ${e.correo || ""}`.toLowerCase();
    return cadena.includes(filtroTexto);
  });

  tabla.innerHTML = "";

  if (lista.length === 0) {
    tabla.innerHTML = `
      <tr>
        <td colspan="5" class="text-center text-muted py-4">
          No hay entrenadores que coincidan con la búsqueda.
        </td>
      </tr>
    `;
    return;
  }

  lista.forEach(entrenador => {
    const fila = document.createElement("tr");
    const nombreCompleto = `${entrenador.nombres ?? ""} ${entrenador.apellidos ?? ""}`.trim();
    const iniciales = typeof obtenerIniciales === "function" ? obtenerIniciales(nombreCompleto) : "EN";

    const tdNombre = document.createElement("td");
    tdNombre.innerHTML = `
      <div class="d-flex align-items-center gap-2">
        <span class="avatar-chip">${iniciales}</span>
        <div>
          <div class="fw-semibold" style="color: var(--navy-900);">${nombreCompleto}</div>
          <small class="text-muted">ID #${entrenador.id}</small>
        </div>
      </div>
    `;
    fila.appendChild(tdNombre);

    const tdEsp = document.createElement("td");
    tdEsp.innerHTML = `<span class="badge bg-primary">${entrenador.especialidad || "General"}</span>`;
    fila.appendChild(tdEsp);

    const tdContacto = document.createElement("td");
    tdContacto.innerHTML = `
      <div class="small fw-medium">${entrenador.correo || "Sin correo"}</div>
      <div class="small text-muted">${entrenador.telefono || "Sin teléfono"}</div>
    `;
    fila.appendChild(tdContacto);

    const tdEstado = document.createElement("td");
    tdEstado.innerHTML = entrenador.activo
      ? '<span class="badge bg-success">Activo</span>'
      : '<span class="badge bg-secondary">Inactivo</span>';
    fila.appendChild(tdEstado);

    const tdAcciones = document.createElement("td");
    tdAcciones.className = "text-center text-nowrap";

    const btnEditar = document.createElement("button");
    btnEditar.type = "button";
    btnEditar.className = "btn btn-sm btn-warning me-1";
    btnEditar.title = "Editar entrenador";
    btnEditar.innerHTML = '<i class="fa-solid fa-pen"></i>';
    btnEditar.addEventListener("click", () => editarEntrenador(entrenador.id));
    tdAcciones.appendChild(btnEditar);

    if (entrenador.activo === true) {
      const btnDesactivar = document.createElement("button");
      btnDesactivar.type = "button";
      btnDesactivar.className = "btn btn-sm btn-outline-danger";
      btnDesactivar.title = "Desactivar entrenador";
      btnDesactivar.innerHTML = '<i class="fa-solid fa-user-slash"></i>';
      btnDesactivar.addEventListener("click", () => desactivarEntrenador(entrenador.id));
      tdAcciones.appendChild(btnDesactivar);
    } else {
      const btnReactivar = document.createElement("button");
      btnReactivar.type = "button";
      btnReactivar.className = "btn btn-sm btn-outline-success";
      btnReactivar.title = "Reactivar entrenador";
      btnReactivar.innerHTML = '<i class="fa-solid fa-user-check"></i>';
      btnReactivar.addEventListener("click", () => reactivarEntrenador(entrenador.id));
      tdAcciones.appendChild(btnReactivar);
    }

    fila.appendChild(tdAcciones);
    tabla.appendChild(fila);
  });
}

formulario.addEventListener("submit", async function (event) {
  event.preventDefault();

  const id = document.getElementById("entrenadorId").value;

  const entrenador = {
    nombres: document.getElementById("nombres").value.trim(),
    apellidos: document.getElementById("apellidos").value.trim(),
    especialidad: document.getElementById("especialidad").value.trim(),
    telefono: document.getElementById("telefono").value.trim(),
    correo: document.getElementById("correo").value.trim() || null,
    activo: id ? document.getElementById("activo").value === "true" : true
  };

  if (!entrenador.nombres || !entrenador.apellidos) {
    mostrarMensaje("Los nombres y apellidos son obligatorios.", true);
    return;
  }

  const boton = document.getElementById("btnGuardar");
  boton.disabled = true;

  try {
    const url = id ? `${API_ENTRENADORES}/${id}` : API_ENTRENADORES;
    const respuesta = await fetch(url, {
      method: id ? "PUT" : "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(entrenador)
    });

    if (!respuesta.ok) {
      const msg = typeof extraerMensajeError === "function"
        ? await extraerMensajeError(respuesta, "No se pudo guardar el entrenador.")
        : "No se pudo guardar. Verifica que el correo no esté duplicado.";
      throw new Error(msg);
    }

    const mensaje = id
      ? "Entrenador actualizado correctamente."
      : "Entrenador registrado correctamente.";

    limpiarFormulario();
    mostrarMensaje(mensaje);
    await cargarEntrenadores();
  } catch (error) {
    console.error(error);
    mostrarMensaje(error.message, true);
  } finally {
    boton.disabled = false;
  }
});

function editarEntrenador(id) {
  const entrenador = entrenadoresActuales.find(e => e.id === id);
  if (!entrenador) {
    mostrarMensaje("No se encontró el entrenador.", true);
    return;
  }

  document.getElementById("entrenadorId").value = entrenador.id;
  document.getElementById("nombres").value = entrenador.nombres ?? "";
  document.getElementById("apellidos").value = entrenador.apellidos ?? "";
  document.getElementById("especialidad").value = entrenador.especialidad ?? "";
  document.getElementById("telefono").value = entrenador.telefono ?? "";
  document.getElementById("correo").value = entrenador.correo ?? "";
  document.getElementById("activo").value = String(entrenador.activo === true);

  document.getElementById("grupoEstado").hidden = false;
  document.getElementById("tituloFormulario").textContent = "Editar entrenador";
  document.getElementById("subtituloFormulario").textContent = "Actualiza los datos del entrenador seleccionado";
  document.getElementById("btnGuardar").innerHTML = '<i class="fa-solid fa-floppy-disk me-1"></i>Guardar cambios';
  document.getElementById("btnCancelar").hidden = false;

  mensajeRegistro.textContent = "";
  window.scrollTo({ top: 0, behavior: "smooth" });
  document.getElementById("nombres").focus({ preventScroll: true });
}

async function desactivarEntrenador(id) {
  const entrenador = entrenadoresActuales.find(e => e.id === id);
  if (!entrenador) return;

  const nombre = `${entrenador.nombres} ${entrenador.apellidos}`;
  if (!confirm(`¿Deseas desactivar al entrenador ${nombre}?`)) return;

  try {
    const respuesta = await fetch(`${API_ENTRENADORES}/${id}/desactivar`, { method: "PUT" });
    if (!respuesta.ok) {
      throw new Error("No se pudo desactivar al entrenador.");
    }

    limpiarFormulario();
    mostrarMensaje("Entrenador desactivado correctamente.");
    await cargarEntrenadores();
  } catch (error) {
    console.error(error);
    mostrarMensaje(error.message, true);
  }
}

async function reactivarEntrenador(id) {
  const entrenador = entrenadoresActuales.find(e => e.id === id);
  if (!entrenador) return;

  const nombre = `${entrenador.nombres} ${entrenador.apellidos}`;
  if (!confirm(`¿Deseas reactivar al entrenador ${nombre}?`)) return;

  try {
    const respuesta = await fetch(`${API_ENTRENADORES}/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        nombres: entrenador.nombres,
        apellidos: entrenador.apellidos,
        especialidad: entrenador.especialidad,
        telefono: entrenador.telefono,
        correo: entrenador.correo,
        activo: true
      })
    });
    if (!respuesta.ok) {
      throw new Error("No se pudo reactivar al entrenador.");
    }
    limpiarFormulario();
    mostrarMensaje("Entrenador reactivado correctamente.");
    await cargarEntrenadores();
  } catch (error) {
    console.error(error);
    mostrarMensaje(error.message, true);
  }
}

function limpiarFormulario() {
  formulario.reset();
  document.getElementById("entrenadorId").value = "";
  document.getElementById("grupoEstado").hidden = true;
  document.getElementById("tituloFormulario").textContent = "Registrar entrenador";
  document.getElementById("subtituloFormulario").textContent = "Completa los datos del entrenador";
  document.getElementById("btnGuardar").innerHTML = '<i class="fa-solid fa-floppy-disk me-1"></i>Registrar entrenador';
  document.getElementById("btnCancelar").hidden = true;
  mensajeRegistro.textContent = "";
}

document.getElementById("btnCancelar").addEventListener("click", limpiarFormulario);

const buscarEntrenador = document.getElementById("buscarEntrenador");
if (buscarEntrenador) {
  buscarEntrenador.addEventListener("input", renderizarTablaEntrenadores);
}

cargarEntrenadores();
