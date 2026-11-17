package com.empresa.solicitudes.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Colección "tipos_solicitud": catálogo de tipos de PQRSF.
 * Ya no guarda la lista de solicitudes; la relación vive del lado de la solicitud.
 */
@Document(collection = "tipos_solicitud")
public class TipoSolicitud {

    @Id
    private String id;

    private String nombre;

    private String descripcion;

    public TipoSolicitud() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
