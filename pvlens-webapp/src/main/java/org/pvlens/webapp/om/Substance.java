package org.pvlens.webapp.om;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/**
 * Substance (Drug Product) model
 */
public class Substance {

    private int id;
    private String primaryName;
    private String sponsor;
    private String sourceType;
    private Date approvalDate;
    private Date labelDate;
    private int adverseEventCount;
    private int indicationCount;
    private List<String> ndcCodes;
    private List<TermCode> rxnormCodes;
    private List<TermCode> snomedCodes;
    private List<TermCode> atcCodes;
    private List<String> guids;

    public Substance() {
        this.ndcCodes = new ArrayList<>();
        this.rxnormCodes = new ArrayList<>();
        this.snomedCodes = new ArrayList<>();
        this.atcCodes = new ArrayList<>();
        this.guids = new ArrayList<>();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPrimaryName() {
        return primaryName;
    }

    public void setPrimaryName(String primaryName) {
        this.primaryName = primaryName;
    }

    public String getSponsor() {
        return sponsor;
    }

    public void setSponsor(String sponsor) {
        this.sponsor = sponsor;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Date getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(Date approvalDate) {
        this.approvalDate = approvalDate;
    }

    public Date getLabelDate() {
        return labelDate;
    }

    public void setLabelDate(Date labelDate) {
        this.labelDate = labelDate;
    }

    public int getAdverseEventCount() {
        return adverseEventCount;
    }

    public void setAdverseEventCount(int adverseEventCount) {
        this.adverseEventCount = adverseEventCount;
    }

    public int getIndicationCount() {
        return indicationCount;
    }

    public void setIndicationCount(int indicationCount) {
        this.indicationCount = indicationCount;
    }

    public List<String> getNdcCodes() {
        return ndcCodes;
    }

    public void setNdcCodes(List<String> ndcCodes) {
        this.ndcCodes = ndcCodes;
    }

    public List<TermCode> getRxnormCodes() {
        return rxnormCodes;
    }

    public void setRxnormCodes(List<TermCode> rxnormCodes) {
        this.rxnormCodes = rxnormCodes;
    }

    public List<TermCode> getSnomedCodes() {
        return snomedCodes;
    }

    public void setSnomedCodes(List<TermCode> snomedCodes) {
        this.snomedCodes = snomedCodes;
    }

    public List<TermCode> getAtcCodes() {
        return atcCodes;
    }

    public void setAtcCodes(List<TermCode> atcCodes) {
        this.atcCodes = atcCodes;
    }

    public List<String> getGuids() {
        return guids;
    }

    public void setGuids(List<String> guids) {
        this.guids = guids;
    }

    // Helper methods
    public String getSourceTypeLabel() {
        if (sourceType == null) return "Unknown";
        switch (sourceType.toLowerCase()) {
            case "prescription": return "Prescription";
            case "otc": return "OTC";
            case "other": return "Other";
            default: return sourceType;
        }
    }

    public String getSourceTypeBadgeClass() {
        if (sourceType == null) return "badge-gray";
        switch (sourceType.toLowerCase()) {
            case "prescription": return "badge-blue";
            case "otc": return "badge-green";
            case "other": return "badge-yellow";
            default: return "badge-gray";
        }
    }
}
