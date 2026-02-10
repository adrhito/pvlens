package org.pvlens.webapp.util;

import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.*;

/**
 * Tracks recently viewed substances and adverse events for the current user session.
 * Uses a LRU (Least Recently Used) approach to maintain a fixed-size list.
 */
public class RecentLookups implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String SESSION_KEY = "recentLookups";
    private static final int MAX_RECENT_ITEMS = 10;

    private final LinkedList<LookupItem> recentSubstances = new LinkedList<>();
    private final LinkedList<LookupItem> recentAdverseEvents = new LinkedList<>();

    /**
     * Get or create the RecentLookups instance from the session.
     */
    public static RecentLookups getFromSession(HttpSession session) {
        RecentLookups lookups = (RecentLookups) session.getAttribute(SESSION_KEY);
        if (lookups == null) {
            lookups = new RecentLookups();
            session.setAttribute(SESSION_KEY, lookups);
        }
        return lookups;
    }

    /**
     * Record a substance view.
     */
    public void addSubstanceLookup(int id, String name, String type) {
        addLookup(recentSubstances, new LookupItem(id, name, type));
    }

    /**
     * Record an adverse event view.
     */
    public void addAdverseEventLookup(int id, String term, String substanceName, String severity) {
        addLookup(recentAdverseEvents, new LookupItem(id, term, substanceName, severity));
    }

    /**
     * Get the list of recently viewed substances.
     */
    public List<LookupItem> getRecentSubstances() {
        return new ArrayList<>(recentSubstances);
    }

    /**
     * Get the list of recently viewed adverse events.
     */
    public List<LookupItem> getRecentAdverseEvents() {
        return new ArrayList<>(recentAdverseEvents);
    }

    /**
     * Get a limited list of recent substances.
     */
    public List<LookupItem> getRecentSubstances(int limit) {
        List<LookupItem> result = new ArrayList<>();
        int count = 0;
        for (LookupItem item : recentSubstances) {
            if (count >= limit) break;
            result.add(item);
            count++;
        }
        return result;
    }

    /**
     * Get a limited list of recent adverse events.
     */
    public List<LookupItem> getRecentAdverseEvents(int limit) {
        List<LookupItem> result = new ArrayList<>();
        int count = 0;
        for (LookupItem item : recentAdverseEvents) {
            if (count >= limit) break;
            result.add(item);
            count++;
        }
        return result;
    }

    private void addLookup(LinkedList<LookupItem> list, LookupItem item) {
        // Remove if already exists (will be re-added at front)
        list.removeIf(existing -> existing.getId() == item.getId());

        // Add to front of list
        list.addFirst(item);

        // Trim to max size
        while (list.size() > MAX_RECENT_ITEMS) {
            list.removeLast();
        }
    }

    /**
     * Represents a recently viewed item.
     */
    public static class LookupItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private final int id;
        private final String primaryName;
        private final String secondaryInfo;
        private final String badge;
        private final long timestamp;

        public LookupItem(int id, String primaryName, String secondaryInfo) {
            this(id, primaryName, secondaryInfo, null);
        }

        public LookupItem(int id, String primaryName, String secondaryInfo, String badge) {
            this.id = id;
            this.primaryName = primaryName;
            this.secondaryInfo = secondaryInfo;
            this.badge = badge;
            this.timestamp = System.currentTimeMillis();
        }

        public int getId() {
            return id;
        }

        public String getPrimaryName() {
            return primaryName;
        }

        public String getSecondaryInfo() {
            return secondaryInfo;
        }

        public String getBadge() {
            return badge;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getTimeAgo() {
            long diff = System.currentTimeMillis() - timestamp;
            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;

            if (hours > 0) {
                return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            } else if (minutes > 0) {
                return minutes + " min" + (minutes > 1 ? "s" : "") + " ago";
            } else {
                return "Just now";
            }
        }

        public String getBadgeClass() {
            if (badge == null) return "badge-gray";
            switch (badge.toLowerCase()) {
                case "blackbox":
                case "black box":
                    return "badge-red";
                case "warning":
                    return "badge-orange";
                case "prescription":
                    return "badge-blue";
                case "otc":
                    return "badge-green";
                default:
                    return "badge-gray";
            }
        }
    }
}
