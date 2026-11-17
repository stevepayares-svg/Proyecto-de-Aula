package com.empresa.solicitudes.service.impl;

import com.empresa.solicitudes.entity.Cliente;
import com.empresa.solicitudes.repository.ClienteRepository;
import com.empresa.solicitudes.repository.SolicitudRepository;
import com.empresa.solicitudes.service.IClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class ClienteServiceImpl implements IClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Cliente guardar(Cliente cliente) {
        if (clienteRepository.existsByEmail(cliente.getEmail())) {
            throw new RuntimeException("Ya existe un cliente con el email: " + cliente.getEmail());
        }
        if (cliente.getFechaRegistro() == null) {
            cliente.setFechaRegistro(new Date());
        }
        // La contraseña nunca se guarda en texto plano
        cliente.setContrasena(passwordEncoder.encode(cliente.getContrasena()));
        return clienteRepository.save(cliente);
    }

    @Override
    public Cliente actualizar(String id, Cliente clienteActualizado) {
        Cliente clienteExistente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con id: " + id));

        clienteExistente.setNombre(clienteActualizado.getNombre());
        clienteExistente.setEmail(clienteActualizado.getEmail());
        clienteExistente.setTelefono(clienteActualizado.getTelefono());

        return clienteRepository.save(clienteExistente);
    }

    @Override
    public void eliminar(String id) {
        if (!clienteRepository.existsById(id)) {
            throw new RuntimeException("Cliente no encontrado con id: " + id);
        }
        // En JPA esto lo hacía cascade = ALL; en Mongo no hay cascada automática,
        // así que se borran a mano las solicitudes del cliente.
        solicitudRepository.deleteAll(solicitudRepository.findByClienteId(id));
        clienteRepository.deleteById(id);
    }

    @Override
    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    @Override
    public Optional<Cliente> buscarPorEmail(String email) {
        return clienteRepository.findByEmail(email);
    }

    @Override
    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    @Override
    public boolean existePorEmail(String email) {
        return clienteRepository.existsByEmail(email);
    }
}
