package com.empresa.solicitudes.service;

import com.empresa.solicitudes.entity.Cliente;

import java.util.List;
import java.util.Optional;

public interface IClienteService {
    Cliente guardar(Cliente cliente);
    Cliente actualizar(String id, Cliente cliente);
    void eliminar(String id);
    Optional<Cliente> buscarPorId(String id);
    Optional<Cliente> buscarPorEmail(String email);
    List<Cliente> listarTodos();
    boolean existePorEmail(String email);
}
