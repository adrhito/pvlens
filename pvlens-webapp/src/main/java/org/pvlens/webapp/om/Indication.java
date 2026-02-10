package org.pvlens.webapp.om;

import java.util.Date;

/**
 * Indication model
 */
public class Indication {

    private int id;
    private int substanceId;
    private String substanceName;
    private int meddraId;
    private String meddraCode;
    private String meddraTerm;
    private String meddraTermType;
    private Date labelDate;
    private boolean exactMatch;

    public Indication() {}

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

    public boolean isExactMatch() {
        return exactMatch;
    }

    public void setExactMatch(boolean exactMatch) {
        this.exactMatch = exactMatch;
    }

    // Helper methods
    public String getMatchTypeLabel() {
        return exactMatch ? "Exact" : "NLP";
    }

    public String getMatchTypeBadgeClass() {
        return exactMatch ? "badge-green" : "badge-blue";
    }
}
