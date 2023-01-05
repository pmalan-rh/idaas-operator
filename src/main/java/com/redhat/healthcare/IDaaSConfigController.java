package com.redhat.healthcare;

import org.apache.commons.lang3.ObjectUtils.Null;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.javaoperatorsdk.operator.api.*;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;

@Controller
public class IDaaSConfigController implements ResourceController<IDaaSConfig> {

    @Override
    public DeleteControl deleteResource(IDaaSConfig resource, Context<IDaaSConfig> context) {
        MixedOperation<FhirConfig, KubernetesResourceList<FhirConfig>, Resource<FhirConfig>> fhirClient = client.resources(FhirConfig.class);
        fhirClient.inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()+"-fhir").delete();
        return ResourceController.super.deleteResource(resource, context);


    }

    private final KubernetesClient client;

    public IDaaSConfigController(KubernetesClient client) {
        this.client = client;
    }

    
    @Override
    public void init(EventSourceManager eventSourceManager) {
        // TODO: fill in init
    }

    @Override
    public UpdateControl<IDaaSConfig> createOrUpdateResource(IDaaSConfig resource, Context<IDaaSConfig> context) {
        IDaaSConfigStatus status;

    
        try {
            status = new IDaaSConfigStatus();
        
            status = resource.getStatus();
            if(status==null) {
                status=new IDaaSConfigStatus();
            }
            status.setError(false);
            status.setState(IDaaSConfigStatus.State.PROCESSING);
            status.setMessage("Creating resources");
            resource.setStatus(status);
            

            if(resource.getSpec()==null) {
                resource.setSpec(new IDaaSConfigSpec());
                resource.getSpec().getCommonConfig().setKafkaBrokerURL("NOT_SET");
                resource.getSpec().setConnectorConfig(new ConnectorConfig());
                
            }

            if(resource.getSpec().getConnectorConfig()==null) {
                resource.getSpec().setConnectorConfig(new ConnectorConfig());
            }

            if(resource.getSpec().getCommonConfig().getKafkaBrokerURL().equals("NOT_SET")) {
                status = new IDaaSConfigStatus();
                status.setMessage("Common configuration not complete");
                status.setState(IDaaSConfigStatus.State.ERROR);
                status.setError(true);
                return UpdateControl.updateStatusSubResource(resource);
            }

            if(resource.getSpec().getConnectorConfig().isFhirConnector()){
                resource.getStatus().setMessage("Creating FHIRConnector");
                
                FhirConfig fhirConfig = new FhirConfig();
                fhirConfig.getMetadata().setName(resource.getMetadata().getName()+"-fhir");
                status.setFhirConnectorName(resource.getMetadata().getName()+"-fhir");
                fhirConfig.getMetadata().setNamespace(resource.getMetadata().getNamespace());
                fhirConfig.setSpec(new FhirConfigSpec());
                fhirConfig.getSpec().getCommonConfig().setKafkaBrokerURL(resource.getSpec().getCommonConfig().getKafkaBrokerURL());
                fhirConfig.getSpec().getCommonConfig().setKafkaPrefix(resource.getSpec().getCommonConfig().getKafkaPrefix());
                MixedOperation<FhirConfig, KubernetesResourceList<FhirConfig>, Resource<FhirConfig>> fhirClient = client.resources(FhirConfig.class);
                //Ignore if already exist
                try {
                    fhirClient.inNamespace(resource.getMetadata().getNamespace()).create(fhirConfig);
                    
                } catch (Exception e) {}

            }


            
            

        } catch (Exception ex) {
            ex.printStackTrace();
            status = new IDaaSConfigStatus();
            status.setMessage("Error querying API: " + ex.getMessage());
            status.setState(IDaaSConfigStatus.State.ERROR);
            status.setError(true);
        }
        resource.setStatus(status);
        return UpdateControl.updateCustomResourceAndStatus(resource);
    }
}
