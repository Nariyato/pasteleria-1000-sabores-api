# Pastelería 1000 Sabores – API REST

Backend del proyecto **Pastelería 1000 Sabores** (Duoc UC, DSY1104 Desarrollo Fullstack II, Evaluación 3).
Reemplaza el `localStorage` del front (React + Vite) por una API REST en Spring Boot con base de datos MySQL.

- Front: <https://github.com/AlanisVerdugo/Pasteleria-1000-sabores>
- Rutas versionadas: `/api/v1/...`
- Documentación interactiva con Swagger
- Seguridad con Spring Security y JWT (roles ADMIN, VENDEDOR y CLIENTE): **pendiente de implementar**, ver "Estado del proyecto"

## Requisitos

- Java 21
- Docker y Docker Compose (para MySQL). Si no tienes Docker, usa el perfil `h2` de más abajo.
- No hace falta instalar Maven: el proyecto trae el wrapper (`mvnw`).

## Cómo levantar la base de datos (MySQL con Docker)

```bash
docker compose up -d db
```

Crea una base `pasteleria` en `localhost:3306` con usuario `root` y contraseña `root`.
Los datos quedan guardados en un volumen de Docker, así que no se pierden al apagarla.

## Cómo correr la API

```bash
./mvnw spring-boot:run
```

En Windows: `mvnw.cmd spring-boot:run`. La API queda en <http://localhost:8089>.

Al primer arranque se crean las tablas solas (`ddl-auto=update`) y el `DataInitializer` carga los datos
iniciales del front (categorías, productos, usuarios, dos boletas de ejemplo y el blog), solo si las tablas están vacías.

Si tu MySQL tiene otro usuario o contraseña (por ejemplo XAMPP, que no usa contraseña), cámbialo con variables de entorno:
`DB_URL`, `DB_USER` y `DB_PASSWORD`.

### Sin MySQL (perfil h2)

Levanta la API con una base en memoria. Los datos se borran al apagarla:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

### Todo con Docker (base de datos y API)

```bash
docker compose up --build
```

## Swagger

- Interfaz: <http://localhost:8089/swagger-ui/index.html>
- JSON de la especificación OpenAPI: <http://localhost:8089/v3/api-docs>

El botón **Authorize** ya está configurado para pegar un token JWT (esquema `bearerAuth`).

## Pruebas

```bash
./mvnw test
```

Usan una base H2 en memoria, no necesitan MySQL. Incluyen:

- `CategoriaServiceTest`: pruebas unitarias del service con Mockito.
- `CategoriaControllerTest`: pruebas del controller con MockMvc (`@WebMvcTest`).
- `CategoriaIntegracionTest`: arranca toda la app con H2 y prueba la API de punta a punta.

## Usuarios de prueba

| Rol | Correo | Contraseña |
|---|---|---|
| ADMIN | admin@duoc.cl | admin123 |
| VENDEDOR | vendedor@pasteleria.cl | vendedor123 |
| CLIENTE | rosa@gmail.com | 1234 |
| CLIENTE (código FELICES50) | pedro@gmail.com | 1234 |
| CLIENTE (Duoc, cumple hoy) | camila@duocuc.cl | 1234 |

Las contraseñas se guardan encriptadas con BCrypt.

## Estructura de paquetes

```
cl.duoc.pasteleria
├── config       OpenApiConfig (Swagger) y DataInitializer (datos iniciales)
├── controller   Reciben las peticiones HTTP y responden. No tienen lógica de negocio
├── dto          Objetos de entrada (Request) y de salida (Response) de la API
├── exception    Excepciones propias y GlobalExceptionHandler (formato único de errores)
├── model        Entidades JPA (las tablas)
├── repository   Interfaces de acceso a datos (Spring Data JPA)
├── security     SecurityConfig (CORS, sesión sin estado, BCrypt). Aquí irán JwtService y JwtFilter
└── service      Lógica de negocio
```

Cada módulo sigue el camino **Controller → Service → Repository**. El módulo de **Categorías** está completo
y es la referencia: para crear Productos, Boletas, etc. se copia su forma.

## Modelo de datos

`USUARIO → BOLETA → DETALLE_BOLETA ← PRODUCTO ← CATEGORIA`, más las tablas `BLOG_POST` y `MENSAJE`
para las páginas de blog y contacto del front.

## Errores

Todos los errores salen con el mismo formato:

```json
{
  "fecha": "2026-11-16T01:06:38",
  "estado": 400,
  "error": "Bad Request",
  "mensaje": "Hay campos con datos no válidos",
  "ruta": "/api/v1/categorias",
  "errores": { "nombre": "El nombre es obligatorio" }
}
```

El campo `errores` solo aparece en las validaciones (400). Los demás códigos son 404 (no existe), 409 (regla de negocio) y 500.

## Estado del proyecto

Hecho:

- Estructura del proyecto, entidades, repositorios y datos iniciales.
- Módulo de Categorías completo (CRUD, validaciones, Swagger y pruebas).
- Manejo global de errores, CORS para `http://localhost:5173` y Swagger.

Pendiente:

- JWT: login, registro, `me` y renovación del token (`refresh`).
- Reglas de seguridad por rol en `SecurityConfig` (hoy todo está en `permitAll`).
- CRUD de productos, boletas, usuarios, blog y mensajes.
- Lógica de negocio de las compras (descuentos, descuento de stock y totales).
- Pruebas de esos módulos y conexión del front.
