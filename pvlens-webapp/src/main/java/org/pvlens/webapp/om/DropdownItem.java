package org.pvlens.webapp.om;

/**
 * Simple dropdown item model for select lists
 */
public class DropdownItem {

    private int id;
    private String name;

    public DropdownItem() {}

    public DropdownItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
