package org.pvlens.webapp.services;

import org.pvlens.webapp.om.*;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Database service for PVLens data access
 * Singleton pattern for connection pooling
 */
public class DatabaseService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseService.class);
    private static DatabaseService instance;
    private BasicDataSource dataSource;

    // Database configuration (should be loaded from properties in production)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/pvlens?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root";

    private DatabaseService() {
        initDataSource();
    }

    public static synchronized DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    private void initDataSource() {
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(DB_URL);
        dataSource.setUsername(DB_USER);
        dataSource.setPassword(DB_PASS);
        dataSource.setMinIdle(5);
        dataSource.setMaxIdle(10);
        dataSource.setMaxTotal(25);
        dataSource.setMaxWaitMillis(10000);
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // ==================== Dashboard Stats ====================

    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();

        try (Connection conn = getConnection()) {
            // Total substances
            stats.setTotalSubstances(getCount(conn, "SELECT COUNT(*) FROM SUBSTANCE"));

            // Total adverse events
            stats.setTotalAdverseEvents(getCount(conn, "SELECT COUNT(*) FROM PRODUCT_AE"));

            // Total indications
            stats.setTotalIndications(getCount(conn, "SELECT COUNT(*) FROM PRODUCT_IND"));

            // Total SRLC updates
            stats.setTotalSrlcUpdates(getCount(conn, "SELECT COUNT(*) FROM SRLC"));

            // Blackbox warnings
            stats.setBlackboxWarnings(getCount(conn, "SELECT COUNT(*) FROM PRODUCT_AE WHERE BLACKBOX = 1"));

            // Standard warnings
            stats.setStandardWarnings(getCount(conn, "SELECT COUNT(*) FROM PRODUCT_AE WHERE WARNING = 1 AND BLACKBOX = 0"));

            // Exact matches
            stats.setExactMatches(getCount(conn, "SELECT COUNT(*) FROM PRODUCT_AE WHERE EXACT_MATCH = 1"));

            // NLP matches
            stats.setNlpMatches(getCount(conn, "SELECT COUNT(*) FROM PRODUCT_AE WHERE EXACT_MATCH = 0"));

            // Source type counts (case-insensitive comparison)
            stats.setPrescriptionDrugs(getCount(conn,
                "SELECT COUNT(DISTINCT s.ID) FROM SUBSTANCE s " +
                "JOIN SPL_SRCFILE sf ON s.ID = sf.PRODUCT_ID " +
                "JOIN SOURCE_TYPE st ON sf.SOURCE_TYPE_ID = st.ID " +
                "WHERE UPPER(st.SOURCE_TYPE) = 'PRESCRIPTION'"));

            stats.setOtcDrugs(getCount(conn,
                "SELECT COUNT(DISTINCT s.ID) FROM SUBSTANCE s " +
                "JOIN SPL_SRCFILE sf ON s.ID = sf.PRODUCT_ID " +
                "JOIN SOURCE_TYPE st ON sf.SOURCE_TYPE_ID = st.ID " +
                "WHERE UPPER(st.SOURCE_TYPE) = 'OTC'"));

            // Date range
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT MIN(LABEL_DATE), MAX(LABEL_DATE) FROM PRODUCT_AE WHERE LABEL_DATE IS NOT NULL");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setDataStartDate(rs.getDate(1));
                    stats.setDataEndDate(rs.getDate(2));
                }
            }

            // Calculate rates
            int total = stats.getTotalSubstances();
            if (total > 0) {
                stats.setProcessedRate(97.22); // Placeholder - calculate actual rate
                stats.setVerifiedRate(71.74);
                stats.setAnomalyRate(10.12);
            }

        } catch (SQLException e) {
            log.error("Error getting dashboard stats", e);
        }

        return stats;
    }

    private int getCount(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ==================== Substances ====================

    public List<Substance> searchSubstances(String searchTerm, String sourceType, String sortBy, String sortOrder, int offset, int limit) {
        List<Substance> substances = new ArrayList<>();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT s.ID, ");
        sql.append("(SELECT n.PRODUCT_NAME FROM NDC_CODE n ");
        sql.append(" JOIN PRODUCT_NDC pn ON n.ID = pn.NDC_ID ");
        sql.append(" WHERE pn.PRODUCT_ID = s.ID LIMIT 1) AS PRIMARY_NAME, ");
        sql.append("sf.NDA_SPONSOR, st.SOURCE_TYPE, sf.APPROVAL_DATE ");
        sql.append("FROM SUBSTANCE s ");
        sql.append("LEFT JOIN SPL_SRCFILE sf ON s.ID = sf.PRODUCT_ID ");
        sql.append("LEFT JOIN SOURCE_TYPE st ON sf.SOURCE_TYPE_ID = st.ID ");
        sql.append("LEFT JOIN PRODUCT_NDC pn ON s.ID = pn.PRODUCT_ID ");
        sql.append("LEFT JOIN NDC_CODE n ON pn.NDC_ID = n.ID ");
        sql.append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (searchTerm != null && !searchTerm.isEmpty()) {
            sql.append("AND (n.PRODUCT_NAME LIKE ? OR n.NDC_CODE LIKE ?) ");
            params.add("%" + searchTerm + "%");
            params.add("%" + searchTerm + "%");
        }

        if (sourceType != null && !sourceType.equals("all")) {
            sql.append("AND UPPER(st.SOURCE_TYPE) = UPPER(?) ");
            params.add(sourceType);
        }

        // Add sorting
        String orderColumn = "PRIMARY_NAME";
        if ("type".equals(sortBy)) orderColumn = "st.SOURCE_TYPE";
        else if ("sponsor".equals(sortBy)) orderColumn = "sf.NDA_SPONSOR";
        else if ("date".equals(sortBy)) orderColumn = "sf.APPROVAL_DATE";

        sql.append("ORDER BY ").append(orderColumn);
        sql.append("desc".equalsIgnoreCase(sortOrder) ? " DESC " : " ASC ");
        sql.append("LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Substance s = new Substance();
                    int substanceId = rs.getInt("ID");
                    s.setId(substanceId);
                    String primaryName = rs.getString("PRIMARY_NAME");
                    // Use fallback if PRIMARY_NAME is null or empty
                    if (primaryName == null || primaryName.trim().isEmpty()) {
                        primaryName = getPrimaryNameForSubstance(conn, substanceId);
                    }
                    s.setPrimaryName(primaryName);
                    s.setSponsor(rs.getString("NDA_SPONSOR"));
                    s.setSourceType(rs.getString("SOURCE_TYPE"));
                    s.setApprovalDate(rs.getDate("APPROVAL_DATE"));
                    substances.add(s);
                }
            }

        } catch (SQLException e) {
            log.error("Error searching substances", e);
        }

        return substances;
    }

    public int getSubstanceCount(String searchTerm, String sourceType) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(DISTINCT s.ID) FROM SUBSTANCE s ");
        sql.append("LEFT JOIN SPL_SRCFILE sf ON s.ID = sf.PRODUCT_ID ");
        sql.append("LEFT JOIN SOURCE_TYPE st ON sf.SOURCE_TYPE_ID = st.ID ");
        sql.append("LEFT JOIN PRODUCT_NDC pn ON s.ID = pn.PRODUCT_ID ");
        sql.append("LEFT JOIN NDC_CODE n ON pn.NDC_ID = n.ID ");
        sql.append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (searchTerm != null && !searchTerm.isEmpty()) {
            sql.append("AND (n.PRODUCT_NAME LIKE ? OR n.NDC_CODE LIKE ?) ");
            params.add("%" + searchTerm + "%");
            params.add("%" + searchTerm + "%");
        }

        if (sourceType != null && !sourceType.equals("all")) {
            sql.append("AND UPPER(st.SOURCE_TYPE) = UPPER(?) ");
            params.add(sourceType);
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            log.error("Error counting substances", e);
        }

        return 0;
    }

    public Substance getSubstanceById(int id) {
        String sql = "SELECT s.ID, sf.NDA_SPONSOR, st.SOURCE_TYPE, sf.APPROVAL_DATE " +
                     "FROM SUBSTANCE s " +
                     "LEFT JOIN SPL_SRCFILE sf ON s.ID = sf.PRODUCT_ID " +
                     "LEFT JOIN SOURCE_TYPE st ON sf.SOURCE_TYPE_ID = st.ID " +
                     "WHERE s.ID = ? LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Substance s = new Substance();
                    s.setId(rs.getInt("ID"));
                    s.setSponsor(rs.getString("NDA_SPONSOR"));
                    s.setSourceType(rs.getString("SOURCE_TYPE"));
                    s.setApprovalDate(rs.getDate("APPROVAL_DATE"));

                    // Get primary name from NDC
                    s.setPrimaryName(getPrimaryNameForSubstance(conn, id));

                    return s;
                }
            }

        } catch (SQLException e) {
            log.error("Error getting substance by ID", e);
        }

        return null;
    }

    private String getPrimaryNameForSubstance(Connection conn, int substanceId) throws SQLException {
        String name = null;

        // Try 1: Get name from NDC_CODE via PRODUCT_NDC
        String ndcSql = "SELECT n.PRODUCT_NAME FROM NDC_CODE n " +
                        "JOIN PRODUCT_NDC pn ON n.ID = pn.NDC_ID " +
                        "WHERE pn.PRODUCT_ID = ? AND n.PRODUCT_NAME IS NOT NULL AND n.PRODUCT_NAME != '' " +
                        "LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(ndcSql)) {
            ps.setInt(1, substanceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    name = rs.getString(1);
                    if (name != null && !name.trim().isEmpty()) {
                        return name.trim();
                    }
                }
            }
        }

        // Try 2: Get name from RxNorm terminology
        String rxnormSql = "SELECT r.TERM FROM RXNORM r " +
                           "JOIN SUBSTANCE_RXNORM sr ON r.ID = sr.RXNORM_ID " +
                           "WHERE sr.PRODUCT_ID = ? AND r.TERM IS NOT NULL AND r.TERM != '' " +
                           "ORDER BY LENGTH(r.TERM) ASC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(rxnormSql)) {
            ps.setInt(1, substanceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    name = rs.getString(1);
                    if (name != null && !name.trim().isEmpty()) {
                        return name.trim();
                    }
                }
            }
        }

        // Try 3: Get name from SNOMED terminology
        String snomedSql = "SELECT s.TERM FROM SNOMED s " +
                           "JOIN SUBSTANCE_SNOMED_PT ss ON s.ID = ss.SNOMED_ID " +
                           "WHERE ss.PRODUCT_ID = ? AND s.TERM IS NOT NULL AND s.TERM != '' " +
                           "ORDER BY LENGTH(s.TERM) ASC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(snomedSql)) {
            ps.setInt(1, substanceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    name = rs.getString(1);
                    if (name != null && !name.trim().isEmpty()) {
                        return name.trim();
                    }
                }
            }
        }

        // Try 4: Get name from SRLC table via SUBSTANCE_SRLC
        String srlcSql = "SELECT srlc.DRUG_NAME FROM SRLC srlc " +
                         "JOIN SUBSTANCE_SRLC ss ON srlc.DRUG_ID = ss.DRUG_ID " +
                         "WHERE ss.PRODUCT_ID = ? AND srlc.DRUG_NAME IS NOT NULL AND srlc.DRUG_NAME != '' " +
                         "LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(srlcSql)) {
            ps.setInt(1, substanceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    name = rs.getString(1);
                    if (name != null && !name.trim().isEmpty()) {
                        return name.trim();
                    }
                }
            }
        }

        // Try 5: Get name from SRLC via APPLICATION_NUMBER (alternative path)
        String srlcAppSql = "SELECT srlc.DRUG_NAME FROM SRLC srlc " +
                            "JOIN SPL_SRCFILE sf ON srlc.APPLICATION_NUMBER = sf.APPLICATION_NUMBER " +
                            "WHERE sf.PRODUCT_ID = ? AND srlc.DRUG_NAME IS NOT NULL AND srlc.DRUG_NAME != '' " +
                            "LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(srlcAppSql)) {
            ps.setInt(1, substanceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    name = rs.getString(1);
                    if (name != null && !name.trim().isEmpty()) {
                        return name.trim();
                    }
                }
            }
        }

        // Try 6: Get name from ATC terminology
        String atcSql = "SELECT a.TERM FROM ATC a " +
                        "JOIN SUBSTANCE_ATC sa ON a.ID = sa.ATC_ID " +
                        "WHERE sa.PRODUCT_ID = ? AND a.TERM IS NOT NULL AND a.TERM != '' " +
                        "ORDER BY LENGTH(a.TERM) ASC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(atcSql)) {
            ps.setInt(1, substanceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    name = rs.getString(1);
                    if (name != null && !name.trim().isEmpty()) {
                        return name.trim();
                    }
                }
            }
        }

        // Try 7: Get NDA_SPONSOR from SPL_SRCFILE as a fallback identifier
        String sponsorSql = "SELECT sf.NDA_SPONSOR FROM SPL_SRCFILE sf " +
                            "WHERE sf.PRODUCT_ID = ? AND sf.NDA_SPONSOR IS NOT NULL AND sf.NDA_SPONSOR != '' " +
                            "LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sponsorSql)) {
            ps.setInt(1, substanceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String sponsor = rs.getString(1);
                    if (sponsor != null && !sponsor.trim().isEmpty()) {
                        // Use sponsor name as identifier when no drug name available
                        return sponsor.trim() + " Product";
                    }
                }
            }
        }

        // Try 8: Get name from SPL source file (using XMLFILE_NAME as last resort, extract drug name)
        String splSql = "SELECT sf.XMLFILE_NAME FROM SPL_SRCFILE sf " +
                        "WHERE sf.PRODUCT_ID = ? AND sf.XMLFILE_NAME IS NOT NULL " +
                        "LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(splSql)) {
            ps.setInt(1, substanceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String xmlFileName = rs.getString(1);
                    if (xmlFileName != null && !xmlFileName.trim().isEmpty()) {
                        // Try to extract a meaningful name from the XML filename
                        // Typically files are like "drug-name-1234.xml" or similar
                        name = extractNameFromFileName(xmlFileName);
                        if (name != null && !name.trim().isEmpty()) {
                            return name.trim();
                        }
                    }
                }
            }
        }

        return "Unknown Substance (ID: " + substanceId + ")";
    }

    /**
     * Try to extract a meaningful drug name from an XML filename.
     * Returns null if no meaningful name can be extracted.
     */
    private String extractNameFromFileName(String fileName) {
        if (fileName == null) return null;

        // Remove file extension
        String name = fileName.replaceAll("\\.(xml|XML)$", "");

        // Remove common prefixes/suffixes
        name = name.replaceAll("^(spl-|SPL-)", "");

        // Skip if the name looks like a GUID/UUID (hex characters and hyphens only)
        // Pattern matches: 483325a2-752b-F9c2-E063-6394a90ada32 or similar
        if (name.matches("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$") ||
            name.matches("(?i)^[0-9a-f-]{20,}$")) {
            return null;
        }

        // Replace underscores and hyphens with spaces
        name = name.replaceAll("[_-]", " ");

        // Remove trailing numbers/IDs
        name = name.replaceAll("\\s+\\d+$", "");

        // Skip if result is mostly hex characters (likely a partial GUID)
        String alphaOnly = name.replaceAll("[^a-zA-Z]", "");
        if (alphaOnly.length() < 3 || name.matches("(?i)^[0-9a-f\\s]+$")) {
            return null;
        }

        // Capitalize first letter of each word
        if (!name.isEmpty()) {
            String[] words = name.split("\\s+");
            StringBuilder result = new StringBuilder();
            for (String word : words) {
                if (!word.isEmpty()) {
                    if (result.length() > 0) result.append(" ");
                    result.append(Character.toUpperCase(word.charAt(0)));
                    if (word.length() > 1) {
                        result.append(word.substring(1).toLowerCase());
                    }
                }
            }
            return result.toString();
        }

        return null;
    }

    public List<Substance> getRecentSubstances(int limit) {
        return searchSubstances("", "all", "name", "asc", 0, limit);
    }

    public List<DropdownItem> getAllSubstancesForDropdown() {
        List<DropdownItem> items = new ArrayList<>();
        items.add(new DropdownItem(0, "All Substances"));

        String sql = "SELECT DISTINCT s.ID, " +
                     "(SELECT n.PRODUCT_NAME FROM NDC_CODE n " +
                     " JOIN PRODUCT_NDC pn ON n.ID = pn.NDC_ID " +
                     " WHERE pn.PRODUCT_ID = s.ID LIMIT 1) AS NAME " +
                     "FROM SUBSTANCE s " +
                     "ORDER BY NAME LIMIT 100";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int substanceId = rs.getInt("ID");
                String name = rs.getString("NAME");
                // Use fallback if name is null or empty
                if (name == null || name.trim().isEmpty()) {
                    name = getPrimaryNameForSubstance(conn, substanceId);
                }
                if (name != null && !name.trim().isEmpty()) {
                    items.add(new DropdownItem(substanceId, name));
                }
            }

        } catch (SQLException e) {
            log.error("Error getting substances for dropdown", e);
        }

        return items;
    }

    // ==================== Adverse Events ====================

    public List<AdverseEvent> searchAdverseEvents(String searchTerm, int substanceId,
                                                   String severity, String matchType,
                                                   String sortBy, String sortOrder,
                                                   int offset, int limit) {
        List<AdverseEvent> events = new ArrayList<>();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ae.ID, ae.PRODUCT_ID, ae.MEDDRA_ID, ae.LABEL_DATE, ");
        sql.append("ae.WARNING, ae.BLACKBOX, ae.EXACT_MATCH, ");
        sql.append("m.MEDDRA_CODE, m.MEDDRA_TERM, m.MEDDRA_TTY, ");
        sql.append("(SELECT n.PRODUCT_NAME FROM NDC_CODE n ");
        sql.append(" JOIN PRODUCT_NDC pn ON n.ID = pn.NDC_ID ");
        sql.append(" WHERE pn.PRODUCT_ID = ae.PRODUCT_ID LIMIT 1) AS SUBSTANCE_NAME ");
        sql.append("FROM PRODUCT_AE ae ");
        sql.append("JOIN MEDDRA m ON ae.MEDDRA_ID = m.ID ");
        sql.append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (searchTerm != null && !searchTerm.isEmpty()) {
            sql.append("AND m.MEDDRA_TERM LIKE ? ");
            params.add("%" + searchTerm + "%");
        }

        if (substanceId > 0) {
            sql.append("AND ae.PRODUCT_ID = ? ");
            params.add(substanceId);
        }

        if (severity != null && !severity.equals("all")) {
            if (severity.equals("blackbox")) {
                sql.append("AND ae.BLACKBOX = 1 ");
            } else if (severity.equals("warning")) {
                sql.append("AND ae.WARNING = 1 AND ae.BLACKBOX = 0 ");
            }
        }

        if (matchType != null && !matchType.equals("all")) {
            if (matchType.equals("exact")) {
                sql.append("AND ae.EXACT_MATCH = 1 ");
            } else if (matchType.equals("nlp")) {
                sql.append("AND ae.EXACT_MATCH = 0 ");
            }
        }

        // Add sorting
        String orderColumn = "ae.LABEL_DATE";
        if ("term".equals(sortBy)) orderColumn = "m.MEDDRA_TERM";
        else if ("code".equals(sortBy)) orderColumn = "m.MEDDRA_CODE";
        else if ("substance".equals(sortBy)) orderColumn = "SUBSTANCE_NAME";

        sql.append("ORDER BY ").append(orderColumn);
        sql.append("desc".equalsIgnoreCase(sortOrder) ? " DESC " : " ASC ");
        sql.append("LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AdverseEvent ae = new AdverseEvent();
                    ae.setId(rs.getInt("ID"));
                    ae.setSubstanceId(rs.getInt("PRODUCT_ID"));
                    ae.setSubstanceName(rs.getString("SUBSTANCE_NAME"));
                    ae.setMeddraId(rs.getInt("MEDDRA_ID"));
                    ae.setMeddraCode(rs.getString("MEDDRA_CODE"));
                    ae.setMeddraTerm(rs.getString("MEDDRA_TERM"));
                    ae.setMeddraTermType(rs.getString("MEDDRA_TTY"));
                    ae.setLabelDate(rs.getDate("LABEL_DATE"));
                    ae.setWarning(rs.getBoolean("WARNING"));
                    ae.setBlackbox(rs.getBoolean("BLACKBOX"));
                    ae.setExactMatch(rs.getBoolean("EXACT_MATCH"));
                    events.add(ae);
                }
            }

        } catch (SQLException e) {
            log.error("Error searching adverse events", e);
        }

        return events;
    }

    public int getAdverseEventCount(String searchTerm, int substanceId, String severity, String matchType) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM PRODUCT_AE ae ");
        sql.append("JOIN MEDDRA m ON ae.MEDDRA_ID = m.ID ");
        sql.append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (searchTerm != null && !searchTerm.isEmpty()) {
            sql.append("AND m.MEDDRA_TERM LIKE ? ");
            params.add("%" + searchTerm + "%");
        }

        if (substanceId > 0) {
            sql.append("AND ae.PRODUCT_ID = ? ");
            params.add(substanceId);
        }

        if (severity != null && !severity.equals("all")) {
            if (severity.equals("blackbox")) {
                sql.append("AND ae.BLACKBOX = 1 ");
            } else if (severity.equals("warning")) {
                sql.append("AND ae.WARNING = 1 AND ae.BLACKBOX = 0 ");
            }
        }

        if (matchType != null && !matchType.equals("all")) {
            if (matchType.equals("exact")) {
                sql.append("AND ae.EXACT_MATCH = 1 ");
            } else if (matchType.equals("nlp")) {
                sql.append("AND ae.EXACT_MATCH = 0 ");
            }
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            log.error("Error counting adverse events", e);
        }

        return 0;
    }

    public List<AdverseEvent> getRecentAdverseEvents(int limit) {
        return searchAdverseEvents("", 0, "all", "all", "date", "desc", 0, limit);
    }

    public List<AdverseEvent> getAdverseEventsForSubstance(int substanceId, int limit) {
        return searchAdverseEvents("", substanceId, "all", "all", "date", "desc", 0, limit);
    }

    public int getAdverseEventCountForSubstance(int substanceId) {
        return getAdverseEventCount("", substanceId, "all", "all");
    }

    // ==================== Indications ====================

    public List<Indication> searchIndications(String searchTerm, int substanceId,
                                               String matchType, String sortBy, String sortOrder,
                                               int offset, int limit) {
        List<Indication> indications = new ArrayList<>();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ind.ID, ind.PRODUCT_ID, ind.MEDDRA_ID, ind.LABEL_DATE, ");
        sql.append("ind.EXACT_MATCH, m.MEDDRA_CODE, m.MEDDRA_TERM, m.MEDDRA_TTY, ");
        sql.append("(SELECT n.PRODUCT_NAME FROM NDC_CODE n ");
        sql.append(" JOIN PRODUCT_NDC pn ON n.ID = pn.NDC_ID ");
        sql.append(" WHERE pn.PRODUCT_ID = ind.PRODUCT_ID LIMIT 1) AS SUBSTANCE_NAME ");
        sql.append("FROM PRODUCT_IND ind ");
        sql.append("JOIN MEDDRA m ON ind.MEDDRA_ID = m.ID ");
        sql.append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (searchTerm != null && !searchTerm.isEmpty()) {
            sql.append("AND m.MEDDRA_TERM LIKE ? ");
            params.add("%" + searchTerm + "%");
        }

        if (substanceId > 0) {
            sql.append("AND ind.PRODUCT_ID = ? ");
            params.add(substanceId);
        }

        if (matchType != null && !matchType.equals("all")) {
            if (matchType.equals("exact")) {
                sql.append("AND ind.EXACT_MATCH = 1 ");
            } else if (matchType.equals("nlp")) {
                sql.append("AND ind.EXACT_MATCH = 0 ");
            }
        }

        // Add sorting
        String orderColumn = "ind.LABEL_DATE";
        if ("term".equals(sortBy)) orderColumn = "m.MEDDRA_TERM";
        else if ("code".equals(sortBy)) orderColumn = "m.MEDDRA_CODE";
        else if ("substance".equals(sortBy)) orderColumn = "SUBSTANCE_NAME";

        sql.append("ORDER BY ").append(orderColumn);
        sql.append("desc".equalsIgnoreCase(sortOrder) ? " DESC " : " ASC ");
        sql.append("LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Indication ind = new Indication();
                    ind.setId(rs.getInt("ID"));
                    ind.setSubstanceId(rs.getInt("PRODUCT_ID"));
                    ind.setSubstanceName(rs.getString("SUBSTANCE_NAME"));
                    ind.setMeddraId(rs.getInt("MEDDRA_ID"));
                    ind.setMeddraCode(rs.getString("MEDDRA_CODE"));
                    ind.setMeddraTerm(rs.getString("MEDDRA_TERM"));
                    ind.setMeddraTermType(rs.getString("MEDDRA_TTY"));
                    ind.setLabelDate(rs.getDate("LABEL_DATE"));
                    ind.setExactMatch(rs.getBoolean("EXACT_MATCH"));
                    indications.add(ind);
                }
            }

        } catch (SQLException e) {
            log.error("Error searching indications", e);
        }

        return indications;
    }

    public int getIndicationCount(String searchTerm, int substanceId, String matchType) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM PRODUCT_IND ind ");
        sql.append("JOIN MEDDRA m ON ind.MEDDRA_ID = m.ID ");
        sql.append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (searchTerm != null && !searchTerm.isEmpty()) {
            sql.append("AND m.MEDDRA_TERM LIKE ? ");
            params.add("%" + searchTerm + "%");
        }

        if (substanceId > 0) {
            sql.append("AND ind.PRODUCT_ID = ? ");
            params.add(substanceId);
        }

        if (matchType != null && !matchType.equals("all")) {
            if (matchType.equals("exact")) {
                sql.append("AND ind.EXACT_MATCH = 1 ");
            } else if (matchType.equals("nlp")) {
                sql.append("AND ind.EXACT_MATCH = 0 ");
            }
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            log.error("Error counting indications", e);
        }

        return 0;
    }

    public List<Indication> getIndicationsForSubstance(int substanceId, int limit) {
        return searchIndications("", substanceId, "all", "date", "desc", 0, limit);
    }

    public int getIndicationCountForSubstance(int substanceId) {
        return getIndicationCount("", substanceId, "all");
    }

    // ==================== SRLC ====================

    public List<SrlcUpdate> searchSrlcUpdates(String searchTerm, String year,
                                               int offset, int limit) {
        List<SrlcUpdate> updates = new ArrayList<>();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ID, DRUG_ID, APPLICATION_NUMBER, DRUG_NAME, ");
        sql.append("ACTIVE_INGREDIENT, SUPPLEMENT_DATE, DATABASE_UPDATED, URL ");
        sql.append("FROM SRLC ");
        sql.append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (searchTerm != null && !searchTerm.isEmpty()) {
            sql.append("AND (DRUG_NAME LIKE ? OR ACTIVE_INGREDIENT LIKE ? OR APPLICATION_NUMBER LIKE ?) ");
            params.add("%" + searchTerm + "%");
            params.add("%" + searchTerm + "%");
            params.add("%" + searchTerm + "%");
        }

        if (year != null && !year.isEmpty() && !"all".equals(year)) {
            sql.append("AND YEAR(SUPPLEMENT_DATE) = ? ");
            params.add(Integer.parseInt(year));
        }

        sql.append("ORDER BY SUPPLEMENT_DATE DESC ");
        sql.append("LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SrlcUpdate srlc = new SrlcUpdate();
                    srlc.setId(rs.getInt("ID"));
                    srlc.setDrugId(rs.getInt("DRUG_ID"));
                    srlc.setApplicationNumber(rs.getInt("APPLICATION_NUMBER"));
                    srlc.setDrugName(rs.getString("DRUG_NAME"));
                    srlc.setActiveIngredient(rs.getString("ACTIVE_INGREDIENT"));
                    srlc.setSupplementDate(rs.getDate("SUPPLEMENT_DATE"));
                    srlc.setDatabaseUpdated(rs.getDate("DATABASE_UPDATED"));
                    srlc.setUrl(rs.getString("URL"));
                    updates.add(srlc);
                }
            }

        } catch (SQLException e) {
            log.error("Error searching SRLC updates", e);
        }

        return updates;
    }

    public int getSrlcCount(String searchTerm, String year) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM SRLC WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (searchTerm != null && !searchTerm.isEmpty()) {
            sql.append("AND (DRUG_NAME LIKE ? OR ACTIVE_INGREDIENT LIKE ? OR APPLICATION_NUMBER LIKE ?) ");
            params.add("%" + searchTerm + "%");
            params.add("%" + searchTerm + "%");
            params.add("%" + searchTerm + "%");
        }

        if (year != null && !year.isEmpty() && !"all".equals(year)) {
            sql.append("AND YEAR(SUPPLEMENT_DATE) = ? ");
            params.add(Integer.parseInt(year));
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            log.error("Error counting SRLC updates", e);
        }

        return 0;
    }

    public List<SrlcUpdate> getRecentSrlcUpdates(int limit) {
        return searchSrlcUpdates("", "all", 0, limit);
    }

    public List<SrlcUpdate> getSrlcForSubstance(int substanceId) {
        List<SrlcUpdate> updates = new ArrayList<>();

        String sql = "SELECT s.ID, s.DRUG_ID, s.APPLICATION_NUMBER, s.DRUG_NAME, " +
                     "s.ACTIVE_INGREDIENT, s.SUPPLEMENT_DATE, s.DATABASE_UPDATED, s.URL " +
                     "FROM SRLC s " +
                     "JOIN SUBSTANCE_SRLC ss ON s.DRUG_ID = ss.DRUG_ID " +
                     "WHERE ss.PRODUCT_ID = ? " +
                     "ORDER BY s.SUPPLEMENT_DATE DESC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, substanceId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SrlcUpdate srlc = new SrlcUpdate();
                    srlc.setId(rs.getInt("ID"));
                    srlc.setDrugId(rs.getInt("DRUG_ID"));
                    srlc.setApplicationNumber(rs.getInt("APPLICATION_NUMBER"));
                    srlc.setDrugName(rs.getString("DRUG_NAME"));
                    srlc.setActiveIngredient(rs.getString("ACTIVE_INGREDIENT"));
                    srlc.setSupplementDate(rs.getDate("SUPPLEMENT_DATE"));
                    srlc.setDatabaseUpdated(rs.getDate("DATABASE_UPDATED"));
                    srlc.setUrl(rs.getString("URL"));
                    updates.add(srlc);
                }
            }

        } catch (SQLException e) {
            log.error("Error getting SRLC for substance", e);
        }

        return updates;
    }

    // ==================== Terminology Lookups ====================

    public List<TermCode> getNdcCodesForSubstance(int substanceId) {
        List<TermCode> codes = new ArrayList<>();

        String sql = "SELECT n.ID, n.NDC_CODE, n.PRODUCT_NAME " +
                     "FROM NDC_CODE n " +
                     "JOIN PRODUCT_NDC pn ON n.ID = pn.NDC_ID " +
                     "WHERE pn.PRODUCT_ID = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, substanceId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TermCode tc = new TermCode();
                    tc.setId(rs.getInt("ID"));
                    tc.setCode(rs.getString("NDC_CODE"));
                    tc.setTerm(rs.getString("PRODUCT_NAME"));
                    tc.setSource("NDC");
                    codes.add(tc);
                }
            }

        } catch (SQLException e) {
            log.error("Error getting NDC codes for substance", e);
        }

        return codes;
    }

    public List<TermCode> getRxNormForSubstance(int substanceId) {
        List<TermCode> codes = new ArrayList<>();

        String sql = "SELECT r.ID, r.CODE, r.TERM, r.TTY, r.AUI, r.CUI " +
                     "FROM RXNORM r " +
                     "JOIN SUBSTANCE_RXNORM sr ON r.ID = sr.RXNORM_ID " +
                     "WHERE sr.PRODUCT_ID = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, substanceId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TermCode tc = new TermCode();
                    tc.setId(rs.getInt("ID"));
                    tc.setCode(rs.getString("CODE"));
                    tc.setTerm(rs.getString("TERM"));
                    tc.setTermType(rs.getString("TTY"));
                    tc.setAui(rs.getString("AUI"));
                    tc.setCui(rs.getString("CUI"));
                    tc.setSource("RxNorm");
                    codes.add(tc);
                }
            }

        } catch (SQLException e) {
            log.error("Error getting RxNorm for substance", e);
        }

        return codes;
    }

    public List<TermCode> getSnomedForSubstance(int substanceId) {
        List<TermCode> codes = new ArrayList<>();

        String sql = "SELECT s.ID, s.CODE, s.TERM, s.TTY, s.AUI, s.CUI " +
                     "FROM SNOMED s " +
                     "JOIN SUBSTANCE_SNOMED_PT ss ON s.ID = ss.SNOMED_ID " +
                     "WHERE ss.PRODUCT_ID = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, substanceId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TermCode tc = new TermCode();
                    tc.setId(rs.getInt("ID"));
                    tc.setCode(rs.getString("CODE"));
                    tc.setTerm(rs.getString("TERM"));
                    tc.setTermType(rs.getString("TTY"));
                    tc.setAui(rs.getString("AUI"));
                    tc.setCui(rs.getString("CUI"));
                    tc.setSource("SNOMED");
                    codes.add(tc);
                }
            }

        } catch (SQLException e) {
            log.error("Error getting SNOMED for substance", e);
        }

        return codes;
    }

    public List<TermCode> getAtcForSubstance(int substanceId) {
        List<TermCode> codes = new ArrayList<>();

        String sql = "SELECT a.ID, a.CODE, a.TERM, a.TTY, a.AUI, a.CUI " +
                     "FROM ATC a " +
                     "JOIN SUBSTANCE_ATC sa ON a.ID = sa.ATC_ID " +
                     "WHERE sa.PRODUCT_ID = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, substanceId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TermCode tc = new TermCode();
                    tc.setId(rs.getInt("ID"));
                    tc.setCode(rs.getString("CODE"));
                    tc.setTerm(rs.getString("TERM"));
                    tc.setTermType(rs.getString("TTY"));
                    tc.setAui(rs.getString("AUI"));
                    tc.setCui(rs.getString("CUI"));
                    tc.setSource("ATC");
                    codes.add(tc);
                }
            }

        } catch (SQLException e) {
            log.error("Error getting ATC for substance", e);
        }

        return codes;
    }

    public List<TermCode> getSourceFilesForSubstance(int substanceId) {
        List<TermCode> files = new ArrayList<>();

        String sql = "SELECT sf.ID, sf.GUID, sf.XMLFILE_NAME, sf.APPLICATION_NUMBER " +
                     "FROM SPL_SRCFILE sf " +
                     "WHERE sf.PRODUCT_ID = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, substanceId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TermCode tc = new TermCode();
                    tc.setId(rs.getInt("ID"));
                    tc.setCode(rs.getString("GUID"));
                    tc.setTerm(rs.getString("XMLFILE_NAME"));
                    tc.setSource("SPL");
                    files.add(tc);
                }
            }

        } catch (SQLException e) {
            log.error("Error getting source files for substance", e);
        }

        return files;
    }

    // ==================== Synonym Support ====================

    /**
     * Get synonyms for a search term using the comprehensive medical synonym dictionary.
     * Returns a list of related medical terms to expand the search.
     */
    private List<String> getSynonyms(String term) {
        return MedicalSynonymDictionary.getSynonyms(term, 10);
    }

    // ==================== Search Autocomplete & Fuzzy Matching ====================

    /**
     * Search MedDRA terms for autocomplete with fuzzy matching and synonym expansion.
     * Returns terms that match the query with relevance scoring.
     *
     * Matching priority:
     * 1. Exact matches (highest score)
     * 2. Starts-with matches
     * 3. Contains as word (e.g., "acute pain" for "pain") - IMPORTANT for partial matching
     * 4. Contains anywhere
     * 5. Phonetic matches (SOUNDEX)
     * 6. Synonym matches
     *
     * This method ensures partial matches like "acute pain" appear when searching "pain".
     */
    public List<Map<String, Object>> searchMeddraTerms(String query, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();

        // Get synonyms for expanded search (up to 10 synonyms)
        List<String> synonyms = getSynonyms(lowerQuery);
        int maxSynonyms = Math.min(synonyms.size(), 10);

        // Build dynamic SQL with synonym support
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT m.ID, m.MEDDRA_CODE, m.MEDDRA_TERM, m.MEDDRA_TTY, ");
        sql.append("CASE ");
        sql.append("  WHEN LOWER(m.MEDDRA_TERM) = ? THEN 100 ");           // exact match
        sql.append("  WHEN LOWER(m.MEDDRA_TERM) LIKE ? THEN 95 ");         // starts with query
        sql.append("  WHEN LOWER(m.MEDDRA_TERM) LIKE ? THEN 90 ");         // ends with query (e.g., "Acute pain" when searching "pain")
        sql.append("  WHEN LOWER(m.MEDDRA_TERM) LIKE ? THEN 85 ");         // word boundary - space before (e.g., "xxx pain")
        sql.append("  WHEN LOWER(m.MEDDRA_TERM) LIKE ? THEN 80 ");         // word boundary - space after (e.g., "pain xxx")
        sql.append("  WHEN LOWER(m.MEDDRA_TERM) LIKE ? THEN 75 ");         // contains query anywhere
        sql.append("  WHEN SOUNDEX(m.MEDDRA_TERM) = SOUNDEX(?) THEN 60 "); // phonetic match

        // Add scoring for synonym matches
        int synonymScore = 55;
        for (int i = 0; i < maxSynonyms; i++) {
            sql.append("  WHEN LOWER(m.MEDDRA_TERM) LIKE ? THEN ").append(Math.max(synonymScore - i * 2, 35)).append(" ");
        }

        sql.append("  ELSE 30 ");
        sql.append("END as relevance, ");
        sql.append("(SELECT COUNT(*) FROM PRODUCT_AE ae WHERE ae.MEDDRA_ID = m.ID) as usage_count ");
        sql.append("FROM MEDDRA m ");
        sql.append("WHERE (");
        sql.append("  LOWER(m.MEDDRA_TERM) = ? ");             // exact match
        sql.append("  OR LOWER(m.MEDDRA_TERM) LIKE ? ");       // starts with
        sql.append("  OR LOWER(m.MEDDRA_TERM) LIKE ? ");       // ends with
        sql.append("  OR LOWER(m.MEDDRA_TERM) LIKE ? ");       // word boundary - space before
        sql.append("  OR LOWER(m.MEDDRA_TERM) LIKE ? ");       // word boundary - space after
        sql.append("  OR LOWER(m.MEDDRA_TERM) LIKE ? ");       // contains
        sql.append("  OR SOUNDEX(m.MEDDRA_TERM) = SOUNDEX(?) ");

        // Add WHERE clauses for synonyms
        for (int i = 0; i < maxSynonyms; i++) {
            sql.append("  OR LOWER(m.MEDDRA_TERM) LIKE ? ");
        }

        sql.append(") ");
        sql.append("ORDER BY relevance DESC, usage_count DESC, LENGTH(m.MEDDRA_TERM) ASC, m.MEDDRA_TERM ASC ");
        sql.append("LIMIT ?");

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;

            // CASE scoring parameters
            ps.setString(paramIndex++, lowerQuery);                    // exact match
            ps.setString(paramIndex++, lowerQuery + "%");              // starts with
            ps.setString(paramIndex++, "%" + lowerQuery);              // ends with
            ps.setString(paramIndex++, "% " + lowerQuery + "%");       // word boundary (space before)
            ps.setString(paramIndex++, "%" + lowerQuery + " %");       // word boundary (space after)
            ps.setString(paramIndex++, "%" + lowerQuery + "%");        // contains
            ps.setString(paramIndex++, query);                         // soundex

            // Synonym scoring parameters
            for (int i = 0; i < maxSynonyms; i++) {
                ps.setString(paramIndex++, "%" + synonyms.get(i).toLowerCase() + "%");
            }

            // WHERE clause parameters (same order as CASE)
            ps.setString(paramIndex++, lowerQuery);                    // exact match
            ps.setString(paramIndex++, lowerQuery + "%");              // starts with
            ps.setString(paramIndex++, "%" + lowerQuery);              // ends with
            ps.setString(paramIndex++, "% " + lowerQuery + "%");       // word boundary (space before)
            ps.setString(paramIndex++, "%" + lowerQuery + " %");       // word boundary (space after)
            ps.setString(paramIndex++, "%" + lowerQuery + "%");        // contains
            ps.setString(paramIndex++, query);                         // soundex

            // Synonym WHERE parameters
            for (int i = 0; i < maxSynonyms; i++) {
                ps.setString(paramIndex++, "%" + synonyms.get(i).toLowerCase() + "%");
            }

            // Get more results to ensure variety (3x requested limit)
            ps.setInt(paramIndex, Math.max(limit * 3, 30));

            try (ResultSet rs = ps.executeQuery()) {
                Set<String> seenTerms = new HashSet<>();
                while (rs.next()) {
                    String term = rs.getString("MEDDRA_TERM");
                    // Avoid duplicate terms
                    if (seenTerms.contains(term.toLowerCase())) {
                        continue;
                    }
                    seenTerms.add(term.toLowerCase());

                    Map<String, Object> result = new HashMap<>();
                    result.put("id", rs.getInt("ID"));
                    result.put("code", rs.getString("MEDDRA_CODE"));
                    result.put("term", term);
                    result.put("termType", rs.getString("MEDDRA_TTY"));
                    result.put("score", rs.getDouble("relevance"));
                    result.put("usageCount", rs.getInt("usage_count"));
                    results.add(result);

                    if (results.size() >= limit) {
                        break;
                    }
                }
            }

        } catch (SQLException e) {
            log.error("Error searching MedDRA terms", e);
        }

        return results;
    }

    /**
     * Search substance/product names for autocomplete.
     */
    public List<Map<String, Object>> searchSubstanceNames(String query, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        String sql = "SELECT DISTINCT n.ID, n.PRODUCT_NAME, n.NDC_CODE, " +
                     "CASE " +
                     "  WHEN LOWER(n.PRODUCT_NAME) LIKE ? THEN 90 " +
                     "  WHEN LOWER(n.PRODUCT_NAME) LIKE ? THEN 70 " +
                     "  ELSE 50 " +
                     "END as relevance " +
                     "FROM NDC_CODE n " +
                     "WHERE LOWER(n.PRODUCT_NAME) LIKE ? " +
                     "ORDER BY relevance DESC, n.PRODUCT_NAME ASC " +
                     "LIMIT ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, lowerQuery + "%");              // starts with
            ps.setString(2, "%" + lowerQuery + "%");        // contains
            ps.setString(3, "%" + lowerQuery + "%");        // where clause
            ps.setInt(4, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", rs.getInt("ID"));
                    result.put("term", rs.getString("PRODUCT_NAME"));
                    result.put("code", rs.getString("NDC_CODE"));
                    result.put("score", rs.getDouble("relevance"));
                    results.add(result);
                }
            }

        } catch (SQLException e) {
            log.error("Error searching substance names", e);
        }

        return results;
    }

    /**
     * Search indication terms for autocomplete with synonym support and improved partial matching.
     * Uses the same matching strategy as searchMeddraTerms to ensure consistency.
     */
    public List<Map<String, Object>> searchIndicationTerms(String query, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();

        // Get synonyms for expanded search (up to 10 synonyms)
        List<String> synonyms = getSynonyms(lowerQuery);
        int maxSynonyms = Math.min(synonyms.size(), 10);

        // Build dynamic SQL with synonym support
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT m.ID, m.MEDDRA_CODE, m.MEDDRA_TERM, m.MEDDRA_TTY, ");
        sql.append("CASE ");
        sql.append("  WHEN LOWER(m.MEDDRA_TERM) = ? THEN 100 ");           // exact match
        sql.append("  WHEN LOWER(m.MEDDRA_TERM) LIKE ? THEN 95 ");         // starts with
        sql.append("  WHEN LOWER(m.MEDDRA_TERM) LIKE ? THEN 90 ");         // ends with
        sql.append("  WHEN LOWER(m.MEDDRA_TERM) LIKE ? THEN 85 ");         // word boundary - space before
        sql.append("  WHEN LOWER(m.MEDDRA_TERM) LIKE ? THEN 80 ");         // word boundary - space after
        sql.append("  WHEN LOWER(m.MEDDRA_TERM) LIKE ? THEN 75 ");         // contains
        sql.append("  WHEN SOUNDEX(m.MEDDRA_TERM) = SOUNDEX(?) THEN 60 "); // phonetic

        // Add scoring for synonym matches
        int synonymScore = 55;
        for (int i = 0; i < maxSynonyms; i++) {
            sql.append("  WHEN LOWER(m.MEDDRA_TERM) LIKE ? THEN ").append(Math.max(synonymScore - i * 2, 35)).append(" ");
        }

        sql.append("  ELSE 30 ");
        sql.append("END as relevance, ");
        sql.append("(SELECT COUNT(*) FROM PRODUCT_IND ind WHERE ind.MEDDRA_ID = m.ID) as usage_count ");
        sql.append("FROM MEDDRA m ");
        sql.append("WHERE m.ID IN (SELECT DISTINCT MEDDRA_ID FROM PRODUCT_IND) ");
        sql.append("  AND (");
        sql.append("    LOWER(m.MEDDRA_TERM) = ? ");               // exact match
        sql.append("    OR LOWER(m.MEDDRA_TERM) LIKE ? ");         // starts with
        sql.append("    OR LOWER(m.MEDDRA_TERM) LIKE ? ");         // ends with
        sql.append("    OR LOWER(m.MEDDRA_TERM) LIKE ? ");         // word boundary - space before
        sql.append("    OR LOWER(m.MEDDRA_TERM) LIKE ? ");         // word boundary - space after
        sql.append("    OR LOWER(m.MEDDRA_TERM) LIKE ? ");         // contains
        sql.append("    OR SOUNDEX(m.MEDDRA_TERM) = SOUNDEX(?) ");

        // Add WHERE clauses for synonyms
        for (int i = 0; i < maxSynonyms; i++) {
            sql.append("    OR LOWER(m.MEDDRA_TERM) LIKE ? ");
        }

        sql.append("  ) ");
        sql.append("ORDER BY relevance DESC, usage_count DESC, LENGTH(m.MEDDRA_TERM) ASC, m.MEDDRA_TERM ASC ");
        sql.append("LIMIT ?");

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;

            // CASE scoring parameters
            ps.setString(paramIndex++, lowerQuery);                    // exact match
            ps.setString(paramIndex++, lowerQuery + "%");              // starts with
            ps.setString(paramIndex++, "%" + lowerQuery);              // ends with
            ps.setString(paramIndex++, "% " + lowerQuery + "%");       // word boundary (space before)
            ps.setString(paramIndex++, "%" + lowerQuery + " %");       // word boundary (space after)
            ps.setString(paramIndex++, "%" + lowerQuery + "%");        // contains
            ps.setString(paramIndex++, query);                         // soundex

            // Synonym scoring parameters
            for (int i = 0; i < maxSynonyms; i++) {
                ps.setString(paramIndex++, "%" + synonyms.get(i).toLowerCase() + "%");
            }

            // WHERE clause parameters (same order as CASE)
            ps.setString(paramIndex++, lowerQuery);                    // exact match
            ps.setString(paramIndex++, lowerQuery + "%");              // starts with
            ps.setString(paramIndex++, "%" + lowerQuery);              // ends with
            ps.setString(paramIndex++, "% " + lowerQuery + "%");       // word boundary (space before)
            ps.setString(paramIndex++, "%" + lowerQuery + " %");       // word boundary (space after)
            ps.setString(paramIndex++, "%" + lowerQuery + "%");        // contains
            ps.setString(paramIndex++, query);                         // soundex

            // Synonym WHERE parameters
            for (int i = 0; i < maxSynonyms; i++) {
                ps.setString(paramIndex++, "%" + synonyms.get(i).toLowerCase() + "%");
            }

            // Get more results to ensure variety (3x requested limit)
            ps.setInt(paramIndex, Math.max(limit * 3, 30));

            try (ResultSet rs = ps.executeQuery()) {
                Set<String> seenTerms = new HashSet<>();
                while (rs.next()) {
                    String term = rs.getString("MEDDRA_TERM");
                    // Avoid duplicate terms
                    if (seenTerms.contains(term.toLowerCase())) {
                        continue;
                    }
                    seenTerms.add(term.toLowerCase());

                    Map<String, Object> result = new HashMap<>();
                    result.put("id", rs.getInt("ID"));
                    result.put("code", rs.getString("MEDDRA_CODE"));
                    result.put("term", term);
                    result.put("termType", rs.getString("MEDDRA_TTY"));
                    result.put("score", rs.getDouble("relevance"));
                    result.put("usageCount", rs.getInt("usage_count"));
                    results.add(result);

                    if (results.size() >= limit) {
                        break;
                    }
                }
            }

        } catch (SQLException e) {
            log.error("Error searching indication terms", e);
        }

        return results;
    }

    /**
     * Get spelling suggestions using phonetic matching, edit distance, and synonyms.
     * Returns terms that sound similar to the query or are synonyms for spell checking.
     */
    public List<Map<String, Object>> getSpellingSuggestions(String query, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();

        // First, check if we have synonyms for this term and suggest those
        List<String> synonyms = getSynonyms(lowerQuery);
        if (!synonyms.isEmpty()) {
            // Find MedDRA terms that match the synonyms
            StringBuilder synonymSql = new StringBuilder();
            synonymSql.append("SELECT DISTINCT m.MEDDRA_TERM, ");
            synonymSql.append("(SELECT COUNT(*) FROM PRODUCT_AE ae WHERE ae.MEDDRA_ID = m.ID) as usage_count ");
            synonymSql.append("FROM MEDDRA m WHERE ");

            List<String> conditions = new ArrayList<>();
            for (int i = 0; i < synonyms.size() && i < 5; i++) {
                conditions.add("LOWER(m.MEDDRA_TERM) LIKE ?");
            }
            synonymSql.append(String.join(" OR ", conditions));
            synonymSql.append(" ORDER BY usage_count DESC LIMIT ?");

            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(synonymSql.toString())) {

                int paramIndex = 1;
                for (int i = 0; i < synonyms.size() && i < 5; i++) {
                    ps.setString(paramIndex++, "%" + synonyms.get(i).toLowerCase() + "%");
                }
                ps.setInt(paramIndex, limit);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next() && results.size() < limit) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("suggestion", rs.getString("MEDDRA_TERM"));
                        result.put("usageCount", rs.getInt("usage_count"));
                        result.put("isSynonym", true);
                        results.add(result);
                    }
                }

            } catch (SQLException e) {
                log.error("Error getting synonym suggestions", e);
            }
        }

        // If we don't have enough results, also try phonetic matching
        if (results.size() < limit) {
            String sql = "SELECT DISTINCT m.MEDDRA_TERM, " +
                         "SOUNDEX(m.MEDDRA_TERM) as term_soundex, " +
                         "SOUNDEX(?) as query_soundex, " +
                         "(SELECT COUNT(*) FROM PRODUCT_AE ae WHERE ae.MEDDRA_ID = m.ID) as usage_count " +
                         "FROM MEDDRA m " +
                         "WHERE SOUNDEX(m.MEDDRA_TERM) = SOUNDEX(?) " +
                         "  AND LOWER(m.MEDDRA_TERM) != LOWER(?) " +
                         "ORDER BY usage_count DESC, m.MEDDRA_TERM ASC " +
                         "LIMIT ?";

            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, query);
                ps.setString(2, query);
                ps.setString(3, query);
                ps.setInt(4, limit - results.size());

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next() && results.size() < limit) {
                        String term = rs.getString("MEDDRA_TERM");
                        // Avoid duplicates
                        boolean exists = results.stream()
                            .anyMatch(r -> term.equalsIgnoreCase((String) r.get("suggestion")));
                        if (!exists) {
                            Map<String, Object> result = new HashMap<>();
                            result.put("suggestion", term);
                            result.put("usageCount", rs.getInt("usage_count"));
                            results.add(result);
                        }
                    }
                }

            } catch (SQLException e) {
                log.error("Error getting spelling suggestions", e);
            }
        }

        return results;
    }

    /**
     * Enhanced search for adverse events with fuzzy matching and synonym support.
     * Searches for symptoms and related terms using MedDRA hierarchy.
     */
    public List<AdverseEvent> searchAdverseEventsFuzzy(String searchTerm, int substanceId,
                                                        String severity, String matchType,
                                                        String sortBy, String sortOrder,
                                                        int offset, int limit) {
        List<AdverseEvent> events = new ArrayList<>();
        String lowerSearch = searchTerm.toLowerCase().trim();

        // Get synonyms for expanded search
        List<String> synonyms = getSynonyms(lowerSearch);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ae.ID, ae.PRODUCT_ID, ae.MEDDRA_ID, ae.LABEL_DATE, ");
        sql.append("ae.WARNING, ae.BLACKBOX, ae.EXACT_MATCH, ");
        sql.append("m.MEDDRA_CODE, m.MEDDRA_TERM, m.MEDDRA_TTY, ");
        sql.append("(SELECT n.PRODUCT_NAME FROM NDC_CODE n ");
        sql.append(" JOIN PRODUCT_NDC pn ON n.ID = pn.NDC_ID ");
        sql.append(" WHERE pn.PRODUCT_ID = ae.PRODUCT_ID LIMIT 1) AS SUBSTANCE_NAME, ");
        sql.append("CASE ");
        sql.append("  WHEN LOWER(m.MEDDRA_TERM) = ? THEN 100 ");           // exact match
        sql.append("  WHEN LOWER(m.MEDDRA_TERM) LIKE ? THEN 95 ");         // starts with
        sql.append("  WHEN LOWER(m.MEDDRA_TERM) LIKE ? THEN 85 ");         // word boundary
        sql.append("  WHEN LOWER(m.MEDDRA_TERM) LIKE ? THEN 75 ");         // contains
        sql.append("  WHEN SOUNDEX(m.MEDDRA_TERM) = SOUNDEX(?) THEN 60 "); // phonetic

        // Add scoring for synonym matches
        int synonymScore = 50;
        for (int i = 0; i < synonyms.size() && i < 5; i++) {
            sql.append("  WHEN LOWER(m.MEDDRA_TERM) LIKE ? THEN ").append(synonymScore - i * 2).append(" ");
        }

        sql.append("  ELSE 30 ");
        sql.append("END as relevance ");
        sql.append("FROM PRODUCT_AE ae ");
        sql.append("JOIN MEDDRA m ON ae.MEDDRA_ID = m.ID ");
        sql.append("WHERE (");
        sql.append("  LOWER(m.MEDDRA_TERM) LIKE ? ");           // contains
        sql.append("  OR LOWER(m.MEDDRA_TERM) LIKE ? ");        // word boundary
        sql.append("  OR SOUNDEX(m.MEDDRA_TERM) = SOUNDEX(?) ");

        // Add WHERE clauses for synonyms
        for (int i = 0; i < synonyms.size() && i < 5; i++) {
            sql.append("  OR LOWER(m.MEDDRA_TERM) LIKE ? ");
        }
        sql.append(") ");

        List<Object> params = new ArrayList<>();
        // CASE scoring parameters
        params.add(lowerSearch);                    // exact
        params.add(lowerSearch + "%");              // starts with
        params.add("% " + lowerSearch + "%");       // word boundary
        params.add("%" + lowerSearch + "%");        // contains
        params.add(searchTerm);                     // soundex

        // Synonym scoring parameters
        for (int i = 0; i < synonyms.size() && i < 5; i++) {
            params.add("%" + synonyms.get(i).toLowerCase() + "%");
        }

        // WHERE clause parameters
        params.add("%" + lowerSearch + "%");        // contains
        params.add("% " + lowerSearch + "%");       // word boundary
        params.add(searchTerm);                     // soundex

        // Synonym WHERE parameters
        for (int i = 0; i < synonyms.size() && i < 5; i++) {
            params.add("%" + synonyms.get(i).toLowerCase() + "%");
        }

        if (substanceId > 0) {
            sql.append("AND ae.PRODUCT_ID = ? ");
            params.add(substanceId);
        }

        if (severity != null && !severity.equals("all")) {
            if (severity.equals("blackbox")) {
                sql.append("AND ae.BLACKBOX = 1 ");
            } else if (severity.equals("warning")) {
                sql.append("AND ae.WARNING = 1 AND ae.BLACKBOX = 0 ");
            }
        }

        if (matchType != null && !matchType.equals("all")) {
            if (matchType.equals("exact")) {
                sql.append("AND ae.EXACT_MATCH = 1 ");
            } else if (matchType.equals("nlp")) {
                sql.append("AND ae.EXACT_MATCH = 0 ");
            }
        }

        // Sort by relevance first, then by the user's chosen column
        sql.append("ORDER BY relevance DESC, ");
        String orderColumn = "ae.LABEL_DATE";
        if ("term".equals(sortBy)) orderColumn = "m.MEDDRA_TERM";
        else if ("code".equals(sortBy)) orderColumn = "m.MEDDRA_CODE";
        else if ("substance".equals(sortBy)) orderColumn = "SUBSTANCE_NAME";

        sql.append(orderColumn);
        sql.append("desc".equalsIgnoreCase(sortOrder) ? " DESC " : " ASC ");
        sql.append("LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AdverseEvent ae = new AdverseEvent();
                    ae.setId(rs.getInt("ID"));
                    ae.setSubstanceId(rs.getInt("PRODUCT_ID"));
                    ae.setSubstanceName(rs.getString("SUBSTANCE_NAME"));
                    ae.setMeddraId(rs.getInt("MEDDRA_ID"));
                    ae.setMeddraCode(rs.getString("MEDDRA_CODE"));
                    ae.setMeddraTerm(rs.getString("MEDDRA_TERM"));
                    ae.setMeddraTermType(rs.getString("MEDDRA_TTY"));
                    ae.setLabelDate(rs.getDate("LABEL_DATE"));
                    ae.setWarning(rs.getBoolean("WARNING"));
                    ae.setBlackbox(rs.getBoolean("BLACKBOX"));
                    ae.setExactMatch(rs.getBoolean("EXACT_MATCH"));
                    events.add(ae);
                }
            }

        } catch (SQLException e) {
            log.error("Error searching adverse events with fuzzy matching", e);
        }

        return events;
    }
}
