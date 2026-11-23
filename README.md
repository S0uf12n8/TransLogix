# TransLogix

Transport & Logistics operations desktop application built with **Java 21**, **Spring Boot**, **JavaFX**, and **MySQL 8.0**.

---

## 1. Prerequisites

Make sure you have installed:
* **JDK 21** (Temurin or OpenJDK)
* **Docker Desktop** (Engine running with WSL2 or Hyper-V)
* **Git** (Git Bash recommended on Windows)

---

## 2. Quick Start (Run Locally)

### Step 1: Clone the Repository
```bash
git clone https://github.com/S0uf12n8/TransLogix.git
cd TransLogix
```

### Step 2: Start the MySQL Database
```bash
docker compose up -d db
```
> **Note on Ports:** The local MySQL container exposes host port **`3307`** (mapped internally to `3306`) to prevent collisions with any native MySQL/XAMPP services running locally.

### Step 3: Run the Application (JavaFX UI)

The app uses MySQL only when `DB_URL` is set. It then creates its tables itself (`src/main/resources/db/schema.sql`, safe to replay) and creates the demo accounts on the very first start only.

* **Windows (PowerShell):**
  ```powershell
  $env:DB_URL = "jdbc:mysql://localhost:3307/translogix_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
  $env:DB_USER = "translogix_user"
  $env:DB_PASSWORD = "translogix_pass"
  .\mvnw.cmd clean javafx:run
  ```
* **Git Bash / Linux / macOS:**
  ```bash
  export DB_URL="jdbc:mysql://localhost:3307/translogix_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
  export DB_USER=translogix_user DB_PASSWORD=translogix_pass
  ./mvnw clean javafx:run
  ```

The bottom of the sidebar shows where the data is kept:

| Sidebar | Meaning |
|---|---|
| `Base MySQL connectée` | Accounts and audit log are saved in MySQL (other modules stay in memory until their MySQL storage is delivered). |
| `Mode démonstration · non enregistré` | `DB_URL` is not set: everything is in memory and restarts from the demo data. |
| `MySQL indisponible · non enregistré` (orange) | `DB_URL` is set but MySQL did not answer: a warning is shown at startup and nothing is saved. Start the container and relaunch. |

---

## 3. Running Tests & CI Verification

The project includes unit and integration tests with an automated GitHub Actions CI pipeline. Before pushing any branch or opening a Pull Request, run the local verification suite:

```bash
# Ensure the database container is up before running tests
./mvnw clean test
```

A clean build must report `[INFO] BUILD SUCCESS` with zero test failures.

---

## Security

| Control | Where | What it does |
|---|---|---|
| Password hashing | `util/PasswordHasher` | PBKDF2-HMAC-SHA512, 210 000 iterations, 16-byte random salt per password (OWASP Password Storage Cheat Sheet). JDK only, no extra library. Old SHA-256 hashes are replaced transparently at the next successful login. |
| No account enumeration | `service/AuthService` | Same message for an unknown email, a wrong password or a deactivated account; an unknown email takes the same time as a wrong password. |
| Brute-force lockout | `service/AuthService` | 5 failures in a row on an email block it for 15 minutes, even with the right password. Unknown emails are blocked the same way. |
| Audit of logins | `service/AuditService` | Every login success, failure and block is written to the audit journal (module "Connexion"), never the password. |
| Role-based access | `controller/common/NavigationMenu.autorise` + `ShellController` | Each role opens only its own module's pages (the Gérant: every module except the driver space). A refused page is audited (module "Sécurité"). |
| SQL injection | `dao/jdbc` | Parameterized `PreparedStatement` only; database errors are shown as French messages, never as stack traces. |

Known limit: the lockout counters live in memory, so they restart at zero with the application.

The tests hash with 1 000 iterations (`translogix.hash.iterations` in the surefire configuration of `pom.xml`) to stay fast; the application always uses 210 000.

---

## 4. Architecture & Team Structure

To prevent integration bottlenecks across our team of 6, we use a **Vertical Slice (Feature-First)** architectural model instead of generic horizontal layers:

```text
src/main/java/ma/translogix/
├── config/              # Spring, DataSource, Security, and JavaFX wiring
├── common/              # Shared base models, utilities, and UI widgets
└── modules/             # Vertical Feature Slices
    ├── user/            # User CRUD: Model, Repository, Service, JavaFX Controller, FXML
    ├── vehicule/        # Vehicule CRUD: Model, Repository, Service, JavaFX Controller, FXML
    ├── driver/          # Driver CRUD: Model, Repository, Service, JavaFX Controller, FXML
    └── client/          # Client CRUD: Model, Repository, Service, JavaFX Controller, FXML
```

* Work strictly inside your assigned feature package under `modules/<feature-name>`.
* Avoid modifying shared classes in `common/` or `config/` without notifying the project lead.

---

## 5. Development & Git Workflow

### 1. Branch Naming
Never push directly to `main`. Create a feature branch cut from the latest `main`:

```bash
# Always sync main before creating a branch
git checkout main
git pull origin main

# Create your feature branch
git checkout -b feature/<feature-name>
# Examples:
# git checkout -b feature/user-crud
# git checkout -b feature/vehicule-crud
```

### 2. Database Migrations (Flyway)
* All schema changes must be added as a new migration file in `src/main/resources/db/migration/`.
* Follow Flyway naming conventions: `V<timestamp_or_next_number>__<short_description>.sql` (e.g., `V2__create_vehicule_table.sql`).
* **Never edit or delete an existing migration file that has already been merged.**

### 3. Pull Requests & CI
1. Push your branch to GitHub:
   ```bash
   git push -u origin feature/<feature-name>
   ```
2. Open a Pull Request targeting `main`.
3. Wait for the automated **GitHub Actions CI check** to turn green (`BUILD SUCCESS`).
4. Request a review from the team lead before squashing and merging.
5. 
