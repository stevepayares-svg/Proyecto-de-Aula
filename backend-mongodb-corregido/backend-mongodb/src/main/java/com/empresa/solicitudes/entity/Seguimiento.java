package com.empresa.solicitudes.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Transient;

import java.util.Date;

/**
 * Seguimiento EMBEBIDO dentro de la solicitud (ya no es una colección aparte).
 *
 * Regla: una solicitud tiene pocos seguimientos y siempre se consultan junto
 * con ella, así que van dentro del mismo documento como un arreglo.
 *
 * Esta clase NO lleva @Document porque no tiene colección propia.
 */
public class Seguimiento {

    // Identificador propio del seguimiento dentro del arreglo. Se guarda como
    // "codigo" en Mongo (para no confundirlo con el _id) y se expone como "id"
    // en el JSON para que la API siga viéndose igual.
    @JsonProperty("id")
    private String codigo;

    private String comentario;

    private Date fecha;

    // Referencia al empleado que hizo el seguimiento (solo su id)
    private String empleadoId;

    // No se guarda: como el seguimiento vive dentro de la solicitud, ya sabemos
    // a cuál pertenece. Solo se llena para responder en la API y recibirlo en el POST.
    @Transient
    private String solicitudId;

    public Seguimiento() {}

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }

    public String getEmpleadoId() { return empleadoId; }
    public void setEmpleadoId(String empleadoId) { this.empleadoId = empleadoId; }

    public String getSolicitudId() { return solicitudId; }
    public void setSolicitudId(String solicitudId) { this.solicitudId = solicitudId; }
}
