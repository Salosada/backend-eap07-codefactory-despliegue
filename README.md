# AppStripe - Motor de Pagos Embebidos (Caso 8)

Backend Spring Boot para una plataforma de pagos B2B estilo Stripe, construido como monolito modular por dominios.

## Estado actual (real del repo)

Implementado y funcional:

- HU001: Registro de comercio por API.
- HU002: Generacion de credenciales API por comercio.
- HU003: Creacion y consulta de transacciones.
- HU004: Estado inicial de transaccion en `CREATED`.
- Contrato de errores estandarizado (`errorCode`, `message`, `details`, `traceId`).
- Persistencia en MySQL por JPA (`merchants`, `api_credentials`, `transactions`, `users`).

Pendiente:

- Integrar validacion obligatoria de API credentials en endpoints de transacciones.
- Endpoints de MFA completos orientados a flujo productivo.
- Auditoria persistente (hoy hay partes en modo stub/log).

## APIs expuestas

| Metodo | Ruta | Descripcion |
|---|---|---|
| POST | `/api/v1/merchants` | Registra comercio (MVP actual lo deja en `VERIFIED`) |
| POST | `/api/v1/credentials/generate` | Genera credenciales para un comercio |
| POST | `/api/v1/transactions` | Crea transaccion en estado `CREATED` |
| GET | `/api/v1/transactions/{id}` | Consulta transaccion por id |
| POST | `/2fa/verify` | Verifica codigo 2FA (flujo tecnico actual) |

## Flujo demo end-to-end

1. Registrar comercio: `POST /api/v1/merchants`
2. Generar credenciales: `POST /api/v1/credentials/generate`
3. Crear transaccion: `POST /api/v1/transactions`
4. Consultar transaccion: `GET /api/v1/transactions/{id}`

## Arquitectura y documentacion

- Contexto C4: [docs/architecture/c4-context.md](docs/architecture/c4-context.md)
- Contenedores C4: [docs/architecture/c4-containers.md](docs/architecture/c4-containers.md)
- Vista Sprint 1 (objetivo + estado implementado): [docs/architecture/sprint1-package-and-components.md](docs/architecture/sprint1-package-and-components.md)

## Stack tecnologico

- Java 17
- Spring Boot 3.5.x
- Spring Web
- Spring Data JPA
- Bean Validation
- MySQL 8 (Docker Compose)
- Maven Wrapper

## Como ejecutar el proyecto

### Prerrequisitos

- Docker Desktop
- JDK 17+

### 1) Levantar MySQL

```bash
docker compose up -d mysql-db
```

Notas:

- El contenedor MySQL se publica en el puerto host `3406` para evitar conflictos locales.
- La app conecta por `jdbc:mysql://127.0.0.1:3406/appstripe_db`.

### 2) Levantar la API

Windows:

```bat
mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

### 3) Ejecutar pruebas

Windows:

```bat
mvnw.cmd test
```

Linux/macOS:

```bash
./mvnw test
```

## Estructura

```text
src/
	main/
		java/com/codefactory/appstripe/
			identity/
			security/
			transactions/
			common/
		resources/
	test/
		java/com/codefactory/appstripe/
docs/
	architecture/
```

## Despliegue (TiDB + Render + Vercel)

### 1) TiDB Cloud

1. Crear cluster y base de datos `appstripe`.
2. Copiar la cadena JDBC del panel (puerto **4000**, SSL obligatorio).
3. Ejemplo de URL:
   `jdbc:mysql://HOST:4000/appstripe?useSSL=true&requireSSL=true&serverTimezone=UTC`

### 2) Render (backend)

1. Conectar el repo `backend-eap07-codefactory` en [Render](https://render.com).
2. Usar **Docker** con el `Dockerfile` del repo (o importar `render.yaml` como Blueprint).
3. Variables de entorno obligatorias:

| Variable | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DOCKER_COMPOSE_ENABLED` | `false` |
| `SPRING_DATASOURCE_URL` | JDBC de TiDB |
| `SPRING_DATASOURCE_USERNAME` | usuario TiDB |
| `SPRING_DATASOURCE_PASSWORD` | contraseña TiDB |
| `JWT_SECRET` | string largo aleatorio |
| `CORS_ALLOWED_ORIGINS` | `https://tu-app.vercel.app,https://*.vercel.app` |

4. Health check: `/actuator/health`
5. Tras el primer deploy se crea el admin seed: `admin@paycore.com` / `admin123`

Referencia completa de variables: [.env.example](.env.example)

### 3) Vercel (frontend)

1. Conectar el repo `frontend-appstripe`.
2. Variable **obligatoria antes del build**:

| Variable | Valor |
|---|---|
| `BACKEND_URL` | `https://appstripe-api.onrender.com` (URL de Render sin barra final) |

3. El frontend llama al backend vía proxy interno `/backend/*` (ver `next.config.ts`). No uses `NEXT_PUBLIC_*` para la URL del API.
4. Si cambias `BACKEND_URL`, redeploy en Vercel para regenerar los rewrites.

Referencia: [frontend `.env.local.example`](../frontend-appstripe/.env.local.example)

### Orden recomendado

1. TiDB → 2. Render (verificar `/actuator/health`) → 3. Vercel con `BACKEND_URL` → 4. Actualizar `CORS_ALLOWED_ORIGINS` en Render con la URL real de Vercel.

## Equipo

Proyecto academico de Fabrica Escuela CodeFactory (EAP07), orientado a buenas practicas de backend, arquitectura y seguridad aplicada a APIs de pagos.