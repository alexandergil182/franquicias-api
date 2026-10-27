# API de Franquicias

API REST reactiva para administrar **franquicias**, sus **sucursales** y los **productos** (con stock) de cada sucursal. Es la solución a la prueba técnica de Backend con Java.

- **Java 17**, **Spring Boot 3.5**, **Spring WebFlux** (programación reactiva con `Mono`/`Flux`)
- **MongoDB** como persistencia (Spring Data Reactive MongoDB)
- Arquitectura **hexagonal** (dominio, aplicación, infraestructura)
- **Docker** (Dockerfile multi-etapa + docker-compose) y plantilla de **Terraform** para la base de datos

```
Franquicia (nombre) ──< Sucursal (nombre) ──< Producto (nombre, stock)
```

> **Estado del despliegue:** la API se probó en local (tests y peticiones reales contra MongoDB). La plantilla de Terraform y los pasos de AWS son **un ejemplo para el siguiente paso**: no están desplegados.

## Contenido

1. [Arquitectura](#arquitectura)
2. [Requisitos](#requisitos)
3. [Cómo correrlo con Docker Compose](#cómo-correrlo-con-docker-compose)
4. [Cómo correrlo en local sin Docker](#cómo-correrlo-en-local-sin-docker)
5. [Endpoints y ejemplos curl](#endpoints-y-ejemplos-curl)
6. [Formato de errores](#formato-de-errores)
7. [Tests](#tests)
8. [Infraestructura como código (Terraform)](#infraestructura-como-código-terraform)
9. [Despliegue en AWS (siguiente paso)](#despliegue-en-aws-siguiente-paso)
10. [Decisiones de diseño](#decisiones-de-diseño)
11. [Flujo de trabajo con git](#flujo-de-trabajo-con-git)

## Arquitectura

Arquitectura hexagonal (puertos y adaptadores): el negocio no depende de Spring ni de MongoDB; las dependencias apuntan siempre hacia el dominio.

```
src/main/java/com/franquicias
├── FranquiciasApplication.java          punto de entrada
├── domain/                              el negocio, sin frameworks
│   ├── model/                           Franquicia, Sucursal, Producto, ProductoDeSucursal (records inmutables con las reglas)
│   ├── exception/                       errores de negocio (no encontrado, conflicto, dato inválido)
│   └── port/
│       ├── in/FranquiciaUseCase         puerto de entrada: qué se puede hacer
│       └── out/FranquiciaRepositoryPort puerto de salida: qué se necesita de la persistencia
├── application/
│   └── FranquiciaService                casos de uso (implementa el puerto de entrada)
└── infrastructure/                      adaptadores a tecnologías concretas
    ├── persistence/                     adaptador MongoDB: documento, repositorio Spring Data y mapeos
    └── web/                             adaptador REST: controlador, DTOs, manejo global de errores
```

Flujo de una petición: `Controller` → `FranquiciaUseCase` (implementado por `FranquiciaService`) → `FranquiciaRepositoryPort` (implementado por `FranquiciaMongoAdapter`) → MongoDB.

## Requisitos

| Para... | Necesitas |
|---|---|
| Docker Compose | Docker y Docker Compose v2 |
| Correr en local | JDK 17, Maven 3.9+ y un MongoDB 6/7 accesible (o Docker solo para la base) |
| Correr los tests | JDK 17 y Maven (los tests normales no necesitan base de datos) |

## Cómo correrlo con Docker Compose

```bash
docker compose up --build
```

Levanta MongoDB y la API (`http://localhost:8080`). Para apagar: `docker compose down` (agrega `-v` para borrar también los datos).

- Salud: <http://localhost:8080/actuator/health>
- Documentación interactiva (Swagger UI): <http://localhost:8080/swagger-ui.html>

## Cómo correrlo en local sin Docker

1. Ten un MongoDB escuchando en `localhost:27017`. Si tienes Docker, solo para la base:
   ```bash
   docker run -d --name franquicias-mongo -p 27017:27017 mongo:7
   ```
2. Arranca la API:
   ```bash
   mvn spring-boot:run
   ```
   o genera y ejecuta el jar:
   ```bash
   mvn clean package
   java -jar target/franquicias-api-1.0.0.jar
   ```

Variables de entorno opcionales:

| Variable | Por defecto | Para qué |
|---|---|---|
| `MONGODB_URI` | `mongodb://localhost:27017/franquicias` | Conexión a MongoDB |
| `PORT` | `8080` | Puerto HTTP |

## Endpoints y ejemplos curl

Base: `/api/franquicias`. Los `id` de franquicia, sucursal y producto son UUID que devuelve la API al crearlos. Los ejemplos usan `jq` para guardar los ids (opcional).

| # | Método y ruta | Qué hace | Respuesta |
|---|---|---|---|
| 1 | `POST /api/franquicias` | Agregar franquicia | 201 |
| 2 | `POST /api/franquicias/{fid}/sucursales` | Agregar sucursal a una franquicia | 201 |
| 3 | `POST /api/franquicias/{fid}/sucursales/{sid}/productos` | Agregar producto a una sucursal | 201 |
| 4 | `DELETE /api/franquicias/{fid}/sucursales/{sid}/productos/{pid}` | Eliminar producto de una sucursal | 204 |
| 5 | `PATCH /api/franquicias/{fid}/sucursales/{sid}/productos/{pid}/stock` | Modificar el stock de un producto | 200 |
| 6 | `GET /api/franquicias/{id}/productos-mas-stock` | Producto con más stock **por sucursal** de la franquicia | 200 |
| 7 | `PUT /api/franquicias/{id}/nombre` | Renombrar franquicia (extra) | 200 |
| 8 | `PUT /api/franquicias/{fid}/sucursales/{sid}/nombre` | Renombrar sucursal (extra) | 200 |
| 9 | `PUT /api/franquicias/{fid}/sucursales/{sid}/productos/{pid}/nombre` | Renombrar producto (extra) | 200 |
| 10 | `GET /api/franquicias` y `GET /api/franquicias/{id}` | Consultar franquicias (para comprobar los cambios) | 200 |

### Recorrido completo

```bash
URL=http://localhost:8080/api/franquicias
JSON='Content-Type: application/json'

# 1. Crear franquicia
FID=$(curl -s -X POST $URL -H "$JSON" -d '{"nombre":"Frutas SA"}' | jq -r .id)

# 2. Agregar sucursales
S1=$(curl -s -X POST $URL/$FID/sucursales -H "$JSON" -d '{"nombre":"Centro"}' | jq -r .id)
S2=$(curl -s -X POST $URL/$FID/sucursales -H "$JSON" -d '{"nombre":"Norte"}'  | jq -r .id)

# 3. Agregar productos (nombre + stock)
P1=$(curl -s -X POST $URL/$FID/sucursales/$S1/productos -H "$JSON" -d '{"nombre":"Manzana","stock":10}' | jq -r .id)
curl -s -X POST $URL/$FID/sucursales/$S1/productos -H "$JSON" -d '{"nombre":"Pera","stock":25}'
curl -s -X POST $URL/$FID/sucursales/$S2/productos -H "$JSON" -d '{"nombre":"Kiwi","stock":40}'

# 6. Producto con más stock por sucursal (lista con la sucursal de cada producto)
curl -s $URL/$FID/productos-mas-stock | jq
# [
#   {"sucursalId":"...","sucursalNombre":"Centro","producto":{"id":"...","nombre":"Pera","stock":25}},
#   {"sucursalId":"...","sucursalNombre":"Norte","producto":{"id":"...","nombre":"Kiwi","stock":40}}
# ]

# 5. Modificar el stock de la manzana (ahora es la de mayor stock en Centro)
curl -s -X PATCH $URL/$FID/sucursales/$S1/productos/$P1/stock -H "$JSON" -d '{"stock":100}'

# 7, 8, 9. Renombrar franquicia, sucursal y producto
curl -s -X PUT $URL/$FID/nombre                          -H "$JSON" -d '{"nombre":"Frutas y Verduras SA"}'
curl -s -X PUT $URL/$FID/sucursales/$S1/nombre           -H "$JSON" -d '{"nombre":"Centro Histórico"}'
curl -s -X PUT $URL/$FID/sucursales/$S1/productos/$P1/nombre -H "$JSON" -d '{"nombre":"Manzana roja"}'

# 4. Eliminar un producto (204 sin cuerpo)
curl -s -i -X DELETE $URL/$FID/sucursales/$S1/productos/$P1

# Consultar la franquicia completa
curl -s $URL/$FID | jq
```

## Formato de errores

Todos los errores usan el mismo JSON (nunca se expone un stack trace):

```json
{
  "fecha": "2026-10-27T16:30:15.923Z",
  "estado": 404,
  "error": "Not Found",
  "mensaje": "No existe la franquicia con id nope",
  "ruta": "/api/franquicias/nope/productos-mas-stock",
  "detalles": []
}
```

| Código | Cuándo |
|---|---|
| 400 | Cuerpo inválido: nombre vacío, stock negativo o no numérico, JSON mal formado. `detalles` lista cada campo con su problema |
| 404 | La franquicia, la sucursal o el producto no existen |
| 409 | Nombre repetido (franquicia, sucursal dentro de su franquicia o producto dentro de su sucursal, sin distinguir mayúsculas) o dos peticiones modificaron la misma franquicia a la vez |
| 500 | Error inesperado (el detalle solo queda en el log) |

## Tests

```bash
mvn test
```

| Test | Qué comprueba |
|---|---|
| `FranquiciaServiceTest` | Casos de uso con el repositorio simulado (Mockito) y `StepVerifier`: agregar sucursal, eliminar producto, modificar stock, producto con más stock por sucursal, franquicia inexistente (404), conflictos y validaciones |
| `FranquiciaTest` | Reglas del modelo de dominio, sin Spring |
| `FranquiciaControllerTest` | Capa HTTP con `WebTestClient`: códigos 201/204/400/404/409 y el JSON de error |
| `FranquiciaMongoAdapterTest` | Conversión dominio ↔ documento sin perder datos |
| `FranquiciaIntegracionTest` | Flujo completo contra un **MongoDB real**. Se omite si no defines la variable: |

```bash
MONGODB_URI_TEST=mongodb://localhost:27017 mvn test
```

(usa una base de datos con nombre aleatorio para no tocar tus datos).

## Infraestructura como código (Terraform)

La carpeta [`terraform/`](terraform) es una **plantilla de ejemplo, no desplegada**, que aprovisiona la persistencia en **MongoDB Atlas sobre AWS** (cluster gratuito M0, usuario con contraseña generada y lista de IPs permitidas). Se validó con `terraform validate`, pero nunca se ejecutó `apply` (requiere una cuenta y llaves de Atlas).

```bash
cd terraform
export MONGODB_ATLAS_PUBLIC_KEY=...   # llaves de API de Atlas
export MONGODB_ATLAS_PRIVATE_KEY=...
cp terraform.tfvars.example terraform.tfvars   # edita atlas_org_id y allowed_cidr
terraform init
terraform plan
terraform apply
terraform output -raw mongodb_uri      # valor para MONGODB_URI
```

`terraform.tfvars` y el estado (`*.tfstate`) están en `.gitignore`: no subas secretos al repositorio.

## Despliegue en AWS (siguiente paso)

> Pasos sugeridos, **sin ejecutar**. Reemplaza `<cuenta>` y `<region>`.

1. **Base de datos:** aplica el Terraform anterior y guarda `mongodb_uri` en AWS Secrets Manager.
2. **Imagen en ECR:**
   ```bash
   aws ecr create-repository --repository-name franquicias-api
   aws ecr get-login-password --region <region> | docker login --username AWS --password-stdin <cuenta>.dkr.ecr.<region>.amazonaws.com
   docker build -t franquicias-api .
   docker tag franquicias-api:latest <cuenta>.dkr.ecr.<region>.amazonaws.com/franquicias-api:latest
   docker push <cuenta>.dkr.ecr.<region>.amazonaws.com/franquicias-api:latest
   ```
3. **ECS (Fargate):** crea un cluster, una *task definition* con esa imagen, el puerto 8080 y la variable `MONGODB_URI` tomada del secreto, y un *service* detrás de un Application Load Balancer con health check en `/actuator/health`.
4. **Red:** en Atlas, permite en la lista de IPs la IP de salida de la tarea (por ejemplo la de un NAT Gateway) usando `allowed_cidr`.
5. **Comprobar:** `curl http://<dns-del-alb>/actuator/health`.

## Decisiones de diseño

- **WebFlux + Mongo reactivo:** todo el camino es no bloqueante; cumple el plus de programación funcional/reactiva (`Mono`, `Flux`, `map`, `flatMap`).
- **Un documento por franquicia, con sucursales y productos anidados:** las consultas ("el producto con más stock por sucursal") leen un solo documento y no hacen joins. El límite es que una franquicia enorme crecería en un solo documento (16 MB); para este alcance es lo más simple. Si creciera, se separarían las sucursales en su propia colección.
- **Modelo de dominio inmutable (records):** cada cambio devuelve una copia nueva; las reglas (nombres vacíos, stock negativo, duplicados) viven en el modelo y se prueban sin Spring.
- **Dominio separado del documento de Mongo:** el dominio no tiene anotaciones de Spring. El adaptador convierte entre ambos.
- **Un único puerto de entrada** (`FranquiciaUseCase`) con todas las operaciones, porque son pocas y cohesivas.
- **Ids UUID** para sucursales y productos (como están anidados, no tienen `_id` propio de Mongo).
- **Bloqueo optimista (`@Version`):** como se guarda la franquicia completa, dos peticiones simultáneas podrían pisarse. Con `@Version`, la segunda recibe un 409 en vez de perder datos (probado con 30 peticiones simultáneas). Mejora posible: reintentar automáticamente.
- **Nombres únicos** (sin distinguir mayúsculas): franquicia en toda la base, sucursal dentro de su franquicia y producto dentro de su sucursal. Para la franquicia se refuerza con un índice único en Mongo.
- **Empate en el producto con más stock:** gana el primero agregado. Las sucursales sin productos no aparecen en el resultado.
- **Validación en dos niveles:** `@Valid` en los DTOs (400 con detalle por campo) y reglas en el dominio como segunda línea de defensa.
- **Manejo de errores centralizado** con `@RestControllerAdvice`: los controladores no tienen `try/catch`.
- **Seguridad:** no hay autenticación (no se pidió). Para producción se añadiría Spring Security con JWT. Las entradas no se concatenan en consultas, y las consultas de Mongo pasan por Spring Data, que parametriza los valores.

## Flujo de trabajo con git

- Historial con commits pequeños y descriptivos (estilo *Conventional Commits*: `feat`, `test`, `chore`, `docs`...).
- Rama `main` estable. Para cambios nuevos: `git switch -c feature/nombre-corto`, commits pequeños, *pull request* hacia `main` y merge cuando los tests pasen.
- Para publicarlo: crear un repositorio público vacío (GitHub/Bitbucket) y ejecutar
  ```bash
  git remote add origin <url-del-repositorio>
  git push -u origin main
  ```
