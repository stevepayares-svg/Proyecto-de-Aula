package com.empresa.solicitudes.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;


public final class RedireccionPorRol {

    private RedireccionPorRol() {}

    public static String urlInicio(UserDetails user) {
        List<String> roles = roles(user);
        if (roles.contains("ROLE_EMPLEADO") || roles.contains("ROLE_ADMIN")) {
            return "/dashboard";
        }
        return "/solicitudes/nueva";
    }

    private static List<String> roles(UserDetails user) {
        return user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }
}
