package com.kinal.app.biblioteca.entity;

import com.kinal.app.biblioteca.entity.enums.EstadoPrestamo;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "prestamos", indexes = {
    @Index(name = "idx_prestamos_usuario_id", columnList = "usuario_id"),
    @Index(name = "idx_prestamos_libro_id", columnList = "libro_id"),
    @Index(name = "idx_prestamos_estado", columnList = "estado"),
    @Index(name = "idx_prestamos_fecha_esperada", columnList = "fechaDevolucionEsperada"),
    @Index(name = "idx_prestamos_usuario_estado", columnList = "usuario_id, estado"),
    @Index(name = "idx_prestamos_estado_fecha", columnList = "estado, fechaDevolucionEsperada")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "libro_id", nullable = false)
    private Libro libro;

    @Column(nullable = false)
    private LocalDate fechaPrestamo;

    @Column(nullable = false)
    private LocalDate fechaDevolucionEsperada;

    @Column
    private LocalDate fechaDevolucionReal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoPrestamo estado = EstadoPrestamo.ACTIVO;
}
