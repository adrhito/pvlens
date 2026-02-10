package org.pvlens.webapp.om;

import java.util.Date;

/**
 * Adverse Event model
 */
public class AdverseEvent {

    private int id;
    private int substanceId;
    private String substanceName;
    private int meddraId;
    private String meddraCode;
    private String meddraTerm;
    private String meddraTermType;
    private Date labelDate;
    private boolean warning;
    private boolean blackbox;
    private boolean exactMatch;

    public AdverseEvent() {}

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSubstanceId() {
        return substanceId;
    }

    public void setSubstanceId(int substanceId) {
        this.substanceId = substanceId;
    }

    public String getSubstanceName() {
        return substanceName;
    }

    public void setSubstanceName(String substanceName) {
        this.substanceName = substanceName;
    }

    public int getMeddraId() {
        return meddraId;
    }

    public void setMeddraId(int meddraId) {
        this.meddraId = meddraId;
    }

    public String getMeddraCode() {
        return meddraCode;
    }

    public void setMeddraCode(String meddraCode) {
        this.meddraCode = meddraCode;
    }

    public String getMeddraTerm() {
        return meddraTerm;
    }

    public void setMeddraTerm(String meddraTerm) {
        this.meddraTerm = meddraTerm;
    }

    public String getMeddraTermType() {
        return meddraTermType;
    }

    public void setMeddraTermType(String meddraTermType) {
        this.meddraTermType = meddraTermType;
    }

    public Date getLabelDate() {
        return labelDate;
    }

    public void setLabelDate(Date labelDate) {
        this.labelDate = labelDate;
    }

    public boolean isWarning() {
        return warning;
    }

    public void setWarning(boolean warning) {
        this.warning = warning;
    }

    public boolean isBlackbox() {
        return blackbox;
    }

    public void setBlackbox(boolean blackbox) {
        this.blackbox = blackbox;
    }

    public boolean isExactMatch() {
        return exactMatch;
    }

    public void setExactMatch(boolean exactMatch) {
        this.exactMatch = exactMatch;
    }

    // Helper methods
    public String getSeverityLabel() {
        if (blackbox) return "Black Box";
        if (warning) return "Warning";
        return "Standard";
    }

    public String getSeverityBadgeClass() {
        if (blackbox) return "badge-red";
        if (warning) return "badge-yellow";
        return "badge-gray";
    }

    public String getMatchTypeLabel() {
        return exactMatch ? "Exact" : "NLP";
    }

    public String getMatchTypeBadgeClass() {
        return exactMatch ? "badge-green" : "badge-blue";
    }
}
