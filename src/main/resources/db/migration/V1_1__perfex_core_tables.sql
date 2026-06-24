-- Bootstrap Perfex: banco vazio (XAMPP, Docker ou remoto) ou CRM já instalado.
-- CREATE IF NOT EXISTS não sobrescreve tabelas reais do Perfex.

CREATE TABLE IF NOT EXISTS tblclients (
    userid INT AUTO_INCREMENT PRIMARY KEY,
    company VARCHAR(191) NULL,
    phonenumber VARCHAR(30) NULL,
    city VARCHAR(100) NULL,
    state VARCHAR(50) NULL,
    address VARCHAR(191) NULL,
    country INT NOT NULL DEFAULT 0,
    active INT NOT NULL DEFAULT 1,
    datecreated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    default_currency INT NOT NULL DEFAULT 0,
    show_primary_contact INT NOT NULL DEFAULT 0,
    registration_confirmed INT NOT NULL DEFAULT 1,
    addedfrom INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tblcontacts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    userid INT NOT NULL,
    firstname VARCHAR(191) NOT NULL,
    lastname VARCHAR(191) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phonenumber VARCHAR(100) NOT NULL DEFAULT '',
    password VARCHAR(255) NULL,
    datecreated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    email_verified_at DATETIME NULL,
    profile_image VARCHAR(191) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    is_primary INT NOT NULL DEFAULT 1,
    invoice_emails TINYINT(1) NOT NULL DEFAULT 1,
    estimate_emails TINYINT(1) NOT NULL DEFAULT 1,
    credit_note_emails TINYINT(1) NOT NULL DEFAULT 1,
    contract_emails TINYINT(1) NOT NULL DEFAULT 1,
    task_emails TINYINT(1) NOT NULL DEFAULT 1,
    project_emails TINYINT(1) NOT NULL DEFAULT 1,
    ticket_emails TINYINT(1) NOT NULL DEFAULT 1,
    FOREIGN KEY (userid) REFERENCES tblclients(userid),
    INDEX idx_contacts_userid (userid),
    INDEX idx_contacts_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tblstaff (
    staffid INT NOT NULL PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    firstname VARCHAR(50) NOT NULL,
    lastname VARCHAR(50) NOT NULL,
    phonenumber VARCHAR(30) NULL,
    password VARCHAR(250) NOT NULL,
    datecreated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    profile_image VARCHAR(191) NULL,
    admin INT NOT NULL DEFAULT 0,
    active INT NOT NULL DEFAULT 1,
    INDEX idx_staff_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
