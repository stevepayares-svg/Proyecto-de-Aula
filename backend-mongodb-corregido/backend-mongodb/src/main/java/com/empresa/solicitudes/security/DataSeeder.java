package com.empresa.solicitudes.security;

import com.empresa.solicitudes.entity.Categoria;
import com.empresa.solicitudes.entity.Cliente;
import com.empresa.solicitudes.entity.Empleado;
import com.empresa.solicitudes.entity.TipoSolicitud;
import com.empresa.solicitudes.repository.CategoriaRepository;
import com.empresa.solicitudes.repository.ClienteRepository;
import com.empresa.solicitudes.repository.EmpleadoRepository;
import com.empresa.solicitudes.repository.TipoSolicitudRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@Order(1) // primero: crea admin, tipos y categorias (el generador masivo los necesita)
public class DataSeeder implements CommandLineRunner {

    private final EmpleadoRepository empleadoRepository;
    private final ClienteRepository clienteRepository;
    private final TipoSolicitudRepository tipoSolicitudRepository;
    private final CategoriaRepository categoriaRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(EmpleadoRepository empleadoRepository, ClienteRepository clienteRepository,
                       TipoSolicitudRepository tipoSolicitudRepository, CategoriaRepository categoriaRepository,
                       PasswordEncoder passwordEncoder) {
        this.empleadoRepository = empleadoRepository;
        this.clienteRepository = clienteRepository;
        this.tipoSolicitudRepository = tipoSolicitudRepository;
        this.categoriaRepository = categoriaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (empleadoRepository.findByEmail("admin@bodytech.com").isEmpty()) {
            Empleado admin = new Empleado();
            admin.setNombre("Administrador BodyTech");
            admin.setEmail("admin@bodytech.com");
            admin.setCargo("Administrador");
            admin.setContrasena(passwordEncoder.encode("admin123"));
            admin.setFechaContratacion(new Date());
            empleadoRepository.save(admin);
        }

        if (clienteRepository.findByEmail("cliente@demo.com").isEmpty()) {
            Cliente cliente = new Cliente();
            cliente.setNombre("Cliente de Prueba");
            cliente.setEmail("cliente@demo.com");
            cliente.setTelefono("3000000000");
            cliente.setContrasena(passwordEncoder.encode("cliente123"));
            cliente.setFechaRegistro(new Date());
            clienteRepository.save(cliente);
        }

        for (String[] tipo : new String[][]{
                {"Petición", "Solicitud de información o de un trámite"},
                {"Queja", "Manifestación de inconformidad con la atención recibida"},
                {"Reclamo", "Exigencia de una solución frente a un problema concreto"},
                {"Sugerencia", "Propuesta para mejorar el servicio"},
                {"Felicitación", "Reconocimiento por un buen servicio recibido"}
        }) {
            if (!tipoSolicitudRepository.existsByNombre(tipo[0])) {
                TipoSolicitud t = new TipoSolicitud();
                t.setNombre(tipo[0]);
                t.setDescripcion(tipo[1]);
                tipoSolicitudRepository.save(t);
            }
        }

        if (categoriaRepository.count() == 0) {
            for (String[] cat : new String[][]{
                    {"Atención al cliente", "Trato del personal, tiempos de espera, etc."},
                    {"Instalaciones", "Estado de baños, vestidores, zonas comunes"},
                    {"Máquinas y equipos", "Fallas o mantenimiento de equipos del gimnasio"},
                    {"Instructores", "Clases dirigidas, entrenadores, rutinas"},
                    {"Facturación y pagos", "Cobros, membresías, facturas"},
                    {"Otro", "Cualquier otro tema no listado"}
            }) {
                Categoria c = new Categoria();
                c.setNombre(cat[0]);
                c.setDescripcion(cat[1]);
                categoriaRepository.save(c);
            }
        }
    }
}
