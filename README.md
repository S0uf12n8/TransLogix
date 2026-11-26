# TransLogix

A desktop application for managing the operations of a small road-freight company: missions,
vehicle and driver assignment, invoicing, employees and the driver's own workspace.

Built with **Java 21**, **JavaFX 21** (FXML), **MySQL 8** and **Maven**. University team project
(Licence Génie Informatique), team of 6.

**Core rule:** the system refuses overlapping vehicle or driver assignments, and every delivered
mission is linked to its invoice.

> **Status (26 Nov 2026):** iteration 2 delivered (tag `v0.2-iteration2`). Iteration 3 is in progress
> and due on **10 Dec 2026**. `main` builds green: 438 tests, 0 failures.

---

## 1. Features by role

Each user signs in and sees only the screens for their role.

| Role | Screens |
|---|---|
| **Gérant / Admin** | Dashboard with charts, user management (create, edit, deactivate, reset password), audit log, reports with CSV export |
| **Dispatcher** | Missions (multi-criteria search, right-click menu, status changes, chart, export), planning and assignment with overlap check, fleet with availability chart, fuel vouchers, incidents |
| **Comptable** | Clients, invoices generated from delivered missions (right-click menu), payments, dashboard charts |
| **RH** | Employees, compliance documents (licences, medical visits) with expiry status, dashboard |
| **Chauffeur** | His own missions only (start / deliver), mission detail, history, incidents, expenses, vehicle inspection, proof of delivery, profile |

---

## 2. Prerequisites

- **JDK 21** (Temurin or OpenJDK)
- **Docker Desktop**, only if you want data saved in MySQL
- **Git**

No local Maven install is needed: the project ships the Maven wrapper (`mvnw` / `mvnw.cmd`).

---

## 3. Run the application

### Option A: demo mode (no database)

```powershell
.\mvnw.cmd javafx:run
```

Everything stays in memory and restarts from the demo data each time.

### Option B: with MySQL (data is saved)

1. Start the database (host port **3307**, so it does not collide with a local MySQL or XAMPP):

   ```powershell
   docker compose up -d db
   ```

2. Point the app at it and run:

   - **Windows (PowerShell)**
     ```powershell
     $env:DB_URL = "jdbc:mysql://localhost:3307/translogix_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
     $env:DB_USER = "translogix_user"
     $env:DB_PASSWORD = "translogix_pass"
     .\mvnw.cmd javafx:run
     ```
   - **Git Bash / Linux / macOS**
     ```bash
     export DB_URL="jdbc:mysql://localhost:3307/translogix_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
     export DB_USER=translogix_user DB_PASSWORD=translogix_pass
     ./mvnw javafx:run
     ```

On start-up the app creates its tables itself from `src/main/resources/db/schema.sql` (safe to
replay) and creates the demo accounts on the very first start only.

### Where is my data? (bottom of the sidebar)

| Sidebar | Meaning |
|---|---|
| `Base MySQL connectée` | Connected to MySQL. Data from the modules that already have MySQL storage is saved (see section 6). |
| `Mode démonstration · non enregistré` | `DB_URL` is not set: everything is in memory. |
| `MySQL indisponible · non enregistré` (orange) | `DB_URL` is set but MySQL did not answer. A warning is shown at start-up and nothing is saved. Start the container and relaunch. |

### Demo accounts

These accounts exist only for the demo (created by `ComptesDemo.java`).

| Role | Email | Password |
|---|---|---|
| Gérant / Admin | `admin@translogix.ma` | `admin1234` |
| Comptable | `comptable@translogix.ma` | `comptable1234` |
| RH | `rh@translogix.ma` | `rh123456` |
| Dispatcher | `dispatcher@translogix.ma` | `dispatcher1234` |
| Chauffeur | `m.bennani@translogix.ma` | `chauffeur123` |

---

## 4. Build and tests

```powershell
.\mvnw.cmd -B verify
```

This is the same command the CI runs. A good run ends with `BUILD SUCCESS` and 0 failures.

- **Unit tests** (JUnit 5) cover the services: business rules, overlap check, mission life cycle,
  invoicing, and the numbers behind the charts.
- **DAO tests**: one shared contract test per DAO, run against both the in-memory and the MySQL
  version. The MySQL tests run only when `DB_URL` is set; without it they are skipped (44 today).
  To run them locally, start the container and set the three variables from section 3 first.

**CI:** GitHub Actions (`.github/workflows/ci.yml`) runs `./mvnw -B verify` against a MySQL 8 service
on every pull request and on every push to `main`. The check is called `build`.

---

## 5. Architecture

```
controller  →  service  →  dao (interface)  →  dao/jdbc   (MySQL)
                                             →  dao/memory (in memory)
```

- **controller**: one FXML view + one controller per screen. A controller never calls a DAO.
- **service**: all business rules (overlap check, mission life cycle, invoice generation, validation).
- **dao**: interfaces, each with an in-memory and, where delivered, a JDBC implementation.
  JDBC code uses `PreparedStatement` only, and SQL errors are turned into clear French messages
  (`SqlErreurs`), never a stack trace on screen.
- **`Persistance`** picks the mode at start-up (MySQL when `DB_URL` is set and reachable, memory
  otherwise). **`AppContext`** is the only place where DAOs and services are created and wired.

```
src/main/java/ma/translogix/
├── Main.java, AppContext.java, Persistance.java, ComptesDemo.java
├── controller/   admin, auth, chauffeur, common, comptable, dispatcher, rh
├── service/      business rules
├── dao/          interfaces
│   ├── jdbc/     MySQL implementations, ConnexionBase, SchemaBase, SqlErreurs, Transactions
│   └── memory/   in-memory implementations
├── model/        entities and enums
└── util/         Recherche (accent/case-insensitive search), CsvExporter, ValidationException…

src/main/resources/
├── db/schema.sql              database schema (idempotent)
└── ma/translogix/fxml/<role>/ FXML views, plus css/ and fonts/
```

---

## 6. Iteration 3: where we are

| Goal | State |
|---|---|
| Charts from real data | ✅ Admin, Comptable and Dispatcher · 🔄 RH (branch in progress) |
| Dynamic reports, export | ✅ Admin reports, Dispatcher missions · 🔄 other modules |
| Multi-criteria search | ✅ Admin users, Dispatcher missions · 🔄 RH, Comptable, Chauffeur |
| Dialogs and right-click menus | ✅ Admin, Comptable invoices, Dispatcher missions, Chauffeur · 🔄 RH |
| MySQL storage | ✅ Users, audit log · 🔄 Clients, invoices, payments (DAOs written, not wired yet) · 🔄 Missions, fleet, employees |
| DB error handling | ✅ Clear message at start-up and in the sidebar when MySQL is down; SQL errors shown in French |
| Tests | ✅ 438 tests, 0 failures |

Until a module's MySQL storage is wired in `AppContext`, that module's data stays in memory even
when `Base MySQL connectée` is shown.

---

## 7. Team

| Member | Role | Module |
|---|---|---|
| Souf | Gérant / Admin, project lead | admin, dashboard, users |
| Hiba | Comptable | clients, invoices, payments |
| Hamid | Dispatcher | missions, assignment, fleet |
| Majda | RH | employees, compliance |
| Oussama | Chauffeur | driver workspace |
| Mohamed | Documentation | diagrams, docs |

---

## 8. Contributing

- Never push to `main`: it is protected (1 approval + green `build` check required).
- Branch from the latest `main`: `feature/…`, `fix/…`, `docs/…`, `chore/…`, `test/…`.
- Commits follow Conventional Commits with the module as scope: `feat(rh): …`, `fix(dispatcher): …`.
  Small commits, one logical change each.
- Run `.\mvnw.cmd -B verify` before pushing. Open a pull request with "What changed" and "How to test"
  filled in. Nobody merges their own pull request.
- Schema changes go in `src/main/resources/db/schema.sql` (statements must stay replayable:
  `CREATE TABLE IF NOT EXISTS`). Ask the lead before touching shared files (`AppContext.java`,
  `pom.xml`, `schema.sql`, `.github/`).

Design documents and mock-ups are in [`docs/`](docs/).
