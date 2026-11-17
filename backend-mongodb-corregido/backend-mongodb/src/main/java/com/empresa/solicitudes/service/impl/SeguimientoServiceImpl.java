package com.empresa.solicitudes.service.impl;

import com.empresa.solicitudes.entity.Seguimiento;
import com.empresa.solicitudes.entity.Solicitud;
import com.empresa.solicitudes.repository.SolicitudRepository;
import com.empresa.solicitudes.service.ISeguimientoService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Los seguimientos ya NO tienen colección ni repositorio propios:
 * viven embebidos dentro del documento de la solicitud.
 * Todas las operaciones se hacen cargando la solicitud, modificando su
 * arreglo "seguimientos" y guardándola de nuevo.
 */
@Service
public class SeguimientoServiceImpl implements ISeguimientoService {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Override
    public Seguimiento guardar(Seguimiento seguimiento) {
        String solicitudId = seguimiento.getSolicitudId();
        if (solicitudId == null) {
            throw new RuntimeException("Debe indicar el solicitudId del seguimiento");
        }

        // Validar que exista la solicitud
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + solicitudId));

        // Cada seguimiento embebido lleva su propio identificador
        seguimiento.setCodigo(new ObjectId().toHexString());

        if (seguimiento.getFecha() == null) {
            seguimiento.setFecha(new Date());
        }

        if (solicitud.getSeguimientos() == null) {
            solicitud.setSeguimientos(new ArrayList<>());
        }
        solicitud.getSeguimientos().add(seguimiento);
        solicitudRepository.save(solicitud);

        seguimiento.setSolicitudId(solicitudId);
        return seguimiento;
    }

    @Override
    public Seguimiento actualizar(String id, Seguimiento seguimientoActualizado) {
        Solicitud solicitud = solicitudRepository.findBySeguimientosCodigo(id)
                .orElseThrow(() -> new RuntimeException("Seguimiento no encontrado con id: " + id));

        Seguimiento existente = solicitud.getSeguimientos().stream()
                .filter(s -> id.equals(s.getCodigo()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Seguimiento no encontrado con id: " + id));

        existente.setComentario(seguimientoActualizado.getComentario());
        if (seguimientoActualizado.getFecha() != null) {
            existente.setFecha(seguimientoActualizado.getFecha());
        }

        solicitudRepository.save(solicitud);

        existente.setSolicitudId(solicitud.getId());
        return existente;
    }

    @Override
    public void eliminar(String id) {
        Solicitud solicitud = solicitudRepository.findBySeguimientosCodigo(id)
                .orElseThrow(() -> new RuntimeException("Seguimiento no encontrado con id: " + id));

        solicitud.getSeguimientos().removeIf(s -> id.equals(s.getCodigo()));
        solicitudRepository.save(solicitud);
    }

    @Override
    public Optional<Seguimiento> buscarPorId(String id) {
        return solicitudRepository.findBySeguimientosCodigo(id)
                .flatMap(solicitud -> solicitud.getSeguimientos().stream()
                        .filter(s -> id.equals(s.getCodigo()))
                        .findFirst()
                        .map(s -> {
                            s.setSolicitudId(solicitud.getId());
                            return s;
                        }));
    }

    @Override
    public List<Seguimiento> listarPorSolicitud(String solicitudId) {
        return solicitudRepository.findById(solicitudId)
                .map(solicitud -> {
                    List<Seguimiento> lista = new ArrayList<>();
                    if (solicitud.getSeguimientos() != null) {
                        lista.addAll(solicitud.getSeguimientos());
                    }
                    lista.forEach(s -> s.setSolicitudId(solicitud.getId()));
                    return lista;
                })
                .orElseGet(ArrayList::new);
    }

    @Override
    public List<Seguimiento> listarPorSolicitudOrdenado(String solicitudId) {
        List<Seguimiento> lista = listarPorSolicitud(solicitudId);
        lista.sort(Comparator.comparing(Seguimiento::getFecha,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return lista;
    }
}
