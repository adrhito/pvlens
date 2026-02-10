package org.pvlens.webapp.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.pvlens.webapp.om.*;
import org.pvlens.webapp.services.DatabaseService;
import org.pvlens.webapp.util.RecentLookups;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Simple Velocity-based servlet for PVLens dashboard.
 * Renders templates without full Turbine framework.
 */
public class PVLensServlet extends HttpServlet {

    private VelocityEngine velocityEngine;
    private DatabaseService dbService;

    @Override
    public void init() throws ServletException {
        super.init();

        // Initialize Velocity Engine
        velocityEngine = new VelocityEngine();
        String templatePath = getServletContext().getRealPath("/templates/");
        velocityEngine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, templatePath);
        velocityEngine.setProperty(RuntimeConstants.INPUT_ENCODING, "UTF-8");
        velocityEngine.setProperty("output.encoding", "UTF-8");
        velocityEngine.init();

        // Initialize Database Service
        dbService = DatabaseService.getInstance();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();
        String screen = "Overview";

        if (pathInfo != null && pathInfo.length() > 1) {
            screen = pathInfo.substring(1); // Remove leading slash
        }

        VelocityContext context = new VelocityContext();

        // Add common context
        context.put("appName", "PVLens");
        context.put("currentScreen", screen);
        context.put("link", new LinkTool(request.getContextPath()));
        context.put("date", new DateTool());

        try {
            // Populate screen-specific data
            switch (screen) {
                case "Overview":
                    populateOverview(request, context);
                    break;
                case "Substances":
                    populateSubstances(request, context);
                    break;
                case "AdverseEvents":
                    populateAdverseEvents(request, context);
                    break;
                case "Indications":
                    populateIndications(request, context);
                    break;
                case "SRLC":
                    populateSRLC(request, context);
                    break;
                case "SubstanceDetail":
                    populateSubstanceDetail(request, context);
                    break;
                default:
                    screen = "Overview";
                    populateOverview(request, context);
            }

            // Load screen template
            Template screenTemplate = velocityEngine.getTemplate("screens/" + screen + ".vm");
            java.io.StringWriter screenWriter = new java.io.StringWriter();
            screenTemplate.merge(context, screenWriter);
            context.put("screen_placeholder", screenWriter.toString());

            // Load and render layout
            Template layoutTemplate = velocityEngine.getTemplate("layouts/Default.vm");
            PrintWriter out = response.getWriter();
            layoutTemplate.merge(context, out);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            PrintWriter out = response.getWriter();
            out.println("<html><body>");
            out.println("<h1>Error rendering page</h1>");
            out.println("<pre>" + e.getMessage() + "</pre>");
            e.printStackTrace(out);
            out.println("</body></html>");
        }
    }

    private void populateOverview(HttpServletRequest request, VelocityContext context) {
        DashboardStats stats = dbService.getDashboardStats();
        context.put("stats", stats);
        context.put("recentSubstances", dbService.getRecentSubstances(5));
        context.put("recentAdverseEvents", dbService.getRecentAdverseEvents(5));
        context.put("recentSrlcUpdates", dbService.getRecentSrlcUpdates(5));

        // Add user's recently viewed items from session
        RecentLookups lookups = RecentLookups.getFromSession(request.getSession());
        context.put("recentlyViewedSubstances", lookups.getRecentSubstances(5));
        context.put("recentlyViewedAdverseEvents", lookups.getRecentAdverseEvents(5));
        context.put("hasRecentLookups",
            !lookups.getRecentSubstances().isEmpty() || !lookups.getRecentAdverseEvents().isEmpty());
    }

    private void populateSubstances(HttpServletRequest request, VelocityContext context) {
        String searchTerm = getParam(request, "search", "");
        String sourceType = getParam(request, "sourceType", "all");
        String sortBy = getParam(request, "sortBy", "name");
        String sortOrder = getParam(request, "sortOrder", "asc");
        int page = getIntParam(request, "page", 1);
        int pageSize = 25;
        int offset = (page - 1) * pageSize;

        List<Substance> substances = dbService.searchSubstances(searchTerm, sourceType, sortBy, sortOrder, offset, pageSize);
        int totalCount = dbService.getSubstanceCount(searchTerm, sourceType);
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        context.put("substances", substances);
        context.put("searchTerm", searchTerm);
        context.put("sourceType", sourceType);
        context.put("sortBy", sortBy);
        context.put("sortOrder", sortOrder);
        context.put("currentPage", page);
        context.put("totalCount", totalCount);
        context.put("totalPages", totalPages);
    }

    private void populateAdverseEvents(HttpServletRequest request, VelocityContext context) {
        String searchTerm = getParam(request, "search", "");
        int substanceId = getIntParam(request, "substanceId", 0);
        String severity = getParam(request, "severity", "all");
        String matchType = getParam(request, "matchType", "all");
        String sortBy = getParam(request, "sortBy", "date");
        String sortOrder = getParam(request, "sortOrder", "desc");
        int page = getIntParam(request, "page", 1);
        int pageSize = 50;
        int offset = (page - 1) * pageSize;

        List<AdverseEvent> adverseEvents;
        int totalCount;

        // Use fuzzy search when search term is provided for better symptom matching
        if (searchTerm != null && !searchTerm.isEmpty()) {
            adverseEvents = dbService.searchAdverseEventsFuzzy(
                searchTerm, substanceId, severity, matchType, sortBy, sortOrder, offset, pageSize);
            // For fuzzy search, we estimate count (actual fuzzy count would be expensive)
            totalCount = dbService.getAdverseEventCount(searchTerm, substanceId, severity, matchType);
        } else {
            adverseEvents = dbService.searchAdverseEvents(
                searchTerm, substanceId, severity, matchType, sortBy, sortOrder, offset, pageSize);
            totalCount = dbService.getAdverseEventCount(searchTerm, substanceId, severity, matchType);
        }

        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        context.put("adverseEvents", adverseEvents);
        context.put("searchTerm", searchTerm);
        context.put("substanceId", substanceId);
        context.put("severity", severity);
        context.put("matchType", matchType);
        context.put("sortBy", sortBy);
        context.put("sortOrder", sortOrder);
        context.put("currentPage", page);
        context.put("totalCount", totalCount);
        context.put("totalPages", totalPages);
        context.put("substanceList", dbService.getAllSubstancesForDropdown());
    }

    private void populateIndications(HttpServletRequest request, VelocityContext context) {
        String searchTerm = getParam(request, "search", "");
        int substanceId = getIntParam(request, "substanceId", 0);
        String matchType = getParam(request, "matchType", "all");
        String sortBy = getParam(request, "sortBy", "date");
        String sortOrder = getParam(request, "sortOrder", "desc");
        int page = getIntParam(request, "page", 1);
        int pageSize = 50;
        int offset = (page - 1) * pageSize;

        List<Indication> indications = dbService.searchIndications(
            searchTerm, substanceId, matchType, sortBy, sortOrder, offset, pageSize);
        int totalCount = dbService.getIndicationCount(searchTerm, substanceId, matchType);
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        context.put("indications", indications);
        context.put("searchTerm", searchTerm);
        context.put("substanceId", substanceId);
        context.put("matchType", matchType);
        context.put("sortBy", sortBy);
        context.put("sortOrder", sortOrder);
        context.put("currentPage", page);
        context.put("totalCount", totalCount);
        context.put("totalPages", totalPages);
        context.put("substanceList", dbService.getAllSubstancesForDropdown());
    }

    private void populateSRLC(HttpServletRequest request, VelocityContext context) {
        String searchTerm = getParam(request, "search", "");
        String year = getParam(request, "year", "all");
        int page = getIntParam(request, "page", 1);
        int pageSize = 25;
        int offset = (page - 1) * pageSize;

        List<SrlcUpdate> srlcUpdates = dbService.searchSrlcUpdates(searchTerm, year, offset, pageSize);
        int totalCount = dbService.getSrlcCount(searchTerm, year);
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        context.put("srlcUpdates", srlcUpdates);
        context.put("searchTerm", searchTerm);
        context.put("year", year);
        context.put("currentPage", page);
        context.put("totalCount", totalCount);
        context.put("totalPages", totalPages);

        // Generate year list
        List<Integer> yearList = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int y = currentYear; y >= currentYear - 10; y--) {
            yearList.add(y);
        }
        context.put("yearList", yearList);
    }

    private void populateSubstanceDetail(HttpServletRequest request, VelocityContext context) {
        int id = getIntParam(request, "id", 0);
        Substance substance = dbService.getSubstanceById(id);

        if (substance != null) {
            List<TermCode> rxnormCodes = dbService.getRxNormForSubstance(id);
            List<TermCode> snomedCodes = dbService.getSnomedForSubstance(id);
            List<TermCode> atcCodes = dbService.getAtcForSubstance(id);
            List<TermCode> ndcCodes = dbService.getNdcCodesForSubstance(id);

            context.put("substance", substance);
            context.put("adverseEvents", dbService.getAdverseEventsForSubstance(id, 100));
            context.put("indications", dbService.getIndicationsForSubstance(id, 100));
            context.put("rxnormCodes", rxnormCodes);
            context.put("snomedCodes", snomedCodes);
            context.put("atcCodes", atcCodes);
            context.put("ndcCodes", ndcCodes);
            context.put("adverseEventsCount", dbService.getAdverseEventCountForSubstance(id));
            context.put("indicationsCount", dbService.getIndicationCountForSubstance(id));
            context.put("termCodesCount",
                rxnormCodes.size() + snomedCodes.size() + atcCodes.size() + ndcCodes.size());

            // Track this substance view in the user's recent lookups
            RecentLookups lookups = RecentLookups.getFromSession(request.getSession());
            lookups.addSubstanceLookup(id, substance.getPrimaryName(), substance.getSourceTypeLabel());
        }
    }

    private String getParam(HttpServletRequest request, String name, String defaultValue) {
        String value = request.getParameter(name);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    private int getIntParam(HttpServletRequest request, String name, int defaultValue) {
        String value = request.getParameter(name);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Simple link tool for templates
     */
    public static class LinkTool {
        private String contextPath;

        public LinkTool(String contextPath) {
            this.contextPath = contextPath;
        }

        public LinkBuilder setScreen(String screen) {
            return new LinkBuilder(contextPath, screen);
        }

        public String setPage(String page) {
            return contextPath + "/" + page;
        }
    }

    public static class LinkBuilder {
        private String contextPath;
        private String screen;
        private Map<String, String> params = new LinkedHashMap<>();

        public LinkBuilder(String contextPath, String screen) {
            this.contextPath = contextPath;
            this.screen = screen;
        }

        public LinkBuilder addPathInfo(String key, Object value) {
            if (value != null) {
                params.put(key, String.valueOf(value));
            }
            return this;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(contextPath).append("/app/").append(screen);
            if (!params.isEmpty()) {
                sb.append("?");
                boolean first = true;
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (!first) sb.append("&");
                    sb.append(entry.getKey()).append("=").append(entry.getValue());
                    first = false;
                }
            }
            return sb.toString();
        }
    }

    /**
     * Simple date formatting tool for templates
     */
    public static class DateTool {
        public String format(String pattern, Date date) {
            if (date == null) return "";
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            return sdf.format(date);
        }
    }
}
