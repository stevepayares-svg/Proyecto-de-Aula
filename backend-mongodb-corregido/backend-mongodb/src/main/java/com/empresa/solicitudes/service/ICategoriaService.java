package com.empresa.solicitudes.service;

import com.empresa.solicitudes.entity.Categoria;

import java.util.List;
import java.util.Optional;

public interface ICategoriaService {

    Categoria guardar(Categoria categoria);

    Categoria actualizar(String id, Categoria categoria);

    void eliminar(String id);

    Optional<Categoria> buscarPorId(String id);

    List<Categoria> listar();
}
