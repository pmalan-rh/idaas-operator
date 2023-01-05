package com.redhat.healthcare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FhirConfigSpec {

    private String fhirServerUrl = "https://fhir-server.apps.os.pietersmalan.com/fhir";
    private FHIRVersion fhirVersion = FHIRVersion.R4;
    private FHIRVendor fhirVendor = FHIRVendor.HAPIFHIR;
    private Boolean fhirProcessToFHIR = true;
    private Boolean fhirProcessTerminology = false;
    private Boolean fhirProcessBundles = false;
    private String appname = "IDAAS-Connect-FHIR";
    private Boolean redeployIntegration = false;
    private Boolean requeryResources = false;

    public Boolean isRequeryResources() {
        return this.requeryResources;
    }

    public Boolean getRequeryResources() {
        return this.requeryResources;
    }

    public void setRequeryResources(Boolean requeryResources) {
        this.requeryResources = requeryResources;
    }

    public Boolean isRedeployIntegration() {
        return this.redeployIntegration;
    }

    public Boolean getRedeployIntegration() {
        return this.redeployIntegration;
    }

    public void setRedeployIntegration(Boolean redeployIntegration) {
        this.redeployIntegration = redeployIntegration;
    }

    public List<FhirResource> fhirResources;

    public List<FhirResource> getFhirResources() {
        return this.fhirResources;
    }

    public void setFhirResources(ArrayList<FhirResource> fhirResources) {
        this.fhirResources = fhirResources;
    }

    public String getAppname() {
        return this.appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public String getProcessingType() {
        return this.processingType;
    }

    public void setProcessingType(String processingType) {
        this.processingType = processingType;
    }

    private String processingType = "data";

    public FhirConfigSpec() {

    }

    private CommonConfig commonConfig = new CommonConfig();

    public CommonConfig getCommonConfig() {
        return this.commonConfig;
    }

    public void setCommonConfig(CommonConfig commonConfig) {
        this.commonConfig = commonConfig;
    }

    public Boolean isFhirProcessToFHIR() {
        return this.fhirProcessToFHIR;
    }

    public Boolean getFhirProcessToFHIR() {
        return this.fhirProcessToFHIR;
    }

    public void setFhirProcessToFHIR(Boolean fhirProcessToFHIR) {
        this.fhirProcessToFHIR = fhirProcessToFHIR;
    }

    public Boolean isFhirProcessTerminology() {
        return this.fhirProcessTerminology;
    }

    public Boolean getFhirProcessTerminology() {
        return this.fhirProcessTerminology;
    }

    public void setFhirProcessTerminology(Boolean fhirProcessTerminology) {
        this.fhirProcessTerminology = fhirProcessTerminology;
    }

    public Boolean isFhirProcessBundles() {
        return this.fhirProcessBundles;
    }

    public Boolean getFhirProcessBundles() {
        return this.fhirProcessBundles;
    }

    public void setFhirProcessBundles(Boolean fhirProcessBundles) {
        this.fhirProcessBundles = fhirProcessBundles;
    }

    public String getFhirServerUrl() {
        return this.fhirServerUrl;
    }

    public void setFhirServerUrl(String fhirServerUrl) {
        if(!fhirServerUrl.endsWith("/"))
            fhirServerUrl=fhirServerUrl+"/";
        this.fhirServerUrl = fhirServerUrl;
    }

    public FHIRVersion getFhirVersion() {
        return this.fhirVersion;
    }

    public void setFhirVersion(FHIRVersion fhirVersion) {
        this.fhirVersion = fhirVersion;
    }

    public FHIRVendor getFhirVendor() {
        return this.fhirVendor;
    }

    public void setFhirVendor(FHIRVendor fhirVendor) {
        this.fhirVendor = fhirVendor;
    }

    public enum FHIRVersion {
        R4, R5
    };

    public enum FHIRVendor {
        HAPIFHIR, Microsoft, IBM
    };
}
