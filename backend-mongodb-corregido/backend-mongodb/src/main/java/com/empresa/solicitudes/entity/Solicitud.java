package com.empresa.solicitudes.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Colección "solicitudes": es el documento central del sistema.
 *
 *  - EMBEBIDO   : seguimientos (arreglo dentro del documento).
 *  - REFERENCIAS: clienteId, empleadoId, tipoSolicitudId y categoriaIds
 *                 (solo se guarda el id; los datos viven en su colección).
 *  - La tabla intermedia solicitud_categoria desaparece: la relación
 *    muchos a muchos se resuelve con el arreglo categoriaIds.
 */
@Document(collection = "solicitudes")
public class Solicitud {

    @Id
    private String id;

    private String titulo;

    private String descripcion;

    @JsonFormat(shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            timezone = "UTC")
    private Date fechaCreacion;

    private String prioridad;

    private String estado;

    private String sede;

    private String zonaGimnasio;

    private String maquinaAfectada;

    private String itemRelacionado;

    private String respuestaAdmin;

    @JsonFormat(shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            timezone = "UTC")
    private Date fechaRespuesta;

    // ───────────── REFERENCIAS (lo que realmente se guarda en Mongo) ─────────────
    private String clienteId;

    private String empleadoId;

    private String tipoSolicitudId;

    private List<String> categoriaIds = new ArrayList<>();

    // ───────────── EMBEBIDO ─────────────
    private List<Seguimiento> seguimientos = new ArrayList<>();

    // ───────────── OBJETOS COMPLETOS (NO se guardan) ─────────────
    // Mongo no hace JOIN. El servicio los llena al leer para que las páginas
    // (s.cliente.nombre, s.tipoSolicitud.nombre) y el JSON de la API sigan igual.
    @Transient
    private Cliente cliente;

    @Transient
    private Empleado empleado;

    @Transient
    private TipoSolicitud tipoSolicitud;

    @Transient
    private List<Categoria> categorias;

    public Solicitud() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Date getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Date fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getSede() { return sede; }
    public void setSede(String sede) { this.sede = sede; }

    public String getZonaGimnasio() { return zonaGimnasio; }
    public void setZonaGimnasio(String zonaGimnasio) { this.zonaGimnasio = zonaGimnasio; }

    public String getMaquinaAfectada() { return maquinaAfectada; }
    public void setMaquinaAfectada(String maquinaAfectada) { this.maquinaAfectada = maquinaAfectada; }

    public String getItemRelacionado() { return itemRelacionado; }
    public void setItemRelacionado(String itemRelacionado) { this.itemRelacionado = itemRelacionado; }

    public String getRespuestaAdmin() { return respuestaAdmin; }
    public void setRespuestaAdmin(String respuestaAdmin) { this.respuestaAdmin = respuestaAdmin; }

    public Date getFechaRespuesta() { return fechaRespuesta; }
    public void setFechaRespuesta(Date fechaRespuesta) { this.fechaRespuesta = fechaRespuesta; }

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    public String getEmpleadoId() { return empleadoId; }
    public void setEmpleadoId(String empleadoId) { this.empleadoId = empleadoId; }

    public String getTipoSolicitudId() { return tipoSolicitudId; }
    public void setTipoSolicitudId(String tipoSolicitudId) { this.tipoSolicitudId = tipoSolicitudId; }

    public List<String> getCategoriaIds() { return categoriaIds; }
    public void setCategoriaIds(List<String> categoriaIds) { this.categoriaIds = categoriaIds; }

    public List<Seguimiento> getSeguimientos() { return seguimientos; }
    public void setSeguimientos(List<Seguimiento> seguimientos) { this.seguimientos = seguimientos; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado empleado) { this.empleado = empleado; }

    public TipoSolicitud getTipoSolicitud() { return tipoSolicitud; }
    public void setTipoSolicitud(TipoSolicitud tipoSolicitud) { this.tipoSolicitud = tipoSolicitud; }

    public List<Categoria> getCategorias() { return categorias; }
    public void setCategorias(List<Categoria> categorias) { this.categorias = categorias; }
}
