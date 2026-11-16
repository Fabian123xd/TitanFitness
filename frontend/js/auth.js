/**
 * Módulo centralizado de configuración API, autenticación, sesión, manejo de errores
 * y utilidades compartidas para Titan Fitness Club — Edición Titan Cobalt Pro.
 */
const API_BASE = "http://localhost:8080/api";

const ENDPOINTS = Object.freeze({
    AUTH_ME: `${API_BASE}/auth/me`,
    REPORTES_RESUMEN: `${API_BASE}/reportes/resumen`,
    CLIENTES: `${API_BASE}/clientes`,
    MEMBRESIAS: `${API_BASE}/membresias`,
    INSCRIPCIONES: `${API_BASE}/inscripciones`,
    ENTRENADORES: `${API_BASE}/entrenadores`,
    RUTINAS: `${API_BASE}/rutinas`
});

(function interceptarFetch() {
    const originalFetch = window.fetch.bind(window);
    window.fetch = async function (input, init = {}) {
        const url = typeof input === "string" ? input : (input && input.url) || "";
        if (url.startsWith(API_BASE) || url.includes("localhost:8080/api")) {
            const headers = new Headers(init.headers || {});
            const token = sessionStorage.getItem("authToken");
            if (token && !headers.has("Authorization")) {
                headers.set("Authorization", `Basic ${token}`);
            }
            init = { ...init, headers };
        }
        const response = await originalFetch(input, init);
        if (response.status === 401 && !window.location.pathname.endsWith("login.html")) {
            sessionStorage.clear();
            window.location.href = "login.html";
        }
        return response;
    };
})();

function cerrarSesion() {
    sessionStorage.clear();
    window.location.href = "login.html";
}

function obtenerIniciales(nombreCompleto) {
    if (!nombreCompleto) return "TF";
    const partes = String(nombreCompleto).trim().split(/\s+/);
    const i1 = partes[0] ? partes[0][0] : "";
    const i2 = partes[1] ? partes[1][0] : "";
    return (i1 + i2).toUpperCase() || "TF";
}

function obtenerNombreCompleto(persona) {
    if (!persona) return "No disponible";
    const nombres = persona.nombres || persona.nombre || "";
    const apellidos = persona.apellidos || persona.apellido || "";
    return `${nombres} ${apellidos}`.trim() || `ID ${persona.id}`;
}

function formatearFechaLocal(d = new Date()) {
    const anio = d.getFullYear();
    const mes = String(d.getMonth() + 1).padStart(2, "0");
    const dia = String(d.getDate()).padStart(2, "0");
    return `${anio}-${mes}-${dia}`;
}

function asignarTexto(id, valor) {
    const el = document.getElementById(id);
    if (el) el.textContent = valor;
}

function mostrarAlertaUI(idElemento, mensaje, tipo = "success", duracionMs = 4500) {
    const alerta = document.getElementById(idElemento);
    if (!alerta) return;
    alerta.className = `alert alert-${tipo}`;
    alerta.textContent = mensaje;
    if (duracionMs > 0) {
        setTimeout(() => alerta.classList.add("d-none"), duracionMs);
    }
}

function badgeEstadoInscripcion(estado, proximaAVencer = false) {
    if (estado === "ACTIVA") {
        if (proximaAVencer) {
            return '<span class="badge bg-warning text-dark">ACTIVA · Próxima a vencer</span>';
        }
        return '<span class="badge bg-success">ACTIVA</span>';
    }
    if (estado === "PENDIENTE") {
        return '<span class="badge bg-primary">PENDIENTE</span>';
    }
    if (estado === "VENCIDA") {
        return '<span class="badge bg-danger">VENCIDA</span>';
    }
    if (estado === "CANCELADA") {
        return '<span class="badge bg-secondary">CANCELADA</span>';
    }
    return `<span class="badge bg-secondary">${estado || "-"}</span>`;
}

async function extraerMensajeError(respuesta, mensajePorDefecto = "Ocurrió un error en la operación") {
    try {
        const data = await respuesta.clone().json();
        if (data && data.mensaje) return data.mensaje;
        if (data && data.message) return data.message;
    } catch (_) {
        try {
            const texto = await respuesta.text();
            if (texto) return texto;
        } catch (_) {}
    }
    return mensajePorDefecto;
}

document.addEventListener("DOMContentLoaded", () => {
    const usuario = sessionStorage.getItem("usuario") || "admin";

    document.querySelectorAll("#nombreUsuarioTop, .nombre-usuario-activo").forEach(el => {
        el.textContent = usuario;
    });

    const sidebar = document.querySelector(".sidebar");
    if (sidebar && !sidebar.querySelector(".btn-logout-sidebar")) {
        const contenedor = document.createElement("div");
        contenedor.className = "sidebar-user-box";
        contenedor.innerHTML = `
            <div class="d-flex align-items-center justify-content-between text-light small mb-2 px-1 user-meta">
                <span class="fw-semibold text-white">
                    <i class="fa-solid fa-user-shield me-2 text-info"></i>${usuario}
                </span>
                <span class="badge bg-primary">ADMIN</span>
            </div>
            <button type="button" class="btn btn-outline-light btn-sm w-100 btn-logout-sidebar" onclick="cerrarSesion()">
                <i class="fa-solid fa-right-from-bracket me-2"></i><span>Cerrar sesión</span>
            </button>
        `;
        sidebar.appendChild(contenedor);
    }
});
