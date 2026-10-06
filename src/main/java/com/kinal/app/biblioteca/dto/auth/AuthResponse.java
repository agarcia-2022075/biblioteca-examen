package com.kinal.app.biblioteca.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String accessToken;
    @Builder.Default
    private String tipo = "Bearer";
    private String email;
    private String rol;
    private String mensaje;
}
