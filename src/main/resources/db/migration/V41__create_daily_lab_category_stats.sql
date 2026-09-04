CREATE TABLE daily_lab_category_stats (
    lab_id           BIGINT NOT NULL,
    stat_date        DATE NOT NULL,
    category         VARCHAR(255) NOT NULL,
    test_count       BIGINT NOT NULL DEFAULT 0,
    gross_revenue    NUMERIC(14,2) NOT NULL DEFAULT 0,
    discount         NUMERIC(14,2) NOT NULL DEFAULT 0,
    paid_revenue     NUMERIC(14,2) NOT NULL DEFAULT 0,
    due_revenue      NUMERIC(14,2) NOT NULL DEFAULT 0,
    cash_revenue     NUMERIC(14,2) NOT NULL DEFAULT 0,
    upi_revenue      NUMERIC(14,2) NOT NULL DEFAULT 0,
    card_revenue     NUMERIC(14,2) NOT NULL DEFAULT 0,
    updated_at       TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (lab_id, stat_date, category)
);

CREATE INDEX idx_daily_lab_category_stats_lab_date ON daily_lab_category_stats (lab_id, stat_date);
