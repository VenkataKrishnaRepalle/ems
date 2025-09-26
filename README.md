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