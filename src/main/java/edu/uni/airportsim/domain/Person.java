package edu.uni.airportsim.domain;

public class Person extends BaseEntity {
    private String fullName;
    private Document document;

    public Person(String id, String fullName, Document document) {
        super(id, fullName);
        this.fullName = requireText(fullName, "fullName");
        this.document = document;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = requireText(fullName, "fullName");
        setName(fullName);
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }
}
