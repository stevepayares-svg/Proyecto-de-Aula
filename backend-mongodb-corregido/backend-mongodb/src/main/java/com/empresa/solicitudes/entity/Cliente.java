package com.empresa.solicitudes.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Colección "clientes". Un cliente puede tener muchas solicitudes, así que
 * NO se embeben aquí: cada solicitud guarda el id del cliente (referencia).
 */
@Document(collection = "clientes")
public class Cliente {

    @Id
    private String id;

    private String nombre;

    // Reemplaza el "unique = true" de @Column en JPA
    @Indexed(unique = true)
    private String email;

    private String telefono;

    private Date fechaRegistro;

    private String contrasena;

    private String rol = "CLIENTE";

    public Cliente() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public Date getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(Date fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}
