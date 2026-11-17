package com.empresa.solicitudes.controller;

import com.empresa.solicitudes.dto.LoginResponse;
import com.empresa.solicitudes.entity.Cliente;
import com.empresa.solicitudes.entity.Empleado;
import com.empresa.solicitudes.repository.ClienteRepository;
import com.empresa.solicitudes.repository.EmpleadoRepository;
import com.empresa.solicitudes.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Login por API (para probar con Postman).
 *
 * POST /api/auth/login   {"email": "...", "password": "..."}
 *   -> devuelve LoginResponse con el JWT.
 * GET  /api/auth/me      (con Authorization: Bearer <token>)
 *   -> devuelve quien es el usuario del token (sirve para probar).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmpleadoRepository empleadoRepository;
    private final ClienteRepository clienteRepository;

    public AuthApiController(AuthenticationManager authenticationManager,
                             JwtService jwtService,
                             EmpleadoRepository empleadoRepository,
                             ClienteRepository clienteRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.empleadoRepository = empleadoRepository;
        this.clienteRepository = clienteRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String contrasena = body.get("password");

        if (email == null || contrasena == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Envia email y password."));
        }

        // AuthenticationManager -> UsuarioDetailsService (MongoDB) + BCrypt
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, contrasena));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Correo o contraseña incorrectos."));
        }

        UserDetails user = (UserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(respuestaLogin(user, email));
    }

    /** Arma el LoginResponse con el token y los datos del usuario. */
    private LoginResponse respuestaLogin(UserDetails user, String email) {
        String token = jwtService.generarToken(user);
        List<String> roles = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        Empleado emp = empleadoRepository.findByEmail(email).orElse(null);
        if (emp != null) {
            return new LoginResponse(token, "Bearer", jwtService.getExpirationMs(),
                    "EMPLEADO", emp.getId(), emp.getNombre(), emp.getEmail(), roles);
        }
        Cliente cli = clienteRepository.findByEmail(email).orElseThrow();
        return new LoginResponse(token, "Bearer", jwtService.getExpirationMs(),
                "CLIENTE", cli.getId(), cli.getNombre(), cli.getEmail(), roles);
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        return Map.of(
                "email", authentication.getName(),
                "roles", authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    }
}
