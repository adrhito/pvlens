-- PVLens PostgreSQL Schema for Supabase
-- Run this in your Supabase SQL Editor

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- -----------------------------------------------------------------------
-- SUBSTANCE (main drug entity)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS substance (
    id SERIAL PRIMARY KEY
);

-- -----------------------------------------------------------------------
-- SOURCE_TYPE
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS source_type (
    id SERIAL PRIMARY KEY,
    source_type VARCHAR(50)
);

-- Insert default source types
INSERT INTO source_type (source_type) VALUES ('PRESCRIPTION'), ('OTC'), ('OTHER')
ON CONFLICT DO NOTHING;

-- -----------------------------------------------------------------------
-- SPL_SRCFILE
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS spl_srcfile (
    id SERIAL PRIMARY KEY,
    product_id INTEGER NOT NULL REFERENCES substance(id),
    guid VARCHAR(500),
    xmlfile_name VARCHAR(500),
    source_type_id INTEGER REFERENCES source_type(id),
    application_number INTEGER DEFAULT 0,
    approval_date DATE,
    nda_sponsor VARCHAR(200)
);

CREATE INDEX IF NOT EXISTS idx_spl_srcfile_product ON spl_srcfile(product_id);

-- -----------------------------------------------------------------------
-- MEDDRA
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS meddra (
    id SERIAL PRIMARY KEY,
    meddra_code VARCHAR(12),
    meddra_ptcode VARCHAR(12),
    meddra_term VARCHAR(500),
    meddra_tty VARCHAR(10),
    meddra_aui VARCHAR(15),
    meddra_cui VARCHAR(15)
);

CREATE INDEX IF NOT EXISTS idx_meddra_code ON meddra(meddra_code);
CREATE INDEX IF NOT EXISTS idx_meddra_term ON meddra(meddra_term);

-- -----------------------------------------------------------------------
-- RXNORM
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rxnorm (
    id SERIAL PRIMARY KEY,
    aui VARCHAR(15),
    cui VARCHAR(15),
    code VARCHAR(12),
    term VARCHAR(2500),
    tty VARCHAR(10)
);

-- -----------------------------------------------------------------------
-- SNOMED
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS snomed (
    id SERIAL PRIMARY KEY,
    aui VARCHAR(15),
    cui VARCHAR(15),
    code VARCHAR(20),
    term VARCHAR(2500),
    tty VARCHAR(10)
);

-- -----------------------------------------------------------------------
-- ATC
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS atc (
    id SERIAL PRIMARY KEY,
    aui VARCHAR(15),
    cui VARCHAR(15),
    code VARCHAR(20),
    term VARCHAR(2500),
    tty VARCHAR(10)
);

-- -----------------------------------------------------------------------
-- NDC_CODE
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ndc_code (
    id SERIAL PRIMARY KEY,
    ndc_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(3500) NOT NULL,
    product_name_hash VARCHAR(64) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ndc_code ON ndc_code(ndc_code);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ndc_code_name ON ndc_code(ndc_code, product_name_hash);

-- -----------------------------------------------------------------------
-- PRODUCT_NDC (links substance to NDC codes)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_ndc (
    id SERIAL PRIMARY KEY,
    product_id INTEGER NOT NULL REFERENCES substance(id),
    ndc_id INTEGER NOT NULL REFERENCES ndc_code(id)
);

CREATE INDEX IF NOT EXISTS idx_product_ndc_product ON product_ndc(product_id);

-- -----------------------------------------------------------------------
-- SRLC (Safety Related Label Changes)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS srlc (
    id SERIAL PRIMARY KEY,
    drug_id INTEGER NOT NULL UNIQUE,
    application_number INTEGER NOT NULL,
    drug_name VARCHAR(500),
    active_ingredient VARCHAR(500),
    supplement_date DATE,
    database_updated DATE,
    url VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_srlc_drug_name ON srlc(drug_name);
CREATE INDEX IF NOT EXISTS idx_srlc_date ON srlc(supplement_date DESC);

-- -----------------------------------------------------------------------
-- SUBSTANCE_SRLC
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS substance_srlc (
    product_id INTEGER REFERENCES substance(id),
    drug_id INTEGER REFERENCES srlc(drug_id)
);

-- -----------------------------------------------------------------------
-- SUBSTANCE_RXNORM
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS substance_rxnorm (
    product_id INTEGER REFERENCES substance(id),
    rxnorm_id INTEGER REFERENCES rxnorm(id)
);

-- -----------------------------------------------------------------------
-- SUBSTANCE_SNOMED_PT
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS substance_snomed_pt (
    product_id INTEGER REFERENCES substance(id),
    snomed_id INTEGER REFERENCES snomed(id)
);

-- -----------------------------------------------------------------------
-- SUBSTANCE_ATC
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS substance_atc (
    product_id INTEGER REFERENCES substance(id),
    ndc_id INTEGER REFERENCES ndc_code(id),
    atc_id INTEGER REFERENCES atc(id)
);

-- -----------------------------------------------------------------------
-- PRODUCT_AE (Adverse Events)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_ae (
    id SERIAL PRIMARY KEY,
    product_id INTEGER NOT NULL REFERENCES substance(id),
    meddra_id INTEGER NOT NULL REFERENCES meddra(id),
    label_date DATE,
    warning BOOLEAN DEFAULT FALSE,
    blackbox BOOLEAN DEFAULT FALSE,
    exact_match BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_product_ae_product ON product_ae(product_id);
CREATE INDEX IF NOT EXISTS idx_product_ae_meddra ON product_ae(meddra_id);

-- -----------------------------------------------------------------------
-- PRODUCT_IND (Indications)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_ind (
    id SERIAL PRIMARY KEY,
    product_id INTEGER NOT NULL REFERENCES substance(id),
    meddra_id INTEGER NOT NULL REFERENCES meddra(id),
    label_date DATE,
    exact_match BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_product_ind_product ON product_ind(product_id);

-- -----------------------------------------------------------------------
-- Row Level Security (RLS) - Enable public read access
-- -----------------------------------------------------------------------
ALTER TABLE substance ENABLE ROW LEVEL SECURITY;
ALTER TABLE source_type ENABLE ROW LEVEL SECURITY;
ALTER TABLE spl_srcfile ENABLE ROW LEVEL SECURITY;
ALTER TABLE meddra ENABLE ROW LEVEL SECURITY;
ALTER TABLE rxnorm ENABLE ROW LEVEL SECURITY;
ALTER TABLE snomed ENABLE ROW LEVEL SECURITY;
ALTER TABLE atc ENABLE ROW LEVEL SECURITY;
ALTER TABLE ndc_code ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_ndc ENABLE ROW LEVEL SECURITY;
ALTER TABLE srlc ENABLE ROW LEVEL SECURITY;
ALTER TABLE substance_srlc ENABLE ROW LEVEL SECURITY;
ALTER TABLE substance_rxnorm ENABLE ROW LEVEL SECURITY;
ALTER TABLE substance_snomed_pt ENABLE ROW LEVEL SECURITY;
ALTER TABLE substance_atc ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_ae ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_ind ENABLE ROW LEVEL SECURITY;

-- Create policies for public read access
CREATE POLICY "Public read access" ON substance FOR SELECT USING (true);
CREATE POLICY "Public read access" ON source_type FOR SELECT USING (true);
CREATE POLICY "Public read access" ON spl_srcfile FOR SELECT USING (true);
CREATE POLICY "Public read access" ON meddra FOR SELECT USING (true);
CREATE POLICY "Public read access" ON rxnorm FOR SELECT USING (true);
CREATE POLICY "Public read access" ON snomed FOR SELECT USING (true);
CREATE POLICY "Public read access" ON atc FOR SELECT USING (true);
CREATE POLICY "Public read access" ON ndc_code FOR SELECT USING (true);
CREATE POLICY "Public read access" ON product_ndc FOR SELECT USING (true);
CREATE POLICY "Public read access" ON srlc FOR SELECT USING (true);
CREATE POLICY "Public read access" ON substance_srlc FOR SELECT USING (true);
CREATE POLICY "Public read access" ON substance_rxnorm FOR SELECT USING (true);
CREATE POLICY "Public read access" ON substance_snomed_pt FOR SELECT USING (true);
CREATE POLICY "Public read access" ON substance_atc FOR SELECT USING (true);
CREATE POLICY "Public read access" ON product_ae FOR SELECT USING (true);
CREATE POLICY "Public read access" ON product_ind FOR SELECT USING (true);
