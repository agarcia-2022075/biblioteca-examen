# Sistema de Gestión de Biblioteca - API REST

API RESTful modular desarrollada con **Spring Boot 3**, **Spring Security 6**, **Spring Data JPA**, **PostgreSQL** y autenticación **JWT (JSON Web Tokens)**, diseñada bajo estándares de alta concurrencia y tolerancia a pruebas de estrés.

---

## 1. Stack Tecnológico

* **Lenguaje:** Java 21 LTS
* **Framework:** Spring Boot 3.4.3
* **Seguridad:** Spring Security 6 + JJWT 0.12.6
* **Persistencia:** Spring Data JPA / Hibernate 6.6
* **Motor de Base de Datos:** PostgreSQL 18
* **Pool de Conexiones:** HikariCP
* **Construcción y Dependencias:** Maven Wrapper (`mvnw`)
* **Herramientas de Test:** ApacheBench (`ab`), cURL, Postman

---

## 2. Arquitectura y Decisiones de Diseño

### A. Autenticación Stateless y Optimización en Pruebas de Estrés
* La arquitectura de seguridad implementa `SessionCreationPolicy.STATELESS` y deshabilita CSRF.
* El filtro [`JwtAuthenticationFilter`](src/main/java/com/kinal/app/biblioteca/security/JwtAuthenticationFilter.java) valida firmas y extrae claims/roles **completamente en memoria**. Esto evita consultas reiteradas a la base de datos por cada petición en ráfagas concurrentes, protegiendo el pool HikariCP contra saturación.

### B. Control de Concurrencia y Consistencia de Stock
* Para evitar condiciones de carrera (*Race Conditions*) cuando múltiples usuarios solicitan el último ejemplar disponible simultáneamente, se implementa bloqueo pesimista en base de datos:
  ```java
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT l FROM Libro l WHERE l.id = :id AND l.activo = true")
  Optional<Libro> findByIdWithLock(@Param("id") Long id);
  ```

### C. Prevención del Problema $N+1$
* Todas las consultas de préstamos que involucran detalles de usuarios y libros utilizan `JOIN FETCH`:
  ```sql
  SELECT p FROM Prestamo p JOIN FETCH p.libro JOIN FETCH p.usuario WHERE ...
  ```
  Esto reduce las consultas generadas por Hibernate a una única sentencia SQL eficiente.

### D. Integridad y Borrado Lógico
* Para mantener el histórico de auditoría y préstamos contables, la eliminación de libros es lógica mediante la bandera `activo = false`.
* No se utiliza `CascadeType.ALL` ni `CascadeType.REMOVE` en las entidades de `Usuario` y `Libro` hacia `Prestamo`.

---

## 3. Modelo de Datos y Entidades

### Enums
* **`Rol`**: `ADMIN`, `BIBLIOTECARIO`, `LECTOR`
* **`EstadoUsuario`**: `ACTIVO`, `SANCIONADO`
* **`EstadoPrestamo`**: `ACTIVO`, `DEVUELTO`, `ATRASADO`

### Entidades Principales
1. **`Usuario`** (Tabla `usuarios`): `id`, `nombre`, `email` (único, indexado), `password` (BCrypt), `rol`, `estado`.
2. **`Libro`** (Tabla `libros`): `id`, `isbn` (único), `titulo`, `autor`, `categoria`, `stockTotal`, `stockDisponible`, `activo`.
3. **`Prestamo`** (Tabla `prestamos`): `id`, `usuario_id` (FK), `libro_id` (FK), `fechaPrestamo`, `fechaDevolucionEsperada` (14 días), `fechaDevolucionReal`, `estado`.

---

## 4. Reglas de Negocio Implementadas

1. **Plazo de Préstamo:** Cada nuevo préstamo se genera con una fecha de devolución esperada exacta a **14 días** a partir de la fecha de solicitud.
2. **Límite Máximo de Préstamos Activos:** Un usuario puede tener como máximo **3 libros activos** en préstamo. Si intenta solicitar un cuarto ejemplar, se rechaza la solicitud con código `400 Bad Request` (`BusinessRuleException`).
3. **Sanción Automática:** Si un usuario tiene al menos un préstamo activo cuya fecha esperada ha vencido, su estado pasa automáticamente a `SANCIONADO` y se le bloquea la creación de nuevos préstamos.
4. **Validación de Stock:** No es posible solicitar préstamos de libros cuyo stock disponible sea menor o igual a cero.
5. **Retorno de Stock en Devolución:** Al devolver un préstamo mediante `PATCH /api/v1/prestamos/{id}/devolucion`, el stock disponible del libro se incrementa automáticamente en 1 y se registra la fecha real.

---

## 5. Matriz de Endpoints

### A. Autenticación y Registro (`/api/v1/auth`)

| Método | Endpoint | Acceso | Descripción |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Público | Registra usuario con rol por defecto `LECTOR` (`201 Created`). |
| `POST` | `/api/v1/auth/login` | Público | Autentica credenciales y retorna token JWT (`200 OK`). |

### B. Gestión de Libros (`/api/v1/libros`)

| Método | Endpoint | Acceso | Descripción |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/libros` | Autenticado | Catálogo de libros con paginación y filtros opcionales (`titulo`, `categoria`). |
| `GET` | `/api/v1/libros/{id}` | Autenticado | Detalle individual de un libro activo. |
| `POST` | `/api/v1/libros` | `ADMIN` | Registra un nuevo libro (`201 Created`). |
| `PUT` | `/api/v1/libros/{id}` | `ADMIN` | Actualiza datos y existencias de un libro. |
| `DELETE` | `/api/v1/libros/{id}` | `ADMIN` | Borrado lógico del libro (`204 No Content`). |

### C. Gestión de Préstamos (`/api/v1/prestamos`)

| Método | Endpoint | Acceso | Descripción |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/prestamos` | `ADMIN`, `BIBLIOTECARIO` | Registra salida de un libro con validación de stock y reglas de negocio. |
| `PATCH` | `/api/v1/prestamos/{id}/devolucion` | `ADMIN`, `BIBLIOTECARIO` | Registra entrega de ejemplar, restaura stock y cambia estado a `DEVUELTO`. |
| `GET` | `/api/v1/prestamos/mis-prestamos` | `LECTOR` | Historial y préstamos activos del usuario autenticado. |
| `GET` | `/api/v1/prestamos/atrasados` | `ADMIN`, `BIBLIOTECARIO` | Lista préstamos cuya fecha actual superó la fecha esperada. |

---

## 6. Usuarios Semilla Preconfigurados (`data.sql`)

El sistema incluye usuarios predefinidos cargados en el arranque de la base de datos:

| Rol | Correo Electrónico | Contraseña |
| :--- | :--- | :--- |
| **ADMIN** | `admin@biblioteca.com` | `Admin123*` |
| **BIBLIOTECARIO** | `bibliotecario@biblioteca.com` | `Biblio123*` |

---

## 7. Configuración e Instalación

### Requisitos Previos
* Java Development Kit (JDK) 21 instalado y configurado en `PATH`.
* PostgreSQL corriendo en el puerto `5432` con una base de datos creada llamada `biblioteca_db`.

### Configuración de Base de Datos (`src/main/resources/application.properties`)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/biblioteca_db?reWriteBatchedInserts=true&sslmode=disable
spring.datasource.username=postgres
spring.datasource.password=admin
```

### Compilar y Ejecutar

1. **Compilar el proyecto:**
   ```bash
   ./mvnw clean package -DskipTests
   ```

2. **Ejecutar la aplicación:**
   ```bash
   java -jar target/biblioteca-0.0.1-SNAPSHOT.jar
   ```
   *(Si el puerto 8080 está ocupado en tu entorno, puedes ejecutarlo especificando otro puerto: `java -Dserver.port=8081 -jar target/biblioteca-0.0.1-SNAPSHOT.jar`)*.

---

## 8. Pruebas y Validación

### A. Script de Pruebas de Estrés y Funcionales (`test-api1.sh`)
El proyecto incluye el script de evaluación con pruebas funcionales y de concurrencia:
```bash
bash test-api1.sh
```

El script valida:
1. Registro de lector.
2. Login de administrador y obtención de JWT.
3. Creación de libros restringida a rol `ADMIN`.
4. Consulta paginada del catálogo.
5. Verificación de control de acceso: Intento de creación con rol `LECTOR` rechazado con **`403 Forbidden`**.
6. **Prueba de concurrencia:** 500 peticiones en 50 hilos paralelos mediante ApacheBench (`ab`) o 100 peticiones en 10 hilos (`cURL + xargs`) obteniendo respuesta `200 OK` en el 100% de las peticiones sin saturar el pool de conexiones.

### B. Colección de Postman
En la raíz del proyecto se encuentra el archivo listo para importar en Postman:
* [`Biblioteca_API_Evaluacion_1.postman_collection.json`](Biblioteca_API_Evaluacion_1.postman_collection.json)

Incluye tests automatizados en JavaScript que almacenan los tokens Bearer (`adminToken`, `lectorToken`) y los identificadores creados (`libroId`, `prestamoId`) para ejecutar el flujo completo en cadena mediante el **Collection Runner**.
