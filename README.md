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
* **Windows (PowerShell / Command Prompt):**
  ```cmd
  .\mvnw.cmd clean javafx:run
  ```
* **Git Bash / Linux / macOS:**
  ```bash
  ./mvnw clean javafx:run
  ```

---

## 3. Running Tests & CI Verification

The project includes unit and integration tests with an automated GitHub Actions CI pipeline. Before pushing any branch or opening a Pull Request, run the local verification suite:

```bash
# Ensure the database container is up before running tests
./mvnw clean test
```

A clean build must report `[INFO] BUILD SUCCESS` with zero test failures.

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