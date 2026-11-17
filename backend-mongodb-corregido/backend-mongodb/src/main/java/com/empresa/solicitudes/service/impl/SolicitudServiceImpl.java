package com.empresa.solicitudes.service.impl;

import com.empresa.solicitudes.entity.Cliente;
import com.empresa.solicitudes.entity.Categoria;
import com.empresa.solicitudes.entity.Empleado;
import com.empresa.solicitudes.entity.Solicitud;
import com.empresa.solicitudes.entity.TipoSolicitud;
import com.empresa.solicitudes.repository.CategoriaRepository;
import com.empresa.solicitudes.repository.ClienteRepository;
import com.empresa.solicitudes.repository.EmpleadoRepository;
import com.empresa.solicitudes.repository.SolicitudRepository;
import com.empresa.solicitudes.repository.TipoSolicitudRepository;
import com.empresa.solicitudes.service.ISolicitudService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class SolicitudServiceImpl implements ISolicitudService {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private TipoSolicitudRepository tipoSolicitudRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Override
    public Solicitud guardar(Solicitud solicitud) {

        // MongoDB no tiene llaves foráneas: aquí se valida a mano que lo referenciado exista.
        String clienteId = (solicitud.getCliente() != null && solicitud.getCliente().getId() != null)
                ? solicitud.getCliente().getId()
                : solicitud.getClienteId();
        if (clienteId == null) {
            throw new RuntimeException("Cliente no encontrado");
        }
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        String tipoId = (solicitud.getTipoSolicitud() != null && solicitud.getTipoSolicitud().getId() != null)
                ? solicitud.getTipoSolicitud().getId()
                : solicitud.getTipoSolicitudId();
        if (tipoId == null) {
            throw new RuntimeException("TipoSolicitud no encontrado");
        }
        TipoSolicitud tipo = tipoSolicitudRepository.findById(tipoId)
                .orElseThrow(() -> new RuntimeException("TipoSolicitud no encontrado"));

        String empleadoId = (solicitud.getEmpleado() != null && solicitud.getEmpleado().getId() != null)
                ? solicitud.getEmpleado().getId()
                : solicitud.getEmpleadoId();
        Empleado empleado;
        if (empleadoId == null) {
            // Si no viene empleado asignado, se asigna el primer administrador
            // (antes se usaba el empleado con id = 1, que en Mongo no existe).
            empleado = empleadoRepository.findByCargo("Administrador").stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("No hay empleados registrados en el sistema"));
        } else {
            empleado = empleadoRepository.findById(empleadoId)
                    .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));
        }

        // Categorías: pueden llegar como ids (categoriaIds) o como objetos (categorias)
        List<String> categoriaIds = solicitud.getCategoriaIds();
        if ((categoriaIds == null || categoriaIds.isEmpty()) && solicitud.getCategorias() != null) {
            categoriaIds = solicitud.getCategorias().stream().map(Categoria::getId).toList();
        }
        if (categoriaIds == null) {
            categoriaIds = new ArrayList<>();
        }
        categoriaIds = new ArrayList<>(categoriaIds.stream().distinct().toList());
        if (categoriaRepository.findAllById(categoriaIds).size() != categoriaIds.size()) {
            throw new RuntimeException("Categoria no encontrada");
        }

        // Lo que se guarda son solo los ids (referencias)
        solicitud.setClienteId(cliente.getId());
        solicitud.setTipoSolicitudId(tipo.getId());
        solicitud.setEmpleadoId(empleado.getId());
        solicitud.setCategoriaIds(categoriaIds);

        if (solicitud.getFechaCreacion() == null) {
            solicitud.setFechaCreacion(new Date());
        }

        if (solicitud.getEstado() == null) {
            solicitud.setEstado("ABIERTA");
        }

        return poblar(solicitudRepository.save(solicitud));
    }

    @Override
    public Solicitud actualizar(String id, Solicitud solicitudActualizada) {

        Solicitud solicitudExistente =
                solicitudRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Solicitud no encontrada"));

        solicitudExistente.setTitulo(solicitudActualizada.getTitulo());
        solicitudExistente.setDescripcion(solicitudActualizada.getDescripcion());
        solicitudExistente.setPrioridad(solicitudActualizada.getPrioridad());
        solicitudExistente.setEstado(solicitudActualizada.getEstado());
        solicitudExistente.setSede(solicitudActualizada.getSede());
        solicitudExistente.setZonaGimnasio(solicitudActualizada.getZonaGimnasio());
        solicitudExistente.setMaquinaAfectada(solicitudActualizada.getMaquinaAfectada());

        String tipoId = (solicitudActualizada.getTipoSolicitud() != null)
                ? solicitudActualizada.getTipoSolicitud().getId()
                : solicitudActualizada.getTipoSolicitudId();
        if (tipoId != null) {
            TipoSolicitud tipo = tipoSolicitudRepository.findById(tipoId)
                    .orElseThrow(() ->
                            new RuntimeException("TipoSolicitud no encontrado"));

            solicitudExistente.setTipoSolicitudId(tipo.getId());
        }

        return poblar(solicitudRepository.save(solicitudExistente));
    }

    @Override
    public void eliminar(String id) {

        if (!solicitudRepository.existsById(id)) {
            throw new RuntimeException("Solicitud no encontrada");
        }

        // Los seguimientos van embebidos: se borran junto con el documento.
        solicitudRepository.deleteById(id);
    }

    @Override
    public Optional<Solicitud> buscarPorId(String id) {
        return solicitudRepository.findById(id).map(this::poblar);
    }

    @Override
    public List<Solicitud> listarTodas() {
        return poblarLista(solicitudRepository.findAll());
    }

    @Override
    public List<Solicitud> buscarPorEstado(String estado) {
        return poblarLista(solicitudRepository.findByEstado(estado));
    }

    @Override
    public List<Solicitud> buscarPorPrioridad(String prioridad) {
        return poblarLista(solicitudRepository.findByPrioridad(prioridad));
    }

    @Override
    public List<Solicitud> buscarPorCategoria(String categoriaId) {
        return poblarLista(solicitudRepository.findByCategoriaIdsContaining(categoriaId));
    }

    @Override
    public List<Solicitud> buscarPorSede(String sede) {
        return poblarLista(solicitudRepository.findBySede(sede));
    }

    @Override
    public List<Solicitud> buscarPorCliente(String clienteId) {
        return poblarLista(solicitudRepository.findByClienteId(clienteId));
    }

    @Override
    public List<Solicitud> buscarPorTipo(String tipoSolicitudId) {
        return poblarLista(solicitudRepository.findByTipoSolicitudId(tipoSolicitudId));
    }

    @Override
    public List<Solicitud> buscarPorEstadoYPrioridad(
            String estado,
            String prioridad) {

        return poblarLista(solicitudRepository.findByEstadoAndPrioridad(
                estado,
                prioridad
        ));
    }

    // ─────────────────────────────────────────────────────────────
    // "JOIN" manual: Mongo solo guarda los ids, así que al leer se
    // buscan los documentos referenciados y se dejan en los campos
    // @Transient para que las páginas y el JSON sigan igual.
    // (Hace una consulta extra por referencia; en un proyecto grande
    // se usaría una agregación con $lookup.)
    // ─────────────────────────────────────────────────────────────
    private Solicitud poblar(Solicitud s) {
        if (s.getClienteId() != null) {
            s.setCliente(clienteRepository.findById(s.getClienteId()).orElse(null));
        }
        if (s.getEmpleadoId() != null) {
            s.setEmpleado(empleadoRepository.findById(s.getEmpleadoId()).orElse(null));
        }
        if (s.getTipoSolicitudId() != null) {
            s.setTipoSolicitud(tipoSolicitudRepository.findById(s.getTipoSolicitudId()).orElse(null));
        }
        if (s.getCategoriaIds() != null && !s.getCategoriaIds().isEmpty()) {
            s.setCategorias(categoriaRepository.findAllById(s.getCategoriaIds()));
        } else {
            s.setCategorias(new ArrayList<>());
        }
        return s;
    }

    private List<Solicitud> poblarLista(List<Solicitud> solicitudes) {
        for (Solicitud s : solicitudes) {
            poblar(s);
        }
        return solicitudes;
    }
}
