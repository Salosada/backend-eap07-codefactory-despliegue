-- TiDB / MySQL: ampliar columna status para estados de reembolso (PARTIALLY_REFUNDED = 18 chars).
-- Ejecutar una vez en TiDB Cloud si al reembolsar aparece "Data truncation" o "Data Too Long".
-- ddl-auto=update no siempre altera columnas existentes creadas en sprints anteriores.

ALTER TABLE transactions
    MODIFY COLUMN status VARCHAR(32) NOT NULL;
