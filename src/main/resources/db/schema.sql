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

-- Comptable: clients, invoices and payments. Child tables are emptied before
-- their parents by BaseDeTest, in the same order as the foreign keys below.
CREATE TABLE IF NOT EXISTS clients (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    nom                  VARCHAR(120) NOT NULL,
    ice                  VARCHAR(15)  NULL,
    telephone            VARCHAR(30)  NULL,
    email                VARCHAR(190) NULL,
    adresse              VARCHAR(255) NULL,
    delai_paiement_jours INT          NOT NULL DEFAULT 30,
    CONSTRAINT pk_clients PRIMARY KEY (id),
    CONSTRAINT uk_clients_ice UNIQUE (ice),
    CONSTRAINT ck_clients_delai_paiement_jours CHECK (delai_paiement_jours >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS factures (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    numero           VARCHAR(40)   NOT NULL,
    client_id        BIGINT        NOT NULL,
    mission_id       BIGINT        NULL,
    reference_trajet VARCHAR(120)  NULL,
    montant_ht       DECIMAL(12,2) NOT NULL,
    taux_tva         DECIMAL(5,2)  NOT NULL,
    montant_tva      DECIMAL(12,2) NOT NULL,
    montant_ttc      DECIMAL(12,2) NOT NULL,
    date_emission    DATE          NOT NULL,
    date_echeance    DATE          NOT NULL,
    statut           VARCHAR(20)   NOT NULL DEFAULT 'EN_ATTENTE',
    CONSTRAINT pk_factures PRIMARY KEY (id),
    CONSTRAINT uk_factures_numero UNIQUE (numero),
    CONSTRAINT uk_factures_mission_id UNIQUE (mission_id),
    CONSTRAINT ck_factures_statut CHECK (statut IN ('EN_ATTENTE', 'PAYEE', 'ANNULEE')),
    CONSTRAINT ck_factures_montants CHECK (montant_ht >= 0 AND taux_tva >= 0
        AND montant_tva >= 0 AND montant_ttc >= 0),
    CONSTRAINT fk_factures_client_id FOREIGN KEY (client_id) REFERENCES clients (id),
    INDEX idx_factures_date_emission (date_emission),
    INDEX idx_factures_statut (statut),
    INDEX idx_factures_client_id (client_id),
    INDEX idx_factures_montant_ttc (montant_ttc)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS paiements (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    facture_id BIGINT      NOT NULL,
    mode       VARCHAR(25) NOT NULL,
    reference  VARCHAR(80) NULL,
    date       DATE        NOT NULL,
    CONSTRAINT pk_paiements PRIMARY KEY (id),
    CONSTRAINT ck_paiements_mode CHECK (mode IN ('VIREMENT', 'CHEQUE', 'EFFET_DE_COMMERCE')),
    CONSTRAINT fk_paiements_facture_id FOREIGN KEY (facture_id) REFERENCES factures (id),
    INDEX idx_paiements_facture_id (facture_id),
    INDEX idx_paiements_date (date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
