package com.empresa.solicitudes.dto;

/**
 * Respuesta de POST /api/auth/login.
 * El cliente (Postman, Angular...) guarda "token" y lo manda en
 * cada peticion como:  Authorization: Bearer <token>
 */
public record LoginResponse(
        String token,
        String tokenType,   // siempre "Bearer"
        long expiraEnMs,
        String tipo,        // "EMPLEADO" o "CLIENTE" (igual que antes)
        String id,
        String nombre,
        String email,
        java.util.List<String> roles) {}
