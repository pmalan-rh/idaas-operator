package com.redhat.healthcare;

public class FhirConfigStatus {

    public enum State {
        CREATED,
        ALREADY_PRESENT,
        PROCESSING,
        ERROR,
        UNKNOWN
    }

    private State state = State.UNKNOWN;
    private boolean error;
    private String message;
    private boolean knativeConfigured=false;
    private boolean fhirconfig=false;
    private boolean fhirintegration=false;

    public boolean isFhirintegration() {
        return this.fhirintegration;
    }

    public boolean getFhirintegration() {
        return this.fhirintegration;
    }

    public void setFhirintegration(boolean fhirintegration) {
        this.fhirintegration = fhirintegration;
    }

    public boolean isFhirconfig() {
        return this.fhirconfig;
    }

    public boolean getFhirconfig() {
        return this.fhirconfig;
    }

    public void setFhirconfig(boolean fhirconfig) {
        this.fhirconfig = fhirconfig;
    }

    public boolean getError() {
        return this.error;
    }


    public boolean isKnativeConfigured() {
        return this.knativeConfigured;
    }

    public boolean getKnativeConfigured() {
        return this.knativeConfigured;
    }

    public void setKnativeConfigured(boolean knativeConfigured) {
        this.knativeConfigured = knativeConfigured;
    }

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
