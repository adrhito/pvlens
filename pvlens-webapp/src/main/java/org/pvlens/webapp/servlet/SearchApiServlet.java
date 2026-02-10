package org.pvlens.webapp.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.pvlens.webapp.services.DatabaseService;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * REST API servlet for search autocomplete and suggestions.
 * Provides endpoints for:
 * - /api/search/suggest - Get autocomplete suggestions
 * - /api/search/spellcheck - Get spelling corrections
 */
public class SearchApiServlet extends HttpServlet {

    private DatabaseService dbService;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        dbService = DatabaseService.getInstance();
        objectMapper = new ObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");

        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                sendError(response, 400, "Missing endpoint");
                return;
            }

            String query = request.getParameter("q");
            String type = request.getParameter("type"); // substances, adverse_events, indications, all
            int limit = getIntParam(request, "limit", 10);

            if (query == null || query.trim().length() < 1) {
                out.print("[]");
                return;
            }

            query = query.trim();

            switch (pathInfo) {
                case "/suggest":
                    handleSuggest(query, type, limit, out);
                    break;
                case "/spellcheck":
                    handleSpellcheck(query, out);
                    break;
                default:
                    sendError(response, 404, "Unknown endpoint: " + pathInfo);
            }

        } catch (Exception e) {
            sendError(response, 500, "Error: " + e.getMessage());
        }
    }

    private void handleSuggest(String query, String type, int limit, PrintWriter out) throws IOException {
        List<Map<String, Object>> suggestions = new ArrayList<>();

        // Request more results from each source to ensure variety including partial matches
        int requestLimit = Math.max(limit * 2, 15);

        if (type == null || type.equals("all") || type.equals("adverse_events")) {
            // Get MedDRA term suggestions for adverse events
            List<Map<String, Object>> aeSuggestions = dbService.searchMeddraTerms(query, requestLimit);
            for (Map<String, Object> s : aeSuggestions) {
                s.put("category", "Adverse Event");
                s.put("type", "adverse_event");
                suggestions.add(s);
            }
        }

        if (type == null || type.equals("all") || type.equals("substances")) {
            // Get substance name suggestions
            List<Map<String, Object>> subSuggestions = dbService.searchSubstanceNames(query, requestLimit);
            for (Map<String, Object> s : subSuggestions) {
                s.put("category", "Substance");
                s.put("type", "substance");
                suggestions.add(s);
            }
        }

        if (type == null || type.equals("all") || type.equals("indications")) {
            // Get indication suggestions (also from MedDRA but for indications)
            List<Map<String, Object>> indSuggestions = dbService.searchIndicationTerms(query, requestLimit);
            for (Map<String, Object> s : indSuggestions) {
                s.put("category", "Indication");
                s.put("type", "indication");
                suggestions.add(s);
            }
        }

        // Sort by relevance score
        suggestions.sort((a, b) -> {
            Double scoreA = (Double) a.getOrDefault("score", 0.0);
            Double scoreB = (Double) b.getOrDefault("score", 0.0);
            int scoreComparison = scoreB.compareTo(scoreA);
            if (scoreComparison != 0) return scoreComparison;

            // Secondary sort by usage count
            Integer usageA = (Integer) a.getOrDefault("usageCount", 0);
            Integer usageB = (Integer) b.getOrDefault("usageCount", 0);
            return usageB.compareTo(usageA);
        });

        // Limit total results but ensure we return enough for a good user experience
        int maxResults = Math.max(limit * 3, 20);
        if (suggestions.size() > maxResults) {
            suggestions = suggestions.subList(0, maxResults);
        }

        out.print(objectMapper.writeValueAsString(suggestions));
    }

    private void handleSpellcheck(String query, PrintWriter out) throws IOException {
        // Get spelling suggestions using fuzzy matching
        List<Map<String, Object>> corrections = dbService.getSpellingSuggestions(query, 5);
        out.print(objectMapper.writeValueAsString(corrections));
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        response.getWriter().print(objectMapper.writeValueAsString(error));
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
}
