package com.empresa.solicitudes.security;

import com.empresa.solicitudes.entity.Cliente;
import com.empresa.solicitudes.entity.Empleado;
import com.empresa.solicitudes.repository.ClienteRepository;
import com.empresa.solicitudes.repository.EmpleadoRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final EmpleadoRepository empleadoRepository;
    private final ClienteRepository clienteRepository;

    public UsuarioDetailsService(EmpleadoRepository empleadoRepository, ClienteRepository clienteRepository) {
        this.empleadoRepository = empleadoRepository;
        this.clienteRepository = clienteRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Empleado empleado = empleadoRepository.findByEmail(email).orElse(null);
        if (empleado != null) {
            List<GrantedAuthority> authorities = new ArrayList<>();

            boolean esAdmin = empleado.getCargo() != null
                    && empleado.getCargo().trim().equalsIgnoreCase("Administrador");

            // Todo empleado que crea el administrador puede entrar de una vez.
            authorities.add(new SimpleGrantedAuthority("ROLE_EMPLEADO"));
            if (esAdmin) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }

            return new User(empleado.getEmail(), empleado.getContrasena(), authorities);
        }

        Cliente cliente = clienteRepository.findByEmail(email).orElse(null);
        if (cliente != null) {
            return new User(
                    cliente.getEmail(),
                    cliente.getContrasena(),
                    List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));
        }

        throw new UsernameNotFoundException("No existe un usuario con el correo: " + email);
    }
}
