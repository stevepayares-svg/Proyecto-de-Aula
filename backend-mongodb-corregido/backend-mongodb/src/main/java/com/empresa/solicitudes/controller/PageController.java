package com.empresa.solicitudes.controller;

import com.empresa.solicitudes.entity.*;
import com.empresa.solicitudes.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.empresa.solicitudes.security.JwtCookieService;
import com.empresa.solicitudes.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PageController {

    @Autowired private IClienteService clienteService;
    @Autowired private IEmpleadoService empleadoService;
    @Autowired private ISolicitudService solicitudService;
    @Autowired private ITipoSolicitudService tipoSolicitudService;
    @Autowired private ICategoriaService categoriaService;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtService jwtService;
    @Autowired private JwtCookieService jwtCookieService;

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/registro")
    public String mostrarRegistro() {
        return "registro";
    }

    // Registro real: crea un Cliente de verdad con IClienteService (que ya
    // encripta la contraseña), y de una vez lo loguea para que caiga
    // directo en el menú de PQRSF, sin tener que volver a escribir sus datos.
    @PostMapping("/registro")
    public String registrar(@RequestParam String nombre,
                             @RequestParam String email,
                             @RequestParam(required = false) String telefono,
                             @RequestParam String contrasena,
                             Model model,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        try {
            Cliente cliente = new Cliente();
            cliente.setNombre(nombre);
            cliente.setEmail(email);
            cliente.setTelefono(telefono);
            cliente.setContrasena(contrasena);
            cliente.setFechaRegistro(new Date());
            clienteService.guardar(cliente);

            // Auto-login: usamos la misma contraseña que acaba de escribir
            // y le entregamos su JWT en la cookie (ya no se usa la sesion).
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, contrasena));
            UserDetails user = (UserDetails) authentication.getPrincipal();
            jwtCookieService.guardarToken(response, jwtService.generarToken(user));

            return "redirect:/solicitudes/nueva";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "registro";
        }
    }

    
    @GetMapping("/acceso-denegado")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String accesoDenegado() {
        return "error/403";
    }

    
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        boolean esEmpleado = esEmpleado(authentication);

        model.addAttribute("email", authentication.getName());
        model.addAttribute("esEmpleado", esEmpleado);
        // El empleado solo cuenta las solicitudes de su categoría; el admin cuenta todas.
        model.addAttribute("totalSolicitudes", solicitudesVisibles(authentication).size());

        if (esEmpleado) {
            model.addAttribute("totalClientes", clienteService.listarTodos().size());
            model.addAttribute("totalEmpleados", empleadoService.listarTodos().size());
        }

        return "dashboard";
    }

    // ─────────────────────────── MENÚ PQRSF (CLIENTE) ───────────────────────────

    @GetMapping("/solicitudes/nueva")
    public String nuevaSolicitudForm(Model model) {
        model.addAttribute("tipos", tipoSolicitudService.listarTodos());
        model.addAttribute("categorias", categoriaService.listar());
        return "nueva-solicitud";
    }

    @PostMapping("/solicitudes/nueva")
    public String crearSolicitud(@RequestParam String titulo,
                                  @RequestParam String tipoSolicitudId,
                                  @RequestParam(required = false) List<String> categoriaIds,
                                  @RequestParam String descripcion,
                                  @RequestParam(required = false) String prioridad,
                                  @RequestParam(required = false) String sede,
                                  @RequestParam(required = false) String zonaGimnasio,
                                  @RequestParam(required = false) String maquinaAfectada,
                                  Authentication authentication,
                                  Model model) {
        try {
            Cliente cliente = clienteService.buscarPorEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

            // Sin categoría ningún empleado la podría ver, así que es obligatoria.
            if (categoriaIds == null || categoriaIds.isEmpty()) {
                throw new IllegalArgumentException("Elige al menos una categoría para tu solicitud.");
            }

            Solicitud solicitud = new Solicitud();
            solicitud.setTitulo(titulo);
            solicitud.setDescripcion(descripcion);
            solicitud.setPrioridad((prioridad == null || prioridad.isBlank()) ? "Media" : prioridad);
            solicitud.setSede(sede);
            solicitud.setZonaGimnasio(zonaGimnasio);
            solicitud.setMaquinaAfectada(maquinaAfectada);
            solicitud.setCliente(cliente);
            
            solicitud.setTipoSolicitudId(tipoSolicitudId);
            solicitud.setCategoriaIds(categoriaIds);

            solicitudService.guardar(solicitud);

            return "redirect:/solicitudes/mias?creada";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("tipos", tipoSolicitudService.listarTodos());
            model.addAttribute("categorias", categoriaService.listar());
            return "nueva-solicitud";
        }
    }

    @GetMapping("/solicitudes/mias")
    public String misSolicitudes(Model model, Authentication authentication) {
        Cliente cliente = clienteService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        model.addAttribute("solicitudes", solicitudService.buscarPorCliente(cliente.getId()));
        return "mis-solicitudes";
    }

    // ─────────────────────── GESTIÓN DE SOLICITUDES (EMPLEADO) ───────────────────────

    // El empleado ve solo las solicitudes de su categoría. El admin ve todas.
    // Se puede filtrar por prioridad con ?prioridad=Alta (Baja, Media o Alta).
    @GetMapping("/solicitudes/todas")
    public String todasSolicitudes(@RequestParam(required = false) String prioridad,
                                   Model model, Authentication authentication) {
        List<Solicitud> solicitudes = solicitudesVisibles(authentication);

        if (prioridad != null && !prioridad.isBlank()) {
            solicitudes = solicitudes.stream()
                    .filter(s -> prioridad.equalsIgnoreCase(s.getPrioridad()))
                    .toList();
        }

        solicitudes = solicitudes.stream()
                .sorted(Comparator.comparing(Solicitud::getFechaCreacion,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        model.addAttribute("solicitudes", solicitudes);
        model.addAttribute("prioridadSeleccionada", prioridad == null ? "" : prioridad);
        model.addAttribute("categoriaEmpleado", nombreCategoriaDe(authentication));
        return "admin-solicitudes";
    }

    @GetMapping("/solicitudes/{id}/gestionar")
    public String gestionarSolicitudForm(@PathVariable String id, Model model, Authentication authentication) {
        Solicitud s = solicitudService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        verificarQuePuedeGestionar(authentication, s);
        model.addAttribute("solicitud", s);
        return "gestionar-solicitud";
    }

    @PostMapping("/solicitudes/{id}/gestionar")
    public String gestionarSolicitud(@PathVariable String id,
                                      @RequestParam String estado,
                                      @RequestParam(required = false) String respuestaAdmin,
                                      Authentication authentication) {
        Solicitud s = solicitudService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        verificarQuePuedeGestionar(authentication, s);
        s.setEstado(estado);
        s.setRespuestaAdmin(respuestaAdmin);
        if (respuestaAdmin != null && !respuestaAdmin.isBlank()) {
            s.setFechaRespuesta(new Date());
        }
        solicitudService.guardar(s);
        return "redirect:/solicitudes/todas";
    }

    // ───────────── Reglas de "qué solicitudes le tocan a cada empleado" ─────────────

    private boolean esAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));
    }

    private Empleado empleadoActual(Authentication authentication) {
        return empleadoService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));
    }

    // Admin: todas. Empleado: solo las que tienen su categoría (si no tiene una asignada, ninguna).
    private List<Solicitud> solicitudesVisibles(Authentication authentication) {
        if (esAdmin(authentication)) {
            return solicitudService.listarTodas();
        }
        String categoriaId = empleadoActual(authentication).getCategoriaId();
        if (categoriaId == null || categoriaId.isBlank()) {
            return List.of();
        }
        return solicitudService.buscarPorCategoria(categoriaId);
    }

    private void verificarQuePuedeGestionar(Authentication authentication, Solicitud solicitud) {
        if (esAdmin(authentication)) {
            return;
        }
        String categoriaId = empleadoActual(authentication).getCategoriaId();
        boolean escogida = categoriaId != null
                && solicitud.getCategoriaIds() != null
                && solicitud.getCategoriaIds().contains(categoriaId);
        if (!escogida) {
            throw new AccessDeniedException("Esta solicitud no es de tu categoría");
        }
    }

    // Nombre de la categoría del empleado (para mostrarlo arriba de la lista). El admin no tiene.
    private String nombreCategoriaDe(Authentication authentication) {
        if (esAdmin(authentication)) {
            return null;
        }
        String categoriaId = empleadoActual(authentication).getCategoriaId();
        if (categoriaId == null) {
            return null;
        }
        return categoriaService.buscarPorId(categoriaId).map(Categoria::getNombre).orElse(null);
    }

    private boolean esEmpleado(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_EMPLEADO"));
    }

    // ───────────────────────── ADMINISTRACIÓN (solo ADMIN) ─────────────────────────

    @GetMapping("/admin/empleados")
    public String listarEmpleados(Model model) {
        Map<String, String> categoriasPorId = new HashMap<>();
        categoriaService.listar().forEach(c -> categoriasPorId.put(c.getId(), c.getNombre()));
        model.addAttribute("empleados", empleadoService.listarTodos());
        model.addAttribute("categoriasPorId", categoriasPorId);
        return "admin-empleados";
    }

    @GetMapping("/admin/empleados/nuevo")
    public String nuevoEmpleadoForm(Model model) {
        model.addAttribute("categorias", categoriaService.listar());
        return "nuevo-empleado";
    }

    // Un empleado normal queda atado a UNA categoría: solo verá y gestionará esas solicitudes.
    // El administrador no necesita categoría porque ve todo.
    @PostMapping("/admin/empleados/nuevo")
    public String crearEmpleado(@RequestParam String nombre,
                                 @RequestParam String email,
                                 @RequestParam String cargo,
                                 @RequestParam(required = false) String categoriaId,
                                 @RequestParam String contrasena,
                                 Model model) {
        try {
            boolean esAdmin = cargo.trim().equalsIgnoreCase("Administrador");

            if (!esAdmin) {
                if (categoriaId == null || categoriaId.isBlank()) {
                    throw new IllegalArgumentException("Elige la categoría que va a atender el empleado.");
                }
                categoriaService.buscarPorId(categoriaId)
                        .orElseThrow(() -> new IllegalArgumentException("La categoría elegida no existe."));
            }

            Empleado empleado = new Empleado();
            empleado.setNombre(nombre);
            empleado.setEmail(email);
            empleado.setCargo(cargo);
            empleado.setCategoriaId(esAdmin ? null : categoriaId);
            empleado.setContrasena(contrasena);
            empleado.setFechaContratacion(new Date());
            empleadoService.guardar(empleado);
            return "redirect:/admin/empleados";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categorias", categoriaService.listar());
            return "nuevo-empleado";
        }
    }

    @PostMapping("/admin/empleados/{id}/eliminar")
    public String eliminarEmpleado(@PathVariable String id) {
        empleadoService.eliminar(id);
        return "redirect:/admin/empleados";
    }

    // Catálogos: tipos de PQRSF y categorías que alimentan el menú de "Nueva Solicitud"
    @GetMapping("/admin/catalogos")
    public String catalogos(Model model) {
        model.addAttribute("tipos", tipoSolicitudService.listarTodos());
        model.addAttribute("categorias", categoriaService.listar());
        return "admin-catalogos";
    }

    @PostMapping("/admin/catalogos/tipos")
    public String crearTipo(@RequestParam String nombre, @RequestParam(required = false) String descripcion) {
        TipoSolicitud tipo = new TipoSolicitud();
        tipo.setNombre(nombre);
        tipo.setDescripcion(descripcion);
        tipoSolicitudService.guardar(tipo);
        return "redirect:/admin/catalogos";
    }

    @PostMapping("/admin/catalogos/categorias")
    public String crearCategoria(@RequestParam String nombre, @RequestParam(required = false) String descripcion) {
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setDescripcion(descripcion);
        categoriaService.guardar(categoria);
        return "redirect:/admin/catalogos";
    }

    @PostMapping("/admin/catalogos/tipos/{id}/editar")
    public String editarTipo(@PathVariable String id, @RequestParam String nombre,
                              @RequestParam(required = false) String descripcion) {
        TipoSolicitud tipo = new TipoSolicitud();
        tipo.setNombre(nombre);
        tipo.setDescripcion(descripcion);
        tipoSolicitudService.actualizar(id, tipo);
        return "redirect:/admin/catalogos";
    }

    @PostMapping("/admin/catalogos/tipos/{id}/eliminar")
    public String eliminarTipo(@PathVariable String id) {
        tipoSolicitudService.eliminar(id);
        return "redirect:/admin/catalogos";
    }

    @PostMapping("/admin/catalogos/categorias/{id}/editar")
    public String editarCategoria(@PathVariable String id, @RequestParam String nombre,
                                   @RequestParam(required = false) String descripcion) {
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setDescripcion(descripcion);
        categoriaService.actualizar(id, categoria);
        return "redirect:/admin/catalogos";
    }

    @PostMapping("/admin/catalogos/categorias/{id}/eliminar")
    public String eliminarCategoria(@PathVariable String id) {
        categoriaService.eliminar(id);
        return "redirect:/admin/catalogos";
    }
}
