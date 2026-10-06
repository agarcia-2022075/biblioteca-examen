package com.kinal.app.biblioteca.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kinal.app.biblioteca.entity.enums.EstadoUsuario;
import com.kinal.app.biblioteca.entity.enums.Rol;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Rol rol = Rol.LECTOR;

    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<Prestamo> prestamos = new ArrayList<>();
}
