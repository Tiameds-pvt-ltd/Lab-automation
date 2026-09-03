-- Same ddl-auto=update drift as V12-V19: any table whose bigint/integer "id"
-- column has no DEFAULT (i.e. no sequence wired to auto-generate it) will fail
-- on insert once JPA relies on identity generation. This scans every base table
-- in the public schema and wires a matching sequence for any "id" column found
-- without one, advancing the sequence past existing data to avoid collisions.
-- Idempotent: CREATE SEQUENCE IF NOT EXISTS, and any table already fixed
-- (e.g. by V19) is naturally skipped since its column_default is no longer NULL.

DO $$
DECLARE
    t record;
    seq_name text;
BEGIN
    FOR t IN
        SELECT c.table_name
        FROM information_schema.columns c
        WHERE c.table_schema = 'public'
          AND c.column_name = 'id'
          AND c.column_default IS NULL
          AND c.data_type IN ('bigint', 'integer')
          AND EXISTS (
              SELECT 1 FROM information_schema.tables
              WHERE table_schema = 'public'
                AND table_name = c.table_name
                AND table_type = 'BASE TABLE'
          )
    LOOP
        seq_name := t.table_name || '_id_seq';

        EXECUTE format(
            'CREATE SEQUENCE IF NOT EXISTS %I START WITH 1 INCREMENT BY 1',
            seq_name
        );

        EXECUTE format(
            'ALTER TABLE %I ALTER COLUMN id SET DEFAULT nextval(%L)',
            t.table_name, seq_name
        );

        EXECUTE format(
            'SELECT setval(%L, COALESCE((SELECT MAX(id) FROM %I), 0) + 1, false)',
            seq_name, t.table_name
        );

        RAISE NOTICE 'V20: fixed missing id sequence default: % -> %', t.table_name, seq_name;
    END LOOP;
END $$;
