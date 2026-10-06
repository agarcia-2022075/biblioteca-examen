package com.kinal.app.biblioteca.dto.libro;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LibroRequest {

    @NotBlank(message = "El ISBN es obligatorio")
    private String isbn;

    @NotBlank(message = "El titulo es obligatorio")
    private String titulo;

    @NotBlank(message = "El autor es obligatorio")
    private String autor;

    @NotBlank(message = "La categoria es obligatoria")
    private String categoria;

    @NotNull(message = "El stockTotal es obligatorio")
    @Min(value = 0, message = "El stockTotal no puede ser negativo")
    private Integer stockTotal;

    @NotNull(message = "El stockDisponible es obligatorio")
    @Min(value = 0, message = "El stockDisponible no puede ser negativo")
    private Integer stockDisponible;
}
