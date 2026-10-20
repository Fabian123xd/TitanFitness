
const API_ENTRENADORES =
  "http://localhost:8080/api/entrenadores";

let entrenadoresActuales = [];

const formulario = document.getElementById("formEntrenador");
const tabla = document.getElementById("tablaEntrenadores");
const mensajeRegistro = document.getElementById("mensajeRegistro");
const mensajeListado = document.getElementById("mensajeListado");

// MOSTRAR MENSAJES
function mostrarMensaje(texto, esError = false) {
  mensajeRegistro.className =
    "mensaje-formulario " +
    (esError ? "text-danger" : "text-success");

  mensajeRegistro.textContent = texto;
}

// CREAR CELDAS DE FORMA SEGURA
function crearCelda(fila, texto) {
  const celda = document.createElement("td");
  celda.textContent = texto ?? "";
  fila.appendChild(celda);
  return celda;
}

// LISTAR ENTRENADORES
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

    document.getElementById("totalEntrenadores")
      .textContent = datos.length;

    tabla.innerHTML = "";

    if (datos.length === 0) {
      const fila = document.createElement("tr");
      const celda = crearCelda(
        fila,
        "No hay entrenadores registrados."
      );

      celda.colSpan = 5;
      celda.className = "text-center text-muted py-4";
      tabla.appendChild(fila);
      return;
    }

    datos.forEach(entrenador => {
      const fila = document.createElement("tr");

      // NOMBRE COMPLETO
      const nombre = crearCelda(
        fila,
        `${entrenador.nombres ?? ""} ${entrenador.apellidos ?? ""}`.trim()
      );

      nombre.className = "nombre-entrenador";

      // ESPECIALIDAD
      crearCelda(
        fila,
        entrenador.especialidad || "Sin especificar"
      );

      // CONTACTO
      const contacto = document.createElement("td");

      const correo = document.createElement("div");
      correo.textContent = entrenador.correo || "Sin correo";

      const telefono = document.createElement("small");
      telefono.className = "text-muted";
      telefono.textContent =
        entrenador.telefono || "Sin teléfono";

      contacto.append(correo, telefono);
      fila.appendChild(contacto);

      // ESTADO
      const estadoCelda = document.createElement("td");
      const estado = document.createElement("span");

      estado.className =
        "estado " +
        (entrenador.activo ? "activo" : "inactivo");

      estado.textContent =
        entrenador.activo ? "Activo" : "Inactivo";

      estadoCelda.appendChild(estado);
      fila.appendChild(estadoCelda);

      // ACCIONES
      const accionesCelda = document.createElement("td");
      const acciones = document.createElement("div");
      acciones.className = "acciones";

      // BOTÓN EDITAR
      const btnEditar = document.createElement("button");
      btnEditar.type = "button";
      btnEditar.className = "btn btn-outline-primary";

      btnEditar.innerHTML =
        '<i class="fa-solid fa-pen me-1"></i>Editar';

      btnEditar.addEventListener("click", () => {
        editarEntrenador(entrenador.id);
      });

      acciones.appendChild(btnEditar);

      // BOTÓN DESACTIVAR
      if (entrenador.activo === true) {
        const btnDesactivar = document.createElement("button");

        btnDesactivar.type = "button";
        btnDesactivar.className = "btn btn-outline-danger";

        btnDesactivar.innerHTML =
          '<i class="fa-solid fa-user-slash me-1"></i>Desactivar';

        btnDesactivar.addEventListener("click", () => {
          desactivarEntrenador(entrenador.id);
        });

        acciones.appendChild(btnDesactivar);
      }

      accionesCelda.appendChild(acciones);
      fila.appendChild(accionesCelda);

      tabla.appendChild(fila);
    });

  } catch (error) {
    console.error("Error:", error);

    entrenadoresActuales = [];
    tabla.innerHTML = "";

    document.getElementById("totalEntrenadores")
      .textContent = "—";

    mensajeListado.textContent =
      "No se pudieron cargar los entrenadores. " +
      "Verifica que Spring Boot esté encendido.";
  }
}

// REGISTRAR O ACTUALIZAR ENTRENADOR
formulario.addEventListener("submit", async function(event) {
  event.preventDefault();

  const id = document.getElementById("entrenadorId").value;

  const entrenador = {
    nombres:
      document.getElementById("nombres").value.trim(),

    apellidos:
      document.getElementById("apellidos").value.trim(),

    especialidad:
      document.getElementById("especialidad").value.trim(),

    telefono:
      document.getElementById("telefono").value.trim(),

    correo:
      document.getElementById("correo").value.trim() || null,

    activo: id
      ? document.getElementById("activo").value === "true"
      : true
  };

  if (!entrenador.nombres || !entrenador.apellidos) {
    mostrarMensaje(
      "Los nombres y apellidos son obligatorios.",
      true
    );
    return;
  }

  const boton = document.getElementById("btnGuardar");
  boton.disabled = true;

  try {
    const url = id
      ? `${API_ENTRENADORES}/${id}`
      : API_ENTRENADORES;

    const respuesta = await fetch(url, {
      method: id ? "PUT" : "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(entrenador)
    });

    if (!respuesta.ok) {
      throw new Error(
        "No se pudo guardar. Revisa los datos " +
        "y verifica que el correo no esté duplicado."
      );
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

// EDITAR ENTRENADOR
function editarEntrenador(id) {
  const entrenador = entrenadoresActuales.find(
    e => e.id === id
  );

  if (!entrenador) {
    mostrarMensaje("No se encontró el entrenador.", true);
    return;
  }

  document.getElementById("entrenadorId").value =
    entrenador.id;

  document.getElementById("nombres").value =
    entrenador.nombres ?? "";

  document.getElementById("apellidos").value =
    entrenador.apellidos ?? "";

  document.getElementById("especialidad").value =
    entrenador.especialidad ?? "";

  document.getElementById("telefono").value =
    entrenador.telefono ?? "";

  document.getElementById("correo").value =
    entrenador.correo ?? "";

  document.getElementById("activo").value =
    String(entrenador.activo === true);

  document.getElementById("grupoEstado").hidden = false;

  document.getElementById("tituloFormulario").textContent =
    "Editar entrenador";

  document.getElementById("subtituloFormulario").textContent =
    "Actualiza los datos del entrenador seleccionado";

  document.getElementById("btnGuardar").innerHTML =
    '<i class="fa-solid fa-floppy-disk me-1"></i>Guardar cambios';

  document.getElementById("btnCancelar").hidden = false;

  mensajeRegistro.textContent = "";

  window.scrollTo({
    top: 0,
    behavior: "smooth"
  });

  document.getElementById("nombres").focus({
    preventScroll: true
  });
}

// DESACTIVAR ENTRENADOR
async function desactivarEntrenador(id) {
  const entrenador = entrenadoresActuales.find(
    e => e.id === id
  );

  if (!entrenador) return;

  const nombre =
    `${entrenador.nombres} ${entrenador.apellidos}`;

  const confirmar = confirm(
    `¿Deseas desactivar al entrenador ${nombre}?`
  );

  if (!confirmar) return;

  try {
    const respuesta = await fetch(
      `${API_ENTRENADORES}/${id}/desactivar`,
      {
        method: "PUT"
      }
    );

    if (!respuesta.ok) {
      throw new Error(
        "No se pudo desactivar al entrenador."
      );
    }

    limpiarFormulario();
    mostrarMensaje(
      "Entrenador desactivado correctamente."
    );

    await cargarEntrenadores();

  } catch (error) {
    console.error(error);
    mostrarMensaje(error.message, true);
  }
}

// LIMPIAR FORMULARIO
function limpiarFormulario() {
  formulario.reset();

  document.getElementById("entrenadorId").value = "";
  document.getElementById("grupoEstado").hidden = true;

  document.getElementById("tituloFormulario").textContent =
    "Registrar entrenador";

  document.getElementById("subtituloFormulario").textContent =
    "Completa los datos del entrenador";

  document.getElementById("btnGuardar").innerHTML =
    '<i class="fa-solid fa-floppy-disk me-1"></i>Registrar entrenador';

  document.getElementById("btnCancelar").hidden = true;

  mensajeRegistro.textContent = "";
}

// CANCELAR EDICIÓN
document.getElementById("btnCancelar")
  .addEventListener("click", limpiarFormulario);

// CARGAR DATOS AL ABRIR LA PÁGINA
cargarEntrenadores();