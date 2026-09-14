# Library Management System

A simple Spring Boot library management system with MySQL, Thymeleaf, Spring Security, BCrypt password storage, role-based authorization, Hibernate/JPA, and Tailwind CSS.

## Features

- User registration and login
- BCrypt password hashing through Spring Security
- Admin and user roles with separate permissions
- Searchable book catalog with categories
- User book borrowing and returns
- Admin book management
- Admin user role and enabled/disabled management
- Admin borrow-record overview
- Bootstrap seed data in `src/main/resources/data.sql`

## Requirements

- Java 25 LTS+
- Maven
- MySQL 8+

## Docker

Run the whole stack with one command:

```bash
docker compose up --build
```

Docker Compose reads `.env` automatically. The `.env` file is the source of truth for Docker/runtime values; `.env.example` is the template for recreating it during provisioning.

This starts MySQL 8 and the Spring Boot app using the values from `.env`. The MySQL service `command` writes and applies the visible `CREATE USER`, `ALTER USER`, and `GRANT ALL PRIVILEGES` SQL on startup before handing control to the official MySQL entrypoint. Uploaded files are stored in the Docker volume mounted at `/app/uploads`.

## Environment

Runtime values are kept in `.env`:

```text
MYSQL_ROOT_PASSWORD
MYSQL_DATABASE
DB_URL
DB_USERNAME
DB_PASSWORD
SPRING_PROFILES_ACTIVE
APP_STORAGE_LOCAL_ROOT
APP_STORAGE_S3_BUCKET
APP_STORAGE_S3_REGION
APP_STORAGE_S3_PREFIX
APP_PORT
MYSQL_PORT
```

Set those variables manually in the host environment or provide `.env` during provisioning. Spring Boot also imports `.env` for local `mvn spring-boot:run` executions.

## Storage

Books support two uploaded assets:

- PDF file
- Cover image

File metadata is stored in the `uploaded_files` table. Book rows link to those records through `pdf_file_id` and `cover_image_file_id`. The bytes can live on local disk or AWS S3.

Local storage is the default Spring profile:

```bash
mvn spring-boot:run
```

To switch to S3, activate the `s3` Spring profile and provide the bucket settings:

```bash
$env:SPRING_PROFILES_ACTIVE="s3"
$env:APP_STORAGE_S3_BUCKET="your-bucket"
$env:APP_STORAGE_S3_REGION="us-east-1"
$env:APP_STORAGE_S3_PREFIX="library"
mvn spring-boot:run
```

AWS credentials are read by the AWS SDK default provider chain, such as environment variables, shared credentials, or an instance/task role. In Docker, switch storage by changing `SPRING_PROFILES_ACTIVE` from `local` to `s3` in `.env` and setting the S3 bucket/region variables there.

## Run

Start MySQL first, then run:

```bash
mvn spring-boot:run
```

Open `http://localhost:8080`.

## Seeded Accounts

- Admin: `admin@library.test` / `admin123`
- User: `user@library.test` / `user12345`

Roles, users, user-role mappings, categories, and starter books are inserted from `data.sql`. The seeded passwords are BCrypt hashes.
