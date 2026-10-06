package com.kinal.app.biblioteca.controller;

import com.kinal.app.biblioteca.dto.prestamo.PrestamoRequest;
import com.kinal.app.biblioteca.dto.prestamo.PrestamoResponse;
import com.kinal.app.biblioteca.service.PrestamoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/prestamos")
@RequiredArgsConstructor
public class PrestamoController {

    private final PrestamoService prestamoService;

    @PostMapping
    public ResponseEntity<PrestamoResponse> registrarPrestamo(
            @Valid @RequestBody PrestamoRequest request,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        PrestamoResponse response = prestamoService.registrarPrestamo(request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/devolucion")
    public ResponseEntity<PrestamoResponse> registrarDevolucion(@PathVariable Long id) {
        PrestamoResponse response = prestamoService.registrarDevolucion(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mis-prestamos")
    public ResponseEntity<List<PrestamoResponse>> misPrestamos(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        List<PrestamoResponse> response = prestamoService.obtenerMisPrestamos(email);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/atrasados")
    public ResponseEntity<List<PrestamoResponse>> prestamosAtrasados() {
        List<PrestamoResponse> response = prestamoService.obtenerAtrasados();
        return ResponseEntity.ok(response);
    }
}
