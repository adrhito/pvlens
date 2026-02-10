package org.pvlens.webapp.om;

/**
 * Generic terminology code model (used for RxNorm, SNOMED, ATC, MedDRA)
 */
public class TermCode {

    private int id;
    private String code;
    private String term;
    private String termType;
    private String aui;
    private String cui;
    private String source;

    public TermCode() {}

    public TermCode(String code, String term) {
        this.code = code;
        this.term = term;
    }

    public TermCode(String code, String term, String termType) {
        this.code = code;
        this.term = term;
        this.termType = termType;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getTermType() {
        return termType;
    }

    public void setTermType(String termType) {
        this.termType = termType;
    }

    public String getAui() {
        return aui;
    }

    public void setAui(String aui) {
        this.aui = aui;
    }

    public String getCui() {
        return cui;
    }

    public void setCui(String cui) {
        this.cui = cui;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return term + " (" + code + ")";
    }
}
