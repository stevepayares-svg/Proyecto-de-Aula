package com.empresa.solicitudes.service.impl;

import com.empresa.solicitudes.entity.Categoria;
import com.empresa.solicitudes.repository.CategoriaRepository;
import com.empresa.solicitudes.repository.EmpleadoRepository;
import com.empresa.solicitudes.repository.SolicitudRepository;
import com.empresa.solicitudes.service.ICategoriaService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaServiceImpl implements ICategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Override
    public Categoria guardar(Categoria categoria) {
        return categoriaRepository.save(categoria);
    }

    @Override
    public Categoria actualizar(String id, Categoria categoriaActualizada) {
        Categoria existente = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + id));
        existente.setNombre(categoriaActualizada.getNombre());
        existente.setDescripcion(categoriaActualizada.getDescripcion());
        return categoriaRepository.save(existente);
    }

    @Override
    public void eliminar(String id) {
        if (!categoriaRepository.existsById(id)) {
            throw new RuntimeException("Categoría no encontrada con id: " + id);
        }
        if (!solicitudRepository.findByCategoriaIdsContaining(id).isEmpty()) {
            throw new RuntimeException("No se puede eliminar: hay solicitudes que usan esta categoría");
        }
        if (empleadoRepository.existsByCategoriaId(id)) {
            throw new RuntimeException("No se puede eliminar: hay empleados atendiendo esta categoría");
        }
        categoriaRepository.deleteById(id);
    }

    @Override
    public Optional<Categoria> buscarPorId(String id) {
        return categoriaRepository.findById(id);
    }

    @Override
    public List<Categoria> listar() {
        return categoriaRepository.findAll();
    }
}
