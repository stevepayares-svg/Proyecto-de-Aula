package com.empresa.solicitudes.service.impl;

import com.empresa.solicitudes.entity.Empleado;
import com.empresa.solicitudes.repository.EmpleadoRepository;
import com.empresa.solicitudes.repository.SolicitudRepository;
import com.empresa.solicitudes.service.IEmpleadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class EmpleadoServiceImpl implements IEmpleadoService {

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Empleado guardar(Empleado empleado) {
        if (empleadoRepository.existsByEmail(empleado.getEmail())) {
            throw new RuntimeException("Ya existe un empleado con el email: " + empleado.getEmail());
        }
        if (empleado.getFechaContratacion() == null) {
            empleado.setFechaContratacion(new Date());
        }
        if (empleado.getContrasena() != null) {
            empleado.setContrasena(passwordEncoder.encode(empleado.getContrasena()));
        }
        return empleadoRepository.save(empleado);
    }

    @Override
    public Empleado actualizar(String id, Empleado empleadoActualizado) {
        Empleado empleadoExistente = empleadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado con id: " + id));

        empleadoExistente.setNombre(empleadoActualizado.getNombre());
        empleadoExistente.setEmail(empleadoActualizado.getEmail());
        empleadoExistente.setCargo(empleadoActualizado.getCargo());
        empleadoExistente.setCategoriaId(empleadoActualizado.getCategoriaId());
        empleadoExistente.setFechaContratacion(empleadoActualizado.getFechaContratacion());

        return empleadoRepository.save(empleadoExistente);
    }

    @Override
    public void eliminar(String id) {
        if (!empleadoRepository.existsById(id)) {
            throw new RuntimeException("Empleado no encontrado con id: " + id);
        }
        // MySQL lo impedía con la llave foránea; en Mongo hay que validarlo a mano.
        if (solicitudRepository.existsByEmpleadoId(id)) {
            throw new RuntimeException("No se puede eliminar: el empleado tiene solicitudes asignadas");
        }
        empleadoRepository.deleteById(id);
    }

    @Override
    public Optional<Empleado> buscarPorId(String id) {
        return empleadoRepository.findById(id);
    }

    @Override
    public Optional<Empleado> buscarPorEmail(String email) {
        return empleadoRepository.findByEmail(email);
    }

    @Override
    public List<Empleado> listarTodos() {
        return empleadoRepository.findAll();
    }

    @Override
    public List<Empleado> buscarPorCargo(String cargo) {
        return empleadoRepository.findByCargo(cargo);
    }

    @Override
    public boolean existePorEmail(String email) {
        return empleadoRepository.existsByEmail(email);
    }
}
