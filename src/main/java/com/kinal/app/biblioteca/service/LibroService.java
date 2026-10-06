package com.kinal.app.biblioteca.service;

import com.kinal.app.biblioteca.dto.libro.LibroRequest;
import com.kinal.app.biblioteca.dto.libro.LibroResponse;
import com.kinal.app.biblioteca.entity.Libro;
import com.kinal.app.biblioteca.exception.BusinessRuleException;
import com.kinal.app.biblioteca.exception.ResourceNotFoundException;
import com.kinal.app.biblioteca.repository.LibroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibroService {

    private final LibroRepository libroRepository;

    @Transactional
    public LibroResponse crearLibro(LibroRequest request) {
        if (libroRepository.existsByIsbn(request.getIsbn())) {
            throw new BusinessRuleException("El ISBN ya se encuentra registrado: " + request.getIsbn());
        }

        Libro libro = Libro.builder()
                .isbn(request.getIsbn().trim())
                .titulo(request.getTitulo().trim())
                .autor(request.getAutor().trim())
                .categoria(request.getCategoria().trim())
                .stockTotal(request.getStockTotal())
                .stockDisponible(request.getStockDisponible())
                .activo(true)
                .build();

        Libro guardado = libroRepository.save(libro);
        return mapToResponse(guardado);
    }

    @Transactional(readOnly = true)
    public LibroResponse obtenerPorId(Long id) {
        Libro libro = libroRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con ID: " + id));
        return mapToResponse(libro);
    }

    @Transactional(readOnly = true)
    public Page<LibroResponse> listarLibros(String titulo, String categoria, Pageable pageable) {
        String tituloParam = (titulo != null && !titulo.isBlank()) ? titulo.trim() : null;
        String categoriaParam = (categoria != null && !categoria.isBlank()) ? categoria.trim() : null;

        return libroRepository.buscarLibros(tituloParam, categoriaParam, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public LibroResponse actualizarLibro(Long id, LibroRequest request) {
        Libro libro = libroRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con ID: " + id));

        // Si cambia de ISBN, validar que no este en uso por otro libro
        if (!libro.getIsbn().equalsIgnoreCase(request.getIsbn().trim())
                && libroRepository.existsByIsbn(request.getIsbn().trim())) {
            throw new BusinessRuleException("El ISBN ya pertenece a otro libro: " + request.getIsbn());
        }

        libro.setIsbn(request.getIsbn().trim());
        libro.setTitulo(request.getTitulo().trim());
        libro.setAutor(request.getAutor().trim());
        libro.setCategoria(request.getCategoria().trim());
        libro.setStockTotal(request.getStockTotal());
        libro.setStockDisponible(request.getStockDisponible());

        Libro actualizado = libroRepository.save(libro);
        return mapToResponse(actualizado);
    }

    @Transactional
    public void eliminarLibro(Long id) {
        Libro libro = libroRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con ID: " + id));

        // Borrado logico para preservar la integridad referencial con prestamos historicos
        libro.setActivo(false);
        libroRepository.save(libro);
    }

    public LibroResponse mapToResponse(Libro libro) {
        return LibroResponse.builder()
                .id(libro.getId())
                .isbn(libro.getIsbn())
                .titulo(libro.getTitulo())
                .autor(libro.getAutor())
                .categoria(libro.getCategoria())
                .stockTotal(libro.getStockTotal())
                .stockDisponible(libro.getStockDisponible())
                .activo(libro.getActivo())
                .build();
    }
}
