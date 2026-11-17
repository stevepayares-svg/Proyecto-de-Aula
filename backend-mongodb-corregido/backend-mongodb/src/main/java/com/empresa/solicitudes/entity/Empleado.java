package com.empresa.solicitudes.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Colección "empleados".
 */
@Document(collection = "empleados")
public class Empleado {

    @Id
    private String id;

    private String nombre;

    @Indexed(unique = true)
    private String email;

    private String cargo;

    private Date fechaContratacion;

    private String contrasena;

    // Categoría de solicitudes que atiende este empleado. Solo ve y gestiona
    // las solicitudes que tengan esta categoría. El administrador no usa este
    // campo porque ve todas.
    private String categoriaId;

    public Empleado() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public Date getFechaContratacion() { return fechaContratacion; }
    public void setFechaContratacion(Date fechaContratacion) { this.fechaContratacion = fechaContratacion; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }

    public String getCategoriaId() { return categoriaId; }
    public void setCategoriaId(String categoriaId) { this.categoriaId = categoriaId; }
}
