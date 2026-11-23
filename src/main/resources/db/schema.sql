-- TransLogix: MySQL 8 schema, admin module (user accounts and audit log).
-- Other modules add their tables to this file in their own PR.
-- Idempotent: every statement can be replayed (CREATE TABLE IF NOT EXISTS, indexes declared inline).
-- Apply: mysql -h 127.0.0.1 -P 3307 -u translogix_user -p translogix_db < src/main/resources/db/schema.sql
-- Statements are separated by ';' at the end of a line (the test helper splits on that).

-- utf8mb4_0900_ai_ci compares case- and accent-insensitively: the UNIQUE email also refuses
-- "Sara@Translogix.ma" when "sara@translogix.ma" exists, and WHERE email = ? uses the index.
CREATE TABLE IF NOT EXISTS utilisateurs (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    nom               VARCHAR(80)  NOT NULL,
    prenom            VARCHAR(80)  NOT NULL,
    email             VARCHAR(190) NOT NULL,
    mot_de_passe_hash VARCHAR(255) NOT NULL,
    role              VARCHAR(20)  NOT NULL,
    actif             BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_utilisateurs PRIMARY KEY (id),
    CONSTRAINT uk_utilisateurs_email UNIQUE (email),
    CONSTRAINT ck_utilisateurs_role CHECK (role IN ('GERANT', 'DISPATCHER', 'COMPTABLE', 'RH', 'CHAUFFEUR')),
    -- "Is there already a Gérant?" (UtilisateurService.gerantExistant) and role filters.
    INDEX idx_utilisateurs_role (role)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Append-only by design: AuditDao has no update/delete. In production, also
-- REVOKE UPDATE, DELETE ON translogix_db.audit_evenements FROM the application account.
CREATE TABLE IF NOT EXISTS audit_evenements (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    horodatage  DATETIME(6)  NOT NULL,
    utilisateur VARCHAR(150) NOT NULL,
    module      VARCHAR(60)  NOT NULL,
    action      VARCHAR(500) NOT NULL,
    resultat    VARCHAR(10)  NOT NULL,
    CONSTRAINT pk_audit_evenements PRIMARY KEY (id),
    CONSTRAINT ck_audit_resultat CHECK (resultat IN ('REUSSI', 'REFUSE')),
    -- Journal sorted by date and the period filter of the audit screen.
    INDEX idx_audit_horodatage (horodatage),
    -- Module filter combined with the period ("Utilisateurs" in November).
    INDEX idx_audit_module_horodatage (module, horodatage)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
