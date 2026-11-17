package com.empresa.solicitudes.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ATRIBUTO_TOKEN_EXPIRADO = "jwtExpirado";

    private final JwtService jwtService;
    private final JwtCookieService cookieService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   JwtCookieService cookieService) {
        this.jwtService = jwtService;
        this.cookieService = cookieService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String token = extraerToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String email = jwtService.extraerUsername(token);
                var roles = jwtService.extraerRoles(token);
                if (roles.isEmpty()) {
                    cookieService.borrarToken(response);
                    chain.doFilter(request, response);
                    return;
                }
                var authorities = roles.stream().map(SimpleGrantedAuthority::new).toList();
                User user = new User(email, "", authorities);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(user, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (ExpiredJwtException e) {
                request.setAttribute(ATRIBUTO_TOKEN_EXPIRADO, true);
                cookieService.borrarToken(response);
            } catch (JwtException | IllegalArgumentException e) {
                cookieService.borrarToken(response);
            }
        }

        chain.doFilter(request, response);
    }

    private String extraerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return cookieService.leerToken(request).orElse(null);
    }
}
