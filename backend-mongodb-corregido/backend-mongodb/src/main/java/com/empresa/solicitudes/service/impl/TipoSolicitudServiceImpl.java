package com.empresa.solicitudes.service.impl;

import com.empresa.solicitudes.entity.TipoSolicitud;
import com.empresa.solicitudes.repository.SolicitudRepository;
import com.empresa.solicitudes.repository.TipoSolicitudRepository;
import com.empresa.solicitudes.service.ITipoSolicitudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TipoSolicitudServiceImpl implements ITipoSolicitudService {

    @Autowired
    private TipoSolicitudRepository tipoSolicitudRepository;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Override
    public TipoSolicitud guardar(TipoSolicitud tipoSolicitud) {
        if (tipoSolicitudRepository.existsByNombre(tipoSolicitud.getNombre())) {
            throw new RuntimeException("Ya existe un tipo de solicitud con el nombre: " + tipoSolicitud.getNombre());
        }
        return tipoSolicitudRepository.save(tipoSolicitud);
    }

    @Override
    public TipoSolicitud actualizar(String id, TipoSolicitud tipoActualizado) {
        TipoSolicitud tipoExistente = tipoSolicitudRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TipoSolicitud no encontrado con id: " + id));

        tipoExistente.setNombre(tipoActualizado.getNombre());
        tipoExistente.setDescripcion(tipoActualizado.getDescripcion());

        return tipoSolicitudRepository.save(tipoExistente);
    }

    @Override
    public void eliminar(String id) {
        if (!tipoSolicitudRepository.existsById(id)) {
            throw new RuntimeException("TipoSolicitud no encontrado con id: " + id);
        }
        // Antes se borraban en cascada todas las solicitudes del tipo; ahora se
        // bloquea el borrado para no perder solicitudes sin querer.
        if (!solicitudRepository.findByTipoSolicitudId(id).isEmpty()) {
            throw new RuntimeException("No se puede eliminar: hay solicitudes de este tipo");
        }
        tipoSolicitudRepository.deleteById(id);
    }

    @Override
    public Optional<TipoSolicitud> buscarPorId(String id) {
        return tipoSolicitudRepository.findById(id);
    }

    @Override
    public Optional<TipoSolicitud> buscarPorNombre(String nombre) {
        return tipoSolicitudRepository.findByNombre(nombre);
    }

    @Override
    public List<TipoSolicitud> listarTodos() {
        return tipoSolicitudRepository.findAll();
    }

    @Override
    public boolean existePorNombre(String nombre) {
        return tipoSolicitudRepository.existsByNombre(nombre);
    }
}
