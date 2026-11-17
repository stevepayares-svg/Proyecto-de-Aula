package com.empresa.solicitudes.repository;

import com.empresa.solicitudes.entity.Categoria;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CategoriaRepository extends MongoRepository<Categoria, String> {

}
