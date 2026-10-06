-- ==============================================================
-- Datos Iniciales de la Biblioteca (Idempotente con ON CONFLICT)
-- ==============================================================

-- Usuario Administrador requerido por test-api1.sh (Pass: Admin123*)
-- Usuario Bibliotecario para cumplir rúbrica de al menos 2 roles
INSERT INTO usuarios (nombre, email, password, estado, rol)
VALUES 
('Administrador Principal', 'admin@biblioteca.com', '$2a$10$qfoal09QvevWctHAiO90zuLzZWbGaDnQxBpjOffh7C0DpzSIFEjDy', 'ACTIVO', 'ADMIN'),
('Bibliotecario General', 'bibliotecario@biblioteca.com', '$2a$10$qfoal09QvevWctHAiO90zuLzZWbGaDnQxBpjOffh7C0DpzSIFEjDy', 'ACTIVO', 'BIBLIOTECARIO')
ON CONFLICT (email) DO NOTHING;

-- Libros de ejemplo iniciales para catálogo público
INSERT INTO libros (isbn, titulo, autor, categoria, stock_total, stock_disponible, activo)
VALUES 
('978-0132350884', 'Clean Code', 'Robert C. Martin', 'Ingenieria de Software', 5, 5, true),
('978-0201633610', 'Design Patterns', 'Erich Gamma et al.', 'Arquitectura', 3, 3, true)
ON CONFLICT (isbn) DO NOTHING;
