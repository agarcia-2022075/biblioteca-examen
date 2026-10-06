package com.kinal.app.biblioteca.dto.prestamo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrestamoRequest {

    // Puede ser null si un lector solicita su propio prestamo; si lo registra admin/bibliotecario, puede especificarse
    private Long usuarioId;

    @NotNull(message = "El libroId es obligatorio")
    private Long libroId;
}
