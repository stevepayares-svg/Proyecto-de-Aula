package com.empresa.solicitudes.security;

import com.empresa.solicitudes.entity.Cliente;
import com.empresa.solicitudes.entity.Empleado;
import com.empresa.solicitudes.repository.ClienteRepository;
import com.empresa.solicitudes.repository.EmpleadoRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    public static final String SESSION_EMAIL_PENDIENTE = "GOOGLE_EMAIL_PENDIENTE";

    private final EmpleadoRepository empleadoRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtCookieService cookieService;

    public OAuth2AuthenticationSuccessHandler(EmpleadoRepository empleadoRepository,
                                               ClienteRepository clienteRepository,
                                               PasswordEncoder passwordEncoder,
                                               JwtService jwtService,
                                               JwtCookieService cookieService) {
        this.empleadoRepository = empleadoRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cookieService = cookieService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        SecurityContextHolder.clearContext();

        OAuth2User googleUser = (OAuth2User) authentication.getPrincipal();
        String email = googleUser.getAttribute("email");
        String nombre = googleUser.getAttribute("name");
        Boolean verificado = googleUser.getAttribute("email_verified");

        if (email == null || !Boolean.TRUE.equals(verificado)) {
            response.sendRedirect("/login?errorGoogle");
            return;
        }

        Empleado empleado = empleadoRepository.findByEmail(email).orElse(null);
        Cliente cliente = clienteRepository.findByEmail(email).orElse(null);

        // Si el correo está registrado en ambas colecciones, el usuario debe elegir.
        if (empleado != null && cliente != null) {
            HttpSession session = request.getSession(true);
            session.setAttribute(SESSION_EMAIL_PENDIENTE, email);
            response.sendRedirect("/elegir-rol");
            return;
        }

        if (empleado != null) {
            iniciarComoEmpleado(empleado, response);
            return;
        }

        if (cliente == null) {
            cliente = crearCliente(email, nombre);
        }
        iniciarComoCliente(cliente, response);
    }

    private Cliente crearCliente(String email, String nombre) {
        Cliente cliente = new Cliente();
        cliente.setNombre(nombre != null ? nombre : email);
        cliente.setEmail(email);
        cliente.setFechaRegistro(new Date());
        cliente.setContrasena(passwordEncoder.encode(UUID.randomUUID().toString()));
        return clienteRepository.save(cliente);
    }

    public static UserDetails usuarioCliente(Cliente cliente) {
        return User.withUsername(cliente.getEmail())
                .password(cliente.getContrasena() == null ? "" : cliente.getContrasena())
                .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))
                .build();
    }

    public static UserDetails usuarioEmpleado(Empleado empleado) {
        List<SimpleGrantedAuthority> roles = new ArrayList<>();
        roles.add(new SimpleGrantedAuthority("ROLE_EMPLEADO"));
        if (empleado.getCargo() != null && empleado.getCargo().trim().equalsIgnoreCase("Administrador")) {
            roles.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return User.withUsername(empleado.getEmail())
                .password(empleado.getContrasena() == null ? "" : empleado.getContrasena())
                .authorities(roles)
                .build();
    }

    public static void guardarTokenYRedirigir(UserDetails user, HttpServletResponse response,
                                       JwtService jwtService, JwtCookieService cookieService) throws IOException {
        cookieService.guardarToken(response, jwtService.generarToken(user));
        response.sendRedirect(RedireccionPorRol.urlInicio(user));
    }

    private void iniciarComoEmpleado(Empleado empleado, HttpServletResponse response) throws IOException {
        guardarTokenYRedirigir(usuarioEmpleado(empleado), response, jwtService, cookieService);
    }

    private void iniciarComoCliente(Cliente cliente, HttpServletResponse response) throws IOException {
        guardarTokenYRedirigir(usuarioCliente(cliente), response, jwtService, cookieService);
    }
}
