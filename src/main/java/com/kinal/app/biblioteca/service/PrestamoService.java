package com.kinal.app.biblioteca.service;

import com.kinal.app.biblioteca.dto.prestamo.PrestamoRequest;
import com.kinal.app.biblioteca.dto.prestamo.PrestamoResponse;
import com.kinal.app.biblioteca.entity.Libro;
import com.kinal.app.biblioteca.entity.Prestamo;
import com.kinal.app.biblioteca.entity.Usuario;
import com.kinal.app.biblioteca.entity.enums.EstadoPrestamo;
import com.kinal.app.biblioteca.entity.enums.EstadoUsuario;
import com.kinal.app.biblioteca.exception.BusinessRuleException;
import com.kinal.app.biblioteca.exception.ResourceNotFoundException;
import com.kinal.app.biblioteca.repository.LibroRepository;
import com.kinal.app.biblioteca.repository.PrestamoRepository;
import com.kinal.app.biblioteca.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrestamoService {

    private final PrestamoRepository prestamoRepository;
    private final LibroRepository libroRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public PrestamoResponse registrarPrestamo(PrestamoRequest request, String emailAutenticado) {
        LocalDate hoy = LocalDate.now();

        // 1. Determinar el usuario del prestamo
        Usuario usuario;
        if (request.getUsuarioId() != null) {
            usuario = usuarioRepository.findById(request.getUsuarioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + request.getUsuarioId()));
        } else {
            usuario = usuarioRepository.findByEmail(emailAutenticado)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + emailAutenticado));
        }

        // 2. Regla de negocio: Verificar si el usuario ya esta SANCIONADO
        if (usuario.getEstado() == EstadoUsuario.SANCIONADO) {
            throw new BusinessRuleException("El usuario se encuentra en estado SANCIONADO y no puede realizar nuevos prestamos");
        }

        // 3. Regla de negocio: Sancion automatica si tiene prestamos con fecha esperada vencida
        boolean tieneAtrasos = prestamoRepository.existsByUsuarioIdAndEstadoAndFechaDevolucionEsperadaBefore(
                usuario.getId(), EstadoPrestamo.ACTIVO, hoy
        );
        if (tieneAtrasos) {
            usuario.setEstado(EstadoUsuario.SANCIONADO);
            usuarioRepository.save(usuario);
            throw new BusinessRuleException("El usuario tiene prestamos vencidos y ha pasado a estado SANCIONADO");
        }

        // 4. Regla de negocio: Maximo 3 prestamos activos
        long prestamosActivos = prestamoRepository.countByUsuarioIdAndEstado(usuario.getId(), EstadoPrestamo.ACTIVO);
        if (prestamosActivos >= 3) {
            throw new BusinessRuleException("El usuario ya cuenta con 3 prestamos activos (limite maximo alcanzado)");
        }

        // 5. Regla de negocio y Concurrencia: Bloqueo pesimista para decrementar stock de forma segura
        Libro libro = libroRepository.findByIdWithLock(request.getLibroId())
                .orElseThrow(() -> new ResourceNotFoundException("Libro no disponible o no encontrado con ID: " + request.getLibroId()));

        if (libro.getStockDisponible() <= 0) {
            throw new BusinessRuleException("No hay ejemplares disponibles para el libro: " + libro.getTitulo());
        }

        // Decrementar stock
        libro.setStockDisponible(libro.getStockDisponible() - 1);
        libroRepository.save(libro);

        // 6. Crear el prestamo con 14 dias de plazo
        Prestamo prestamo = Prestamo.builder()
                .usuario(usuario)
                .libro(libro)
                .fechaPrestamo(hoy)
                .fechaDevolucionEsperada(hoy.plusDays(14))
                .estado(EstadoPrestamo.ACTIVO)
                .build();

        Prestamo guardado = prestamoRepository.save(prestamo);
        return mapToResponse(guardado);
    }

    @Transactional
    public PrestamoResponse registrarDevolucion(Long prestamoId) {
        Prestamo prestamo = prestamoRepository.findByIdWithDetalle(prestamoId)
                .orElseThrow(() -> new ResourceNotFoundException("Prestamo no encontrado con ID: " + prestamoId));

        if (prestamo.getEstado() == EstadoPrestamo.DEVUELTO) {
            throw new BusinessRuleException("El prestamo con ID " + prestamoId + " ya fue devuelto previamente");
        }

        LocalDate hoy = LocalDate.now();
        prestamo.setEstado(EstadoPrestamo.DEVUELTO);
        prestamo.setFechaDevolucionReal(hoy);

        // Devolver ejemplar al stock
        Libro libro = prestamo.getLibro();
        libro.setStockDisponible(libro.getStockDisponible() + 1);
        libroRepository.save(libro);

        // Si el usuario estaba sancionado y ya no tiene otros prestamos atrasados, se reactiva
        Usuario usuario = prestamo.getUsuario();
        if (usuario.getEstado() == EstadoUsuario.SANCIONADO) {
            boolean aunTieneAtrasos = prestamoRepository.existsByUsuarioIdAndEstadoAndFechaDevolucionEsperadaBefore(
                    usuario.getId(), EstadoPrestamo.ACTIVO, hoy
            );
            if (!aunTieneAtrasos) {
                usuario.setEstado(EstadoUsuario.ACTIVO);
                usuarioRepository.save(usuario);
            }
        }

        Prestamo actualizado = prestamoRepository.save(prestamo);
        return mapToResponse(actualizado);
    }

    @Transactional(readOnly = true)
    public List<PrestamoResponse> obtenerMisPrestamos(String emailAutenticado) {
        Usuario usuario = usuarioRepository.findByEmail(emailAutenticado)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + emailAutenticado));

        return prestamoRepository.findByUsuarioIdWithDetalle(usuario.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PrestamoResponse> obtenerAtrasados() {
        return prestamoRepository.findAtrasadosConDetalle(EstadoPrestamo.ACTIVO, LocalDate.now())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PrestamoResponse mapToResponse(Prestamo prestamo) {
        return PrestamoResponse.builder()
                .id(prestamo.getId())
                .usuarioId(prestamo.getUsuario().getId())
                .usuarioNombre(prestamo.getUsuario().getNombre())
                .usuarioEmail(prestamo.getUsuario().getEmail())
                .libroId(prestamo.getLibro().getId())
                .libroTitulo(prestamo.getLibro().getTitulo())
                .libroIsbn(prestamo.getLibro().getIsbn())
                .fechaPrestamo(prestamo.getFechaPrestamo())
                .fechaDevolucionEsperada(prestamo.getFechaDevolucionEsperada())
                .fechaDevolucionReal(prestamo.getFechaDevolucionReal())
                .estado(prestamo.getEstado())
                .build();
    }
}
