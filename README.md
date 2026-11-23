# Pastelería 1000 Sabores – API REST

Backend del proyecto **Pastelería 1000 Sabores** (Duoc UC, DSY1104 Desarrollo Fullstack II, Evaluación 3).
Reemplaza el `localStorage` del front (React + Vite) por una API REST en Spring Boot con base de datos MySQL.

- Front: <https://github.com/AlanisVerdugo/Pasteleria-1000-sabores>
- Rutas versionadas: `/api/v1/...`
- Documentación interactiva con Swagger
- Seguridad con Spring Security y JWT (roles ADMIN, VENDEDOR y CLIENTE)

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

En Windows: `docker compose up --build`. La API queda en el mismo puerto **8089**; MySQL usa el host **`db`** dentro de la red de Docker (no `localhost`).

Variables opcionales (ver `.env.example`; el `.env` no se sube al repo):

| Variable | Uso |
| :--- | :--- |
| `DB_PASSWORD` | Contraseña root de MySQL (por defecto `root`) |
| `JWT_SECRET` | Clave JWT (en desarrollo hay valor por defecto en compose) |
| `CORS_ORIGENES` | Orígenes del front, separados por coma (por defecto `http://localhost:5173`) |

Desde cero (borra datos locales del volumen): `docker compose down -v` y luego `docker compose up --build`. La primera compilación dentro de la imagen puede tardar varios minutos.

## Problemas al arrancar

Si `spring-boot:run` termina con `BUILD FAILURE` pero la compilación fue bien, revisa el final del log:
Spring suele decir qué falló (puerto, base de datos, etc.).

### Puerto 8089 ya en uso

Mensaje típico:

```text
Web server failed to start. Port 8089 was already in use.
```

Suele pasar si la API sigue corriendo en otra terminal, en el IDE (Run/Debug) o porque no se cerró
la instancia anterior. Solo puede haber **una** API escuchando en el puerto configurado (`server.port=8089`).

**Qué hacer:**

1. Cierra la ejecución anterior (detén el Run en el IDE o pulsa `Ctrl+C` en la terminal donde corre Maven).
2. Si no sabes dónde quedó, mira qué proceso usa el puerto y ciérralo.

En Windows (PowerShell):

```powershell
Get-NetTCPConnection -LocalPort 8089 -State Listen |
  ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

En Linux o macOS:

```bash
lsof -i :8089
kill <PID>
```

3. O levanta la API en otro puerto sin cambiar archivos:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8090
```

En Windows:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8090"
```

Si cambias el puerto, actualiza la URL del front y la de Swagger (`/swagger-ui/index.html`).

## Swagger

Al arrancar la API, al final del log verás un bloque con las URLs (clase `InicioUrlsLog`).

- Interfaz: <http://localhost:8089/swagger-ui/index.html>
- JSON de la especificación OpenAPI: <http://localhost:8089/v3/api-docs>

**Swagger UI** es la página en el navegador donde pruebas los endpoints (GET, POST, etc.) sin Postman.

**OpenAPI** es el estándar que describe tu API en un archivo JSON (rutas, parámetros, respuestas).
SpringDoc lo genera solo a partir de tus controllers; Swagger UI lo lee para dibujar esa interfaz.
Si abres `/v3/api-docs` ves el mismo contenido en bruto (útil para herramientas o para el front).

El botón **Authorize** ya está configurado para pegar un token JWT (esquema `bearerAuth`).

## Pruebas

```bash
./mvnw test
```

Usan una base H2 en memoria, no necesitan MySQL (`src/test/resources/application.properties`).

En Windows: `.\mvnw.cmd test`. La suite actual en `main` incluye pruebas por módulo (service, controller e integración con tokens reales donde corresponde): auth, categorías, productos, usuarios, boletas, blog, mensajes y `DescuentosTest`.

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
├── security     JwtService, JwtFilter, SecurityConfig (CORS, reglas por rol), 401/403 en JSON
├── util         Descuentos (reglas de la compra, Fase D)
└── service      Lógica de negocio
```

Cada módulo sigue el camino **Controller → Service → Repository** con DTOs de entrada y salida. El módulo de **Categorías** es la referencia para copiar la forma; Blog y Mensajes (Fase E) siguen ese patrón.

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

**Backend en `main` (EV3):** módulos listos en Swagger bajo `/api/v1`:

| Módulo | Endpoints | Notas |
| :--- | :--- | :--- |
| Auth | login, registro, refresh, me, perfil | JWT access + refresh, roles |
| Categorías | CRUD | Referencia de capas |
| Productos | 9 rutas | Catálogo público; críticos solo ADMIN |
| Usuarios | CRUD | Solo ADMIN; sin `password` en JSON |
| Boletas | compra, listados, estado | Descuentos y stock en el servidor |
| Blog | GET público; POST/PUT/DELETE ADMIN | Fase E (Alanis) |
| Mensajes | POST público; GET/DELETE ADMIN | Fase E (Benjamin); `usuarioId` desde el token |

`.\mvnw.cmd test` en verde (~199 pruebas). Detalle para estudiar: `docs/Guia_estudio_proyecto.md`.

**Pendiente en el plan EV3 (Alan / equipo):**

- **Fase G (rama `p1/docker`):** CORS y JWT por variables; `docker compose up --build` (probar con front tras levantar contenedores).
- **Front:** integración en `pasteleria_sabores` (rama `p1/integracion-api`); **F7** hecho contra API local.
- **Entregables I/J:** documentos, tabla Excel de APIs, demo ensayada.
