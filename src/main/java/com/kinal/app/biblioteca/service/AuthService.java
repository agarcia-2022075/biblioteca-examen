package com.kinal.app.biblioteca.service;

import com.kinal.app.biblioteca.dto.auth.AuthResponse;
import com.kinal.app.biblioteca.dto.auth.LoginRequest;
import com.kinal.app.biblioteca.dto.auth.RegisterRequest;
import com.kinal.app.biblioteca.entity.Usuario;
import com.kinal.app.biblioteca.entity.enums.EstadoUsuario;
import com.kinal.app.biblioteca.entity.enums.Rol;
import com.kinal.app.biblioteca.exception.BusinessRuleException;
import com.kinal.app.biblioteca.exception.ResourceNotFoundException;
import com.kinal.app.biblioteca.repository.UsuarioRepository;
import com.kinal.app.biblioteca.security.CustomUserDetails;
import com.kinal.app.biblioteca.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("El email ya se encuentra registrado en el sistema");
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .estado(EstadoUsuario.ACTIVO)
                .rol(Rol.LECTOR)
                .build();

        usuarioRepository.save(usuario);

        CustomUserDetails userDetails = new CustomUserDetails(usuario);
        String token = jwtUtils.generateToken(userDetails, usuario.getRol().name());

        return AuthResponse.builder()
                .token(token)
                .accessToken(token)
                .email(usuario.getEmail())
                .rol(usuario.getRol().name())
                .mensaje("Usuario registrado exitosamente")
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Usuario usuario = userDetails.getUsuario();

        String token = jwtUtils.generateToken(userDetails, usuario.getRol().name());

        return AuthResponse.builder()
                .token(token)
                .accessToken(token)
                .email(usuario.getEmail())
                .rol(usuario.getRol().name())
                .mensaje("Inicio de sesion exitoso")
                .build();
    }
}
