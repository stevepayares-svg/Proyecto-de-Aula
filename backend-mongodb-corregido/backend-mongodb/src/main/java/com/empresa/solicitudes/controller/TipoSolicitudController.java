package com.empresa.solicitudes.controller;

import com.empresa.solicitudes.entity.TipoSolicitud;
import com.empresa.solicitudes.service.ITipoSolicitudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-solicitud")
public class TipoSolicitudController {

    @Autowired
    private ITipoSolicitudService tipoSolicitudService;

    // GET /api/tipos-solicitud
    @GetMapping
    public ResponseEntity<List<TipoSolicitud>> listarTodos() {
        return ResponseEntity.ok(tipoSolicitudService.listarTodos());
    }

    // GET /api/tipos-solicitud/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable String id) {
        return tipoSolicitudService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/tipos-solicitud/nombre/{nombre}
    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<?> buscarPorNombre(@PathVariable String nombre) {
        return tipoSolicitudService.buscarPorNombre(nombre)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/tipos-solicitud
    @PostMapping
    public ResponseEntity<?> guardar(@RequestBody TipoSolicitud tipoSolicitud) {
        try {
            TipoSolicitud nuevo = tipoSolicitudService.guardar(tipoSolicitud);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PUT /api/tipos-solicitud/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable String id, @RequestBody TipoSolicitud tipoSolicitud) {
        try {
            TipoSolicitud actualizado = tipoSolicitudService.actualizar(id, tipoSolicitud);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE /api/tipos-solicitud/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable String id) {
        try {
            tipoSolicitudService.eliminar(id);
            return ResponseEntity.ok("Tipo de solicitud eliminado correctamente.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
