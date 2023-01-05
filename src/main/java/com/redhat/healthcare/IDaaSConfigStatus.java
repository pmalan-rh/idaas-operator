package com.redhat.healthcare;

public class IDaaSConfigStatus {

    public enum State {
        CREATED,
        ALREADY_PRESENT,
        PROCESSING,
        ERROR,
        UNKNOWN
    }

    public IDaaSConfigStatus() {
    }

    private State state = State.UNKNOWN;
    private boolean error=false;
    private String message="";
    private String blueButtonConnectorName="";

    public boolean getError() {
        return this.error;
    }


    public String getBlueButtonConnectorName() {
        return this.blueButtonConnectorName;
    }

    public void setBlueButtonConnectorName(String blueButtonConnectorName) {
        this.blueButtonConnectorName = blueButtonConnectorName;
    }

    public String getFhirConnectorName() {
        return this.fhirConnectorName;
    }

    public void setFhirConnectorName(String fhirConnectorName) {
        this.fhirConnectorName = fhirConnectorName;
    }

    public String getHl7ConnectorName() {
        return this.hl7ConnectorName;
    }

    public void setHl7ConnectorName(String hl7ConnectorName) {
        this.hl7ConnectorName = hl7ConnectorName;
    }
    private String fhirConnectorName="";
    private String hl7ConnectorName="";

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
