package com.empresa.solicitudes.repository;

import com.empresa.solicitudes.entity.TipoSolicitud;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoSolicitudRepository extends MongoRepository<TipoSolicitud, String> {
    Optional<TipoSolicitud> findByNombre(String nombre);
    boolean existsByNombre(String nombre);
}
