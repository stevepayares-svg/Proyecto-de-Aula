package com.empresa.solicitudes.controller;

import com.empresa.solicitudes.entity.Seguimiento;
import com.empresa.solicitudes.service.ISeguimientoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seguimientos")
public class SeguimientoController {

    @Autowired
    private ISeguimientoService seguimientoService;

    // GET /api/seguimientos/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable String id) {
        return seguimientoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/seguimientos/solicitud/{solicitudId}
    @GetMapping("/solicitud/{solicitudId}")
    public ResponseEntity<List<Seguimiento>> listarPorSolicitud(@PathVariable String solicitudId) {
        return ResponseEntity.ok(seguimientoService.listarPorSolicitud(solicitudId));
    }

    // GET /api/seguimientos/solicitud/{solicitudId}/recientes
    @GetMapping("/solicitud/{solicitudId}/recientes")
    public ResponseEntity<List<Seguimiento>> listarPorSolicitudOrdenado(@PathVariable String solicitudId) {
        return ResponseEntity.ok(seguimientoService.listarPorSolicitudOrdenado(solicitudId));
    }

    // POST /api/seguimientos
    @PostMapping
    public ResponseEntity<?> guardar(@RequestBody Seguimiento seguimiento) {
        try {
            Seguimiento nuevo = seguimientoService.guardar(seguimiento);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PUT /api/seguimientos/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable String id, @RequestBody Seguimiento seguimiento) {
        try {
            Seguimiento actualizado = seguimientoService.actualizar(id, seguimiento);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE /api/seguimientos/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable String id) {
        try {
            seguimientoService.eliminar(id);
            return ResponseEntity.ok("Seguimiento eliminado correctamente.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
