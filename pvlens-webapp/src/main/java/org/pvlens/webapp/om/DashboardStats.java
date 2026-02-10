package org.pvlens.webapp.om;

import java.util.Date;

/**
 * Dashboard statistics model for the Overview screen
 */
public class DashboardStats {

    private int totalSubstances;
    private int totalAdverseEvents;
    private int totalIndications;
    private int totalSrlcUpdates;
    private int prescriptionDrugs;
    private int otcDrugs;
    private int otherDrugs;
    private int blackboxWarnings;
    private int standardWarnings;
    private int exactMatches;
    private int nlpMatches;
    private Date dataStartDate;
    private Date dataEndDate;
    private double processedRate;
    private double verifiedRate;
    private double anomalyRate;

    // Constructors
    public DashboardStats() {}

    // Getters and Setters
    public int getTotalSubstances() {
        return totalSubstances;
    }

    public void setTotalSubstances(int totalSubstances) {
        this.totalSubstances = totalSubstances;
    }

    public int getTotalAdverseEvents() {
        return totalAdverseEvents;
    }

    public void setTotalAdverseEvents(int totalAdverseEvents) {
        this.totalAdverseEvents = totalAdverseEvents;
    }

    public int getTotalIndications() {
        return totalIndications;
    }

    public void setTotalIndications(int totalIndications) {
        this.totalIndications = totalIndications;
    }

    public int getTotalSrlcUpdates() {
        return totalSrlcUpdates;
    }

    public void setTotalSrlcUpdates(int totalSrlcUpdates) {
        this.totalSrlcUpdates = totalSrlcUpdates;
    }

    public int getPrescriptionDrugs() {
        return prescriptionDrugs;
    }

    public void setPrescriptionDrugs(int prescriptionDrugs) {
        this.prescriptionDrugs = prescriptionDrugs;
    }

    public int getOtcDrugs() {
        return otcDrugs;
    }

    public void setOtcDrugs(int otcDrugs) {
        this.otcDrugs = otcDrugs;
    }

    public int getOtherDrugs() {
        return otherDrugs;
    }

    public void setOtherDrugs(int otherDrugs) {
        this.otherDrugs = otherDrugs;
    }

    public int getBlackboxWarnings() {
        return blackboxWarnings;
    }

    public void setBlackboxWarnings(int blackboxWarnings) {
        this.blackboxWarnings = blackboxWarnings;
    }

    public int getStandardWarnings() {
        return standardWarnings;
    }

    public void setStandardWarnings(int standardWarnings) {
        this.standardWarnings = standardWarnings;
    }

    public int getExactMatches() {
        return exactMatches;
    }

    public void setExactMatches(int exactMatches) {
        this.exactMatches = exactMatches;
    }

    public int getNlpMatches() {
        return nlpMatches;
    }

    public void setNlpMatches(int nlpMatches) {
        this.nlpMatches = nlpMatches;
    }

    public Date getDataStartDate() {
        return dataStartDate;
    }

    public void setDataStartDate(Date dataStartDate) {
        this.dataStartDate = dataStartDate;
    }

    public Date getDataEndDate() {
        return dataEndDate;
    }

    public void setDataEndDate(Date dataEndDate) {
        this.dataEndDate = dataEndDate;
    }

    public double getProcessedRate() {
        return processedRate;
    }

    public void setProcessedRate(double processedRate) {
        this.processedRate = processedRate;
    }

    public double getVerifiedRate() {
        return verifiedRate;
    }

    public void setVerifiedRate(double verifiedRate) {
        this.verifiedRate = verifiedRate;
    }

    public double getAnomalyRate() {
        return anomalyRate;
    }

    public void setAnomalyRate(double anomalyRate) {
        this.anomalyRate = anomalyRate;
    }

    // Formatted getters for display
    public String getFormattedProcessedRate() {
        return String.format("%.2f", processedRate);
    }

    public String getFormattedVerifiedRate() {
        return String.format("%.2f", verifiedRate);
    }

    public String getFormattedAnomalyRate() {
        return String.format("%.2f", anomalyRate);
    }

    public String getFormattedSubstanceCount() {
        if (totalSubstances >= 1000) {
            return String.format("%.1fk", totalSubstances / 1000.0);
        }
        return String.valueOf(totalSubstances);
    }

    public String getFormattedAdverseEventCount() {
        if (totalAdverseEvents >= 1000) {
            return String.format("%.1fk", totalAdverseEvents / 1000.0);
        }
        return String.valueOf(totalAdverseEvents);
    }
}
