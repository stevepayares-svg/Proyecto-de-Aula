package com.empresa.solicitudes.controller;

import com.empresa.solicitudes.entity.Cliente;
import com.empresa.solicitudes.entity.Empleado;
import com.empresa.solicitudes.repository.ClienteRepository;
import com.empresa.solicitudes.repository.EmpleadoRepository;
import com.empresa.solicitudes.security.JwtCookieService;
import com.empresa.solicitudes.security.JwtService;
import com.empresa.solicitudes.security.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
public class SeleccionRolController {

    private final EmpleadoRepository empleadoRepository;
    private final ClienteRepository clienteRepository;
    private final JwtService jwtService;
    private final JwtCookieService cookieService;

    public SeleccionRolController(EmpleadoRepository empleadoRepository,
                                  ClienteRepository clienteRepository,
                                  JwtService jwtService,
                                  JwtCookieService cookieService) {
        this.empleadoRepository = empleadoRepository;
        this.clienteRepository = clienteRepository;
        this.jwtService = jwtService;
        this.cookieService = cookieService;
    }

    @GetMapping("/elegir-rol")
    public String mostrarSeleccion(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        String email = session == null ? null : (String) session.getAttribute(
                OAuth2AuthenticationSuccessHandler.SESSION_EMAIL_PENDIENTE);
        if (email == null) return "redirect:/login?requiereLogin";

        model.addAttribute("email", email);
        return "elegir-rol";
    }

    @PostMapping("/elegir-rol")
    public void elegirRol(@RequestParam String rol,
                          HttpServletRequest request,
                          HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        String email = session == null ? null : (String) session.getAttribute(
                OAuth2AuthenticationSuccessHandler.SESSION_EMAIL_PENDIENTE);
        if (email == null) {
            response.sendRedirect("/login?requiereLogin");
            return;
        }

        if ("CLIENTE".equals(rol)) {
            Cliente cliente = clienteRepository.findByEmail(email).orElse(null);
            if (cliente == null) {
                response.sendRedirect("/login?errorGoogle");
                return;
            }
            invalidarSesion(session);
            OAuth2AuthenticationSuccessHandler.guardarTokenYRedirigir(
                    OAuth2AuthenticationSuccessHandler.usuarioCliente(cliente), response, jwtService, cookieService);
            return;
        }

        if ("EMPLEADO".equals(rol)) {
            Empleado empleado = empleadoRepository.findByEmail(email).orElse(null);
            if (empleado == null) {
                response.sendRedirect("/login?errorGoogle");
                return;
            }
            invalidarSesion(session);
            OAuth2AuthenticationSuccessHandler.guardarTokenYRedirigir(
                    OAuth2AuthenticationSuccessHandler.usuarioEmpleado(empleado), response, jwtService, cookieService);
            return;
        }

        response.sendRedirect("/elegir-rol?error");
    }

    private void invalidarSesion(HttpSession session) {
        if (session != null) session.invalidate();
    }
}
