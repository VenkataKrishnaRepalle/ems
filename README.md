# Docker

## Steps to Create Docker Image

1. Check Docker Images

```bash
docker images
```

2. Build the Docker Image

```bash
    docker build -t your-image-name .
```

3. Run the Docker container:

```bash
    docker run -d -p 8080:8080 your-image-name
```

## Setup Postgres in Docker

1. Pull the Postgres Docker Image
    * The first step is to pull the Postgres Docker image from the Docker Hub repository. This is done by running the
      following command:

```bash
docker pull postgres
```

2. Create a Docker Volume
    * Next, we need to create a Docker volume to persist our Postgres data. This is done by running the following
      command:

```bash
docker volume create postgres_data
```

3. Run the Postgres Docker Container
    * Now we can run the Postgres Docker container using the following command:

```bash
docker run --name postgres -e POSTGRES_PASSWORD=root -d -p 5432:5432 -v postgres_data:/var/lib/postgresql/data postgres
```

4. Verify the Container is Running

* To verify that the Docker container is running, run the following command:

```bash
docker ps
```

## Setup podman Postgres DB
1. Pull the podman docker Image
```bash
podman pull postgres
```

2. Create Podman Volume
```bash
podman volume create postgres_data
```

3. Run the Postgres Podman Container
   * Now we can run the Postgres Docker container using the following command:

```bash
podman run --name postgres -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=root -e POSTGRES_DB=ems -d -p 5432:5432 -v postgres_data:/var/lib/postgresql/data postgres 
```

4. Creating Multiple Databases
```bash
podman run --name postgres -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=root -e POSTGRES_MULTIPLE_DATABASES="ems,kyc" -d -p 5432:5432 -v postgres_data:/var/lib/postgresql/data postgres 
```

# Keycloak

## Enable Keycloak Login Endpoint

Set the following environment variables:

- `KEYCLOAK_ENABLED=true`
- `KEYCLOAK_ISSUER_URI=http://<keycloak-host>:<port>/realms/<realm>`

Then call the API with a Keycloak access token:

```bash
curl -X POST http://localhost:8083/api/keycloak/login-keycloak \
  -H "Authorization: Bearer <KEYCLOAK_ACCESS_TOKEN>"
```

You can also call secured `/api/**` endpoints directly with the Keycloak token (no cookie exchange), as long as `KEYCLOAK_ISSUER_URI` matches the token `iss` claim.

## Keycloak Admin Client (User Provisioning)

This app can call Keycloak Admin REST API to create users (e.g. `POST /admin/realms/{realm}/users`) using a **client-credentials** token.

Required env vars:

- `KEYCLOAK_BASE_URL=http://<keycloak-host>:<port>`
- `KEYCLOAK_REALM=<realm>`
- `KEYCLOAK_ADMIN_CLIENT_ID=<confidential-client-id>`
- `KEYCLOAK_ADMIN_CLIENT_SECRET=<client-secret>`

Keycloak setup (to avoid `403 Forbidden` from the Admin API):

- Create/configure the admin client as **confidential** with **Service accounts enabled**
- In `Clients -> <admin-client> -> Service accounts roles`, assign roles from `realm-management`:
  - `manage-users` (and typically `view-users`)

## Run Keycloak via Docker Compose

`docker-compose.yml` starts Keycloak on `http://keycloak:8080` and imports `init/keycloak/realm-ems.json`:

- Realm: `ems`
- Frontend clientId: `ems-frontend`
- Test users: `admin` / `Admin@123`, `venky` / `Venky@123`
- Keycloak admin: `admin` / `admin`

To make `http://keycloak:8080` work from your browser, add this to your hosts file:

- Windows: `C:\Windows\System32\drivers\etc\hosts`
  - `127.0.0.1 keycloak`

Frontend (example config):

- `url`: `http://keycloak:8080`
- `realm`: `ems`
- `clientId`: `ems-frontend`
