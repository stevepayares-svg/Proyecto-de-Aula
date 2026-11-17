package com.empresa.solicitudes.controller;

import com.empresa.solicitudes.entity.Solicitud;
import com.empresa.solicitudes.service.ISolicitudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    @Autowired
    private ISolicitudService solicitudService;

    @GetMapping
    public ResponseEntity<List<Solicitud>> listarTodas() {
        return ResponseEntity.ok(solicitudService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable String id) {
        return solicitudService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Solicitud>> buscarPorEstado(@PathVariable String estado) {
        return ResponseEntity.ok(solicitudService.buscarPorEstado(estado));
    }

    @GetMapping("/prioridad/{prioridad}")
    public ResponseEntity<List<Solicitud>> buscarPorPrioridad(@PathVariable String prioridad) {
        return ResponseEntity.ok(solicitudService.buscarPorPrioridad(prioridad));
    }

    @GetMapping("/sede/{sede}")
    public ResponseEntity<List<Solicitud>> buscarPorSede(@PathVariable String sede) {
        return ResponseEntity.ok(solicitudService.buscarPorSede(sede));
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<Solicitud>> buscarPorCliente(@PathVariable String clienteId) {
        return ResponseEntity.ok(solicitudService.buscarPorCliente(clienteId));
    }

    @GetMapping("/tipo/{tipoId}")
    public ResponseEntity<List<Solicitud>> buscarPorTipo(@PathVariable String tipoId) {
        return ResponseEntity.ok(solicitudService.buscarPorTipo(tipoId));
    }

    @GetMapping("/filtro")
    public ResponseEntity<List<Solicitud>> buscarPorEstadoYPrioridad(
            @RequestParam String estado,
            @RequestParam String prioridad) {
        return ResponseEntity.ok(
                solicitudService.buscarPorEstadoYPrioridad(estado, prioridad));
    }

    @PostMapping
    public ResponseEntity<?> guardar(@RequestBody Solicitud solicitud) {
        try {
            Solicitud nueva = solicitudService.guardar(solicitud);
            return ResponseEntity.ok(nueva);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable String id,
            @RequestBody Solicitud solicitud) {
        try {
            Solicitud actualizada = solicitudService.actualizar(id, solicitud);
            return ResponseEntity.ok(actualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstado(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            Solicitud s = solicitudService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
            s.setEstado(body.get("estado"));
            return ResponseEntity.ok(solicitudService.guardar(s));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/responder")
    public ResponseEntity<?> responder(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            Solicitud s = solicitudService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
            s.setRespuestaAdmin(body.get("respuestaAdmin"));
            return ResponseEntity.ok(solicitudService.guardar(s));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable String id) {
        try {
            solicitudService.eliminar(id);
            return ResponseEntity.ok("Solicitud eliminada correctamente.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}