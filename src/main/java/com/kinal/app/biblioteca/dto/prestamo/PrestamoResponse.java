package com.kinal.app.biblioteca.dto.prestamo;

import com.kinal.app.biblioteca.entity.enums.EstadoPrestamo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrestamoResponse {

    private Long id;
    private Long usuarioId;
    private String usuarioNombre;
    private String usuarioEmail;
    private Long libroId;
    private String libroTitulo;
    private String libroIsbn;
    private LocalDate fechaPrestamo;
    private LocalDate fechaDevolucionEsperada;
    private LocalDate fechaDevolucionReal;
    private EstadoPrestamo estado;
}
