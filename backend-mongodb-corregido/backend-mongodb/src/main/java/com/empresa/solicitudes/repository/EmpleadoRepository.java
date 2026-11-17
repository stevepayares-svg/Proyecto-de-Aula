package com.empresa.solicitudes.repository;

import com.empresa.solicitudes.entity.Empleado;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpleadoRepository extends MongoRepository<Empleado, String> {
    Optional<Empleado> findByEmail(String email);
    List<Empleado> findByCargo(String cargo);
    boolean existsByEmail(String email);
    boolean existsByCategoriaId(String categoriaId);
}
