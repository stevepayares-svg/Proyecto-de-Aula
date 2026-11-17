package com.empresa.solicitudes.service;

import com.empresa.solicitudes.entity.Seguimiento;

import java.util.List;
import java.util.Optional;

public interface ISeguimientoService {
    Seguimiento guardar(Seguimiento seguimiento);
    Seguimiento actualizar(String id, Seguimiento seguimiento);
    void eliminar(String id);
    Optional<Seguimiento> buscarPorId(String id);
    List<Seguimiento> listarPorSolicitud(String solicitudId);
    List<Seguimiento> listarPorSolicitudOrdenado(String solicitudId);
}
