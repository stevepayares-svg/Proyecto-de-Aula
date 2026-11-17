package com.empresa.solicitudes.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;


@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Los errores de seguridad (403) los maneja Spring Security: se vuelven a lanzar.
    @ExceptionHandler(AccessDeniedException.class)
    public void accesoDenegado(AccessDeniedException e) {
        throw e;
    }

    // URL que no existe -> 404
    @ExceptionHandler(NoResourceFoundException.class)
    public Object noEncontrada(NoResourceFoundException e, HttpServletRequest request) {
        return responder(request, HttpStatus.NOT_FOUND, "Página no encontrada",
                "La dirección que intentaste abrir no existe.");
    }

    // Método HTTP incorrecto -> 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Object metodoNoPermitido(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        return responder(request, HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido",
                "Esta dirección no acepta ese tipo de petición.");
    }

    
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public Object peticionInvalida(Exception e, HttpServletRequest request) {
        return responder(request, HttpStatus.BAD_REQUEST, "Petición inválida",
                "Faltan datos o tienen un formato incorrecto.");
    }

    @ExceptionHandler(RuntimeException.class)
    public Object errorDeNegocio(RuntimeException e, HttpServletRequest request) {
        String mensaje = e.getMessage() == null ? "" : e.getMessage();
        String minuscula = mensaje.toLowerCase();

        if (minuscula.contains("no encontrad")) {
            return responder(request, HttpStatus.NOT_FOUND, "Recurso no encontrado", mensaje);
        }
        if (minuscula.contains("ya existe")) {
            return responder(request, HttpStatus.CONFLICT, "Dato duplicado", mensaje);
        }
        if (minuscula.contains("no se puede")) {
            return responder(request, HttpStatus.CONFLICT, "Operación no permitida", mensaje);
        }
        log.error("Error inesperado en {}", request.getRequestURI(), e);
        return responder(request, HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado. Intenta de nuevo más tarde.");
    }

    // Cualquier otra cosa que no se haya previsto -> 500
    @ExceptionHandler(Exception.class)
    public Object errorGeneral(Exception e, HttpServletRequest request) {
        log.error("Error inesperado en {}", request.getRequestURI(), e);
        return responder(request, HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado. Intenta de nuevo más tarde.");
    }

    private Object responder(HttpServletRequest request, HttpStatus status, String titulo, String mensaje) {
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith(request.getContextPath() + "/api/")) {
            return ResponseEntity.status(status).body(Map.of(
                    "status", status.value(),
                    "error", titulo,
                    "mensaje", mensaje));
        }
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("titulo", titulo);
        mav.addObject("mensaje", mensaje);
        return mav;
    }
}
