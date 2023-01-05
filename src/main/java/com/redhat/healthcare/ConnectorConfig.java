package com.redhat.healthcare;

public class ConnectorConfig {
    private Boolean fhirConnector=false;

    public Boolean isFhirConnector() {
        return this.fhirConnector;
    }

    public Boolean getFhirConnector() {
        return this.fhirConnector;
    }

    public void setFhirConnector(Boolean fhirConnector) {
        this.fhirConnector = fhirConnector;
    }

    public Boolean isHl7Connector() {
        return this.hl7Connector;
    }

    public Boolean getHl7Connector() {
        return this.hl7Connector;
    }

    public void setHl7Connector(Boolean hl7Connector) {
        this.hl7Connector = hl7Connector;
    }

    public Boolean isBlueButtonConnector() {
        return this.blueButtonConnector;
    }

    public Boolean getBlueButtonConnector() {
        return this.blueButtonConnector;
    }

    public void setBlueButtonConnector(Boolean blueButtonConnector) {
        this.blueButtonConnector = blueButtonConnector;
    }

    public Boolean isHIDN() {
        return this.hIDN;
    }

    public Boolean getHIDN() {
        return this.hIDN;
    }

    public void setHIDN(Boolean hIDN) {
        this.hIDN = hIDN;
    }
    private Boolean hl7Connector=false;
    private Boolean blueButtonConnector=false;
    private Boolean hIDN=true;
    
}
