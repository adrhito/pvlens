package org.pvlens.webapp.om;

import java.util.Date;

/**
 * Safety-Related Label Change (SRLC) update model
 */
public class SrlcUpdate {

    private int id;
    private int drugId;
    private int applicationNumber;
    private String drugName;
    private String activeIngredient;
    private Date supplementDate;
    private Date databaseUpdated;
    private String url;

    public SrlcUpdate() {}

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDrugId() {
        return drugId;
    }

    public void setDrugId(int drugId) {
        this.drugId = drugId;
    }

    public int getApplicationNumber() {
        return applicationNumber;
    }

    public void setApplicationNumber(int applicationNumber) {
        this.applicationNumber = applicationNumber;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getActiveIngredient() {
        return activeIngredient;
    }

    public void setActiveIngredient(String activeIngredient) {
        this.activeIngredient = activeIngredient;
    }

    public Date getSupplementDate() {
        return supplementDate;
    }

    public void setSupplementDate(Date supplementDate) {
        this.supplementDate = supplementDate;
    }

    public Date getDatabaseUpdated() {
        return databaseUpdated;
    }

    public void setDatabaseUpdated(Date databaseUpdated) {
        this.databaseUpdated = databaseUpdated;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    // Helper method
    public String getFormattedApplicationNumber() {
        return String.format("NDA %06d", applicationNumber);
    }
}
