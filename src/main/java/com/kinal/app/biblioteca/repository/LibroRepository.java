package com.kinal.app.biblioteca.repository;

import com.kinal.app.biblioteca.entity.Libro;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LibroRepository extends JpaRepository<Libro, Long> {

    Optional<Libro> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    Optional<Libro> findByIdAndActivoTrue(Long id);

    // Bloqueo pesimista para evitar condiciones de carrera en decremento de stock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Libro l WHERE l.id = :id AND l.activo = true")
    Optional<Libro> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT l FROM Libro l WHERE l.activo = true AND " +
           "(cast(:titulo as string) IS NULL OR LOWER(l.titulo) LIKE LOWER(CONCAT('%', cast(:titulo as string), '%'))) AND " +
           "(cast(:categoria as string) IS NULL OR LOWER(l.categoria) = LOWER(cast(:categoria as string)))")
    Page<Libro> buscarLibros(@Param("titulo") String titulo,
                             @Param("categoria") String categoria,
                             Pageable pageable);
}
