package com.empresa.solicitudes.repository;

import com.empresa.solicitudes.entity.Solicitud;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudRepository extends MongoRepository<Solicitud, String> {

    List<Solicitud> findByEstado(String estado);
    List<Solicitud> findByPrioridad(String prioridad);
    List<Solicitud> findBySede(String sede);
    List<Solicitud> findByClienteId(String clienteId);
    List<Solicitud> findByTipoSolicitudId(String tipoSolicitudId);
    List<Solicitud> findByCategoriaIdsContaining(String categoriaId);

    // Antes usaba @Query con JPQL; en Mongo basta con el nombre del método.
    List<Solicitud> findByEstadoAndPrioridad(String estado, String prioridad);

    // Reemplazan las restricciones de llave foránea que antes hacía MySQL
    boolean existsByEmpleadoId(String empleadoId);

    // Busca la solicitud que contiene un seguimiento (campo embebido seguimientos.codigo)
    Optional<Solicitud> findBySeguimientosCodigo(String codigo);
}
