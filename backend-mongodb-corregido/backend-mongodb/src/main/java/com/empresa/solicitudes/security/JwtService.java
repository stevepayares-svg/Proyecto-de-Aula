package com.empresa.solicitudes.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Collections;


@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generarToken(UserDetails user) {
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        Date ahora = new Date();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("roles", roles)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }


    public String extraerUsername(String token) {
        return leerClaims(token).getSubject();
    }

    public List<String> extraerRoles(String token) {
        Object roles = leerClaims(token).get("roles");
        if (!(roles instanceof List<?> lista)) return Collections.emptyList();
        return lista.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    private Claims leerClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)          
                .build()
                .parseSignedClaims(token) 
                .getPayload();
    }
}
