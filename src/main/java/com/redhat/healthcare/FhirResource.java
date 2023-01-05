package com.redhat.healthcare;

public class FhirResource {

    

    private String name="";

    public FhirResource(String name, String preProcessing, String postProcessing) {
        this.name = name;
        this.preProcessing = preProcessing;
        this.postProcessing = postProcessing;
    }

    public FhirResource() {
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPreProcessing() {
        return this.preProcessing;
    }

    public void setPreProcessing(String preProcessing) {
        this.preProcessing = preProcessing;
    }

    public String getPostProcessing() {
        return this.postProcessing;
    }

    public void setPostProcessing(String postProcessing) {
        this.postProcessing = postProcessing;
    }
    private String preProcessing="";
    private String postProcessing="";
    
}
