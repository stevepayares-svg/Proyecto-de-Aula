package com.empresa.solicitudes.service;

import com.empresa.solicitudes.entity.Solicitud;

import java.util.List;
import java.util.Optional;

public interface ISolicitudService {

    Solicitud guardar(Solicitud solicitud);

    Solicitud actualizar(String id, Solicitud solicitud);

    void eliminar(String id);

    Optional<Solicitud> buscarPorId(String id);

    List<Solicitud> listarTodas();

    List<Solicitud> buscarPorEstado(String estado);

    List<Solicitud> buscarPorPrioridad(String prioridad);

    List<Solicitud> buscarPorSede(String sede);

    List<Solicitud> buscarPorCategoria(String categoriaId);

    List<Solicitud> buscarPorCliente(String clienteId);

    List<Solicitud> buscarPorTipo(String tipoSolicitudId);

    List<Solicitud> buscarPorEstadoYPrioridad(
            String estado,
            String prioridad
    );
}