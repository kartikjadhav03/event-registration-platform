
# Event Registration Platform

A backend REST API built with **Java 21 and Spring Boot** that allows users to discover events, organise events, and register for events. It includes JWT-based authentication, role-based authorization, email verification, password reset, and event registration management.

## Features

### Authentication and Security

- User registration with input validation
- Email verification
- Login using email and password
- JWT access tokens and refresh tokens
- Refresh token rotation
- Secure password hashing using BCrypt
- Forgot password and reset password functionality
- Stateless authentication using Spring Security
- Role-based access control

### User Management

- View and update own profile
- Admin can view all users
- Admin can view individual users
- Pagination for user listing

### Event Management

- Create events
- View published events
- View event details
- View events created by the logged-in organiser
- Update events
- Publish and unpublish events
- Cancel events
- Soft-delete events
- Admin can feature or unfeature events
- Pagination and sorting for published events

### Event Registration

- Attendees can register for published events
- Attendees can cancel their registrations
- Prevent duplicate active registrations
- Enforce event capacity
- Support reactivating a previously cancelled registration

### API Documentation

- Swagger UI
- OpenAPI documentation
- JWT Bearer authentication support in Swagger

## Tech Stack

| Technology | Purpose |
|---|---|
| Java 21 | Programming language |
| Spring Boot 4.1.1 | Backend framework |
| Spring Web MVC | REST APIs |
| Spring Security | Authentication and authorization |
| Spring Data JPA | Database operations |
| Hibernate | ORM |
| MySQL | Relational database |
| Redis | Redis integration |
| Spring Mail | Email verification and password reset |
| JJWT | JWT token generation and validation |
| BCrypt | Password hashing |
| Maven | Build and dependency management |
| Swagger / OpenAPI | API documentation |
| JUnit and Mockito | Automated testing |

## User Roles

| Role | Permissions |
|---|---|
| ADMIN | Manage users, manage events, feature events |
| ORGANISER | Create and manage own events |
| ATTENDEE | Browse events and register/cancel registrations |

Access to protected endpoints is controlled by Spring Security and role-based authorization.

## Project Structure

```text
src/main/java/com/event_registration_platform/
├── config/
│   ├── JwtFilter.java
│   ├── OpenApiConfig.java
│   ├── RedisConfig.java
│   └── SecurityConfig.java
├── controller/
│   ├── AuthController.java
│   ├── UserController.java
│   ├── EventController.java
│   ├── RegistrationController.java
│   └── TestController.java
├── dto/
├── entity/
├── exception/
├── repository/
├── service/
└── util/
```

## Prerequisites

Install the following before running the application:

- Java 21 or later
- MySQL
- Redis (if using Redis-backed functionality)
- Git

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/kartikjadhav03/event-registration-platform.git
cd event-registration-platform
```

### 2. Create the MySQL database

Open MySQL and execute:

```sql
CREATE DATABASE event_registration_db;
```

The application uses the following database configuration:

```text
Database: event_registration_db
Host: localhost
Port: 3306
Username: root
```

### 3. Configure environment variables

The application reads database, email, and JWT credentials from environment variables.

Set the following variables in your terminal:

```bash
export DB_PASSWORD='your_mysql_password'
export MAIL_USERNAME='your_email@gmail.com'
export MAIL_PASSWORD='your_email_app_password'
export JWT_SECRET='your_generated_secret'
```

Generate a strong JWT secret, for example:

```bash
openssl rand -base64 32
```

Use the generated value as `JWT_SECRET`.

**Important:** Never commit actual passwords, email app passwords, JWT secrets, or other credentials to GitHub.

Email configuration is required for email verification and password reset functionality.

### 4. Run the application

On macOS/Linux:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

The application runs at:

```text
http://localhost:8080
```

### 5. Run automated tests

```bash
./mvnw clean test
```

## API Endpoints

All endpoints use the `/api` base path.

### Authentication — `/api/auth`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/api/auth/register` | Register a new user | Public |
| GET | `/api/auth/verify?token={token}` | Verify email | Public |
| POST | `/api/auth/login` | Login | Public |
| POST | `/api/auth/refresh` | Refresh access token | Public |
| POST | `/api/auth/forgot-password` | Request password reset | Public |
| POST | `/api/auth/reset-password` | Reset password | Public |

### User Management — `/api/users`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/api/users/me` | Get own profile | Authenticated |
| PUT | `/api/users/me` | Update own profile | Authenticated |
| GET | `/api/users` | Get all users with pagination | ADMIN |
| GET | `/api/users/{id}` | Get user by ID | ADMIN |

### Event Management — `/api/events`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/api/events` | Create event | ORGANISER / ADMIN |
| GET | `/api/events` | Get published events | Public |
| GET | `/api/events/{id}` | Get published event details | Public |
| GET | `/api/events/my` | Get own events | ORGANISER / ADMIN |
| PUT | `/api/events/{id}` | Update event | ORGANISER / ADMIN |
| PATCH | `/api/events/{id}/publish` | Publish event | ORGANISER / ADMIN |
| PATCH | `/api/events/{id}/unpublish` | Unpublish event | ORGANISER / ADMIN |
| PATCH | `/api/events/{id}/cancel` | Cancel event | ORGANISER / ADMIN |
| DELETE | `/api/events/{id}` | Soft-delete event | ORGANISER / ADMIN |
| PATCH | `/api/events/{id}/feature` | Toggle featured status | ADMIN |

Event listing supports pagination and sorting.

Example:

```http
GET /api/events?page=0&size=10&sortBy=eventDate&direction=asc
```

Supported sort fields:

- `eventDate`
- `title`
- `price`
- `createdAt`

Maximum page size: 100.

### Event Registration — `/api/events`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/api/events/{eventId}/register` | Register for an event | ATTENDEE |
| POST | `/api/events/{eventId}/cancel-registration` | Cancel registration | ATTENDEE |

## Authentication

Protected endpoints require a JWT access token in the Authorization header.

Example:

```http
Authorization: Bearer YOUR_ACCESS_TOKEN
```

Token expiration:

| Token | Expiration |
|---|---|
| Access token | 15 minutes |
| Refresh token | 7 days |

Use the refresh endpoint with the refresh token in the request body to obtain refreshed authentication credentials.

## Swagger API Documentation

After starting the application, open:

- Swagger UI: [Open Swagger UI](http://localhost:8080/swagger-ui/index.html)
- OpenAPI JSON: [OpenAPI Documentation](http://localhost:8080/v3/api-docs)

Swagger supports JWT Bearer authentication for testing protected endpoints.

## Configuration

The main configuration file is:

```text
src/main/resources/application.properties
```

Sensitive values are externalized through environment variables:

| Environment Variable | Purpose |
|---|---|
| `DB_PASSWORD` | MySQL password |
| `MAIL_USERNAME` | Email account |
| `MAIL_PASSWORD` | Email app password |
| `JWT_SECRET` | JWT signing secret |

## Testing

The project includes automated tests for:

- Application context startup
- User service operations
- JWT authentication filter
- Event service operations
- Event registration service operations

Run all tests using:

```bash
./mvnw clean test
```

Latest verified test result:

```text
Tests run: 63
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

## Future Improvements

- Docker and Docker Compose deployment
- GitHub Actions CI pipeline
- Production-specific configuration and deployment

## Author

**Kartik Jadhav**

[GitHub](https://github.com/kartikjadhav03) | [LinkedIn](https://www.linkedin.com/in/kartik-jadhav-22a0b2246/)

Java Backend Developer | Java | Spring Boot | REST APIs | MySQL