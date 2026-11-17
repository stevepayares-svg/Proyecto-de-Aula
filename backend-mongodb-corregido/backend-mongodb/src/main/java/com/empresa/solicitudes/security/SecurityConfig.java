package com.empresa.solicitudes.security;

import com.empresa.solicitudes.repository.ClienteRepository;
import com.empresa.solicitudes.repository.EmpleadoRepository;
import com.empresa.solicitudes.security.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Seguridad con JWT + OAuth2 (Google).
 *
 * Antes: formLogin guardaba al usuario en la SESION del servidor (JSESSIONID).
 * Ahora:  el servidor NO guarda sesion (STATELESS). Al iniciar sesion se genera
 *         un JWT que viaja en la cookie JWT_TOKEN (navegador) o en el header
 *         "Authorization: Bearer <token>" (API). JwtAuthenticationFilter lo
 *         valida en cada peticion.
 *
 * Formas de iniciar sesion (todas terminan en el mismo JWT):
 *  1. Formulario /login (correo + contrasena)      -> cookie JWT_TOKEN
 *  2. Boton "Google" /oauth2/authorization/google  -> cookie JWT_TOKEN
 *  3. POST /api/auth/login (JSON)                  -> token en la respuesta
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${app.security.proteger-api:true}")
    private boolean protegerApi;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UsuarioDetailsService usuarioDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * El filtro JWT es un @Component; esto evita que Spring Boot lo registre
     * por segunda vez fuera de Spring Security. Solo debe correr dentro de la cadena.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registro = new FilterRegistrationBean<>(filter);
        registro.setEnabled(false);
        return registro;
    }

    /**
     * Login con correo y contrasena correcto: se genera el JWT y se guarda
     * en la cookie. Luego se redirige segun el rol (igual que antes).
     * (No es @Bean para no confundirlo con OAuth2AuthenticationSuccessHandler.)
     */
    private AuthenticationSuccessHandler formLoginSuccessHandler(JwtService jwtService,
                                                                 JwtCookieService cookieService,
                                                                 EmpleadoRepository empleadoRepository,
                                                                 ClienteRepository clienteRepository) {
        return (request, response, authentication) -> {
            UserDetails user = (UserDetails) authentication.getPrincipal();
            String email = user.getUsername();

            // Si el correo existe en las dos colecciones, también en el login
            // tradicional se solicita elegir el perfil antes de crear el JWT.
            boolean esEmpleado = empleadoRepository.findByEmail(email).isPresent();
            boolean esCliente = clienteRepository.findByEmail(email).isPresent();
            if (esEmpleado && esCliente) {
                HttpSession session = request.getSession(true);
                session.setAttribute(OAuth2AuthenticationSuccessHandler.SESSION_EMAIL_PENDIENTE, email);
                response.sendRedirect(request.getContextPath() + "/elegir-rol");
                return;
            }

            cookieService.guardarToken(response, jwtService.generarToken(user));
            response.sendRedirect(request.getContextPath() + RedireccionPorRol.urlInicio(user));
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler(JwtCookieService cookieService) {
        return (request, response, accessDeniedException) -> {
            // Se muestra en la consola de VS Code para saber POR QUE se nego el acceso
            log.warn("Acceso denegado a {} {} -> {}", request.getMethod(), request.getRequestURI(),
                    accessDeniedException.getClass().getSimpleName());
            if (esApi(request)) {
                responderJson(response, HttpServletResponse.SC_FORBIDDEN,
                        "No tienes permiso para este recurso");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/acceso-denegado");
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtFilter,
                                           JwtService jwtService,
                                           JwtCookieService cookieService,
                                           AccessDeniedHandler accessDeniedHandler,
                                           OAuth2AuthenticationSuccessHandler oauth2SuccessHandler,
                                           EmpleadoRepository empleadoRepository,
                                           ClienteRepository clienteRepository) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Con JWT se desactiva CSRF (igual que en la guia de la profe).
                // La cookie JWT_TOKEN es SameSite=Lax: el navegador no la envia en
                // formularios que vengan de otros sitios, que es lo que evitaba CSRF.
                .csrf(csrf -> csrf.disable())

                // JWT: el servidor no guarda al usuario en sesion
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/", "/login", "/registro", "/css/**", "/js/**", "/webjars/**", "/img/**",
                            "/error", "/acceso-denegado", "/elegir-rol"
                    ).permitAll();
                    // Login con Google (los maneja Spring)
                    auth.requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll();
                    // Login por API: publico (es donde se pide el token)
                    auth.requestMatchers("/api/auth/login").permitAll();
                    // Registro de clientes por API: publico (todavia no tiene token)
                    auth.requestMatchers(HttpMethod.POST, "/api/clientes").permitAll();
                    // Resto de la API: con token (o abierta si app.security.proteger-api=false)
                    if (protegerApi) {
                        auth.requestMatchers("/api/**").authenticated();
                    } else {
                        auth.requestMatchers("/api/**").permitAll();
                    }
                    // El menu de PQRSF es solo para CLIENTE
                    auth.requestMatchers("/solicitudes/nueva", "/solicitudes/mias").hasRole("CLIENTE");
                    // Dashboard y gestion de solicitudes: EMPLEADO o ADMIN (el empleado solo ve las de su categoría, lo valida PageController)
                    auth.requestMatchers("/dashboard", "/solicitudes/todas", "/solicitudes/*/gestionar")
                            .hasAnyRole("EMPLEADO", "ADMIN");
                    // Solo ADMIN administra empleados y catalogos
                    auth.requestMatchers("/admin/**").hasRole("ADMIN");
                    auth.anyRequest().authenticated();
                })

                // 1. Login tradicional (el mismo formulario de siempre)
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username") // en el form se llama "username" pero es el email
                        .passwordParameter("password")
                        .successHandler(formLoginSuccessHandler(jwtService, cookieService, empleadoRepository, clienteRepository))
                        .failureUrl("/login?error")
                        .permitAll()
                )

                // 2. Login con Google
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .successHandler(oauth2SuccessHandler)
                        .failureUrl("/login?errorGoogle")
                )

                // Cerrar sesion = borrar la cookie del JWT
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies(cookieService.getCookieName(), "JSESSIONID")
                        .permitAll()
                )

                .exceptionHandling(handling -> handling
                        // Sin token (o token vencido)
                        .authenticationEntryPoint((request, response, ex) -> {
                            boolean expirado = request.getAttribute(JwtAuthenticationFilter.ATRIBUTO_TOKEN_EXPIRADO) != null;
                            if (esApi(request)) {
                                responderJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        expirado ? "El token expiro" : "Falta el token o no es valido");
                            } else {
                                response.sendRedirect(request.getContextPath()
                                        + (expirado ? "/login?expirada" : "/login?requiereLogin"));
                            }
                        })
                        // Con token pero sin el rol necesario
                        .accessDeniedHandler(accessDeniedHandler)
                );

        // 3. El filtro JWT corre ANTES del filtro de usuario/contrasena de Spring
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Permite llamar a la API desde otros front-ends que corran en localhost (ej: Angular en 4200).
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*")); // incluye "Authorization"

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private static boolean esApi(HttpServletRequest request) {
        return request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    private static void responderJson(HttpServletResponse response, int status, String mensaje) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"error\":\"" + mensaje + "\"}");
    }
}