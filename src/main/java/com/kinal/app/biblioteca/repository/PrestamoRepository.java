package com.kinal.app.biblioteca.repository;

import com.kinal.app.biblioteca.entity.Prestamo;
import com.kinal.app.biblioteca.entity.enums.EstadoPrestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    // Regla de negocio: maximo 3 prestamos activos por lector
    long countByUsuarioIdAndEstado(Long usuarioId, EstadoPrestamo estado);

    // Regla de negocio: verificar si tiene prestamos vencidos antes de permitir uno nuevo
    boolean existsByUsuarioIdAndEstadoAndFechaDevolucionEsperadaBefore(Long usuarioId,
                                                                      EstadoPrestamo estado,
                                                                      LocalDate fecha);

    // Valida si un libro tiene prestamos en un estado especifico (ej. ACTIVO antes de borrar)
    boolean existsByLibroIdAndEstado(Long libroId, EstadoPrestamo estado);

    // Consulta para mis-prestamos sin problema de N+1 (JOIN FETCH)
    @Query("SELECT p FROM Prestamo p " +
           "JOIN FETCH p.libro " +
           "JOIN FETCH p.usuario " +
           "WHERE p.usuario.id = :usuarioId " +
           "ORDER BY p.fechaPrestamo DESC")
    List<Prestamo> findByUsuarioIdWithDetalle(@Param("usuarioId") Long usuarioId);

    // Consulta para prestamos atrasados sin problema de N+1 (JOIN FETCH)
    @Query("SELECT p FROM Prestamo p " +
           "JOIN FETCH p.libro " +
           "JOIN FETCH p.usuario " +
           "WHERE p.estado = :estado AND p.fechaDevolucionEsperada < :fechaActual " +
           "ORDER BY p.fechaDevolucionEsperada ASC")
    List<Prestamo> findAtrasadosConDetalle(@Param("estado") EstadoPrestamo estado,
                                          @Param("fechaActual") LocalDate fechaActual);

    // Buscar prestamo por id con libro y usuario cargados (util para devoluciones)
    @Query("SELECT p FROM Prestamo p " +
           "JOIN FETCH p.libro " +
           "JOIN FETCH p.usuario " +
           "WHERE p.id = :id")
    Optional<Prestamo> findByIdWithDetalle(@Param("id") Long id);
}
