# TransLogix

Transport management web app (Java 21, Spring Boot, PostgreSQL).

## Quick start
1. Install JDK 21 and Docker Desktop.
2. `docker compose up -d db`
3. Windows: `.\mvnw.cmd spring-boot:run` / Linux-Mac: `./mvnw spring-boot:run`
4. Open http://localhost:8080

## Run the tests
`.\mvnw.cmd -B verify` (the database must be running)

## Rules
- Never push to `main`; open a Pull Request from a feature branch.
- Schema changes = a new Flyway file `V<n>__description.sql`. Never edit an old one.

Note: the dev database listens on port 5455 (not 5432) to avoid clashing with a local PostgreSQL install.
