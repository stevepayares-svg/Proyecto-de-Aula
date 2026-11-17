package com.empresa.solicitudes.service;

import com.empresa.solicitudes.entity.Empleado;

import java.util.List;
import java.util.Optional;

public interface IEmpleadoService {
    Empleado guardar(Empleado empleado);
    Empleado actualizar(String id, Empleado empleado);
    void eliminar(String id);
    Optional<Empleado> buscarPorId(String id);
    Optional<Empleado> buscarPorEmail(String email);
    List<Empleado> listarTodos();
    List<Empleado> buscarPorCargo(String cargo);
    boolean existePorEmail(String email);
}
