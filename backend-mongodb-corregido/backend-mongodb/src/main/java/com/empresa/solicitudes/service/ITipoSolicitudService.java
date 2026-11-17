package com.empresa.solicitudes.service;

import com.empresa.solicitudes.entity.TipoSolicitud;

import java.util.List;
import java.util.Optional;

public interface ITipoSolicitudService {
    TipoSolicitud guardar(TipoSolicitud tipoSolicitud);
    TipoSolicitud actualizar(String id, TipoSolicitud tipoSolicitud);
    void eliminar(String id);
    Optional<TipoSolicitud> buscarPorId(String id);
    Optional<TipoSolicitud> buscarPorNombre(String nombre);
    List<TipoSolicitud> listarTodos();
    boolean existePorNombre(String nombre);
}
