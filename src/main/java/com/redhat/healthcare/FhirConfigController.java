package com.redhat.healthcare;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.redhat.healthcare.FhirConfigSpec.FHIRVersion;

import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceComponent;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IFetchConformanceUntyped;
import io.fabric8.kubernetes.api.model.APIService;
import io.fabric8.kubernetes.api.model.APIServiceSpec;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apiextensions.v1.*;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.camelk.client.CamelKClient;
import io.fabric8.knative.client.DefaultKnativeClient;
import io.fabric8.knative.eventing.v1.Broker;
import io.fabric8.knative.eventing.v1.BrokerSpec;
import io.fabric8.knative.eventing.v1.BrokerSpecBuilder;
import io.fabric8.camelk.client.DefaultCamelKClient;
import io.fabric8.camelk.v1.SourceSpec;
import io.fabric8.camelk.v1.TraitSpec;
import io.fabric8.camelk.v1.ConfigurationSpec;
import io.fabric8.camelk.v1.Integration;
import io.fabric8.camelk.v1.IntegrationList;
import io.fabric8.camelk.v1.IntegrationSpec;
import io.javaoperatorsdk.operator.api.*;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;

@Controller
public class FhirConfigController implements ResourceController<FhirConfig> {

    private final KubernetesClient client;

    public FhirConfigController(KubernetesClient client) {
        this.client = client;
    }

    // TODO Fill in the rest of the controller

    @Override
    public void init(EventSourceManager eventSourceManager) {

    }

    @Override
    public UpdateControl<FhirConfig> createOrUpdateResource(FhirConfig resource, Context<FhirConfig> context) {
        System.out.println(resource.getSpec().getFhirVersion() + ":" + resource.getSpec().getFhirServerUrl());
        FhirConfigStatus status = resource.getStatus();
        boolean update=false;

        if (status == null) {
            status = new FhirConfigStatus();
            update=true;
        }

        
        if(!status.getFhirconfig()) {
            resource=setupFHIR(resource);
            update=true;
        }

        if(!status.getFhirintegration()) {
            resource=setupIntegration(resource);
            update=true;
        }

        // Recreate FHIR resources
        if(resource.getSpec().getRequeryResources()) {
            resource=setupFHIR(resource);
            resource.getSpec().setRedeployIntegration(false);
            update=true;
        }

        // Force redeployment
        if(resource.getSpec().getRedeployIntegration()) {
            resource=setupIntegration(resource);
            resource.getSpec().setRequeryResources(false);
            update=true;
        }

        // Create default broker for knative / cloud events
        if(!resource.getStatus().getKnativeConfigured())
        {
            resource=setupKnative(resource);
            update=true;
        }
        
        status.setState(FhirConfigStatus.State.CREATED);
        status.setError(false);
        resource.setStatus(status);
        resource.setStatus(status);
        if(update)
            return UpdateControl.updateCustomResourceAndStatus(resource);
        else
            return UpdateControl.noUpdate();

    }

    private FhirConfig setupKnative(FhirConfig resource) {
        FhirConfigStatus status = resource.getStatus();
        DefaultKnativeClient kn=new DefaultKnativeClient(client.getConfiguration());
        Broker broker=new Broker();
        broker.setMetadata(new ObjectMeta());
        broker.getMetadata().setName("processor-channel"); 
        broker.getMetadata().setNamespace(resource.getMetadata().getNamespace());
        
        //BrokerSpec spec=new BrokerSpecBuilder().withNewConfig("v1", "ConfigMap", "processor-channel",resource.getMetadata().getNamespace()).build();
        //broker.setSpec(spec);
        kn.brokers().create(broker);
        
        status.setKnativeConfigured(true);
        return resource;
    }

    public FhirConfig setupFHIR(FhirConfig resource) {
        FhirConfigStatus status = resource.getStatus();
        if(status==null)
            status=new FhirConfigStatus();
        try {
            client.getNamespace();
            FhirContext ctx = null;
            if (resource.getSpec().getFhirVersion().equals(FHIRVersion.R4))
                ctx = FhirContext.forR4();
            if (resource.getSpec().getFhirVersion().equals(FHIRVersion.R5))
                ctx = FhirContext.forR5();

            System.out.println("Namespace:" + resource.getMetadata().getNamespace());

            IGenericClient iclient = ctx.newRestfulGenericClient(resource.getSpec().getFhirServerUrl());

            CapabilityStatement cs = iclient.fetchResourceFromUrl(CapabilityStatement.class,
                    resource.getSpec().getFhirServerUrl() + "metadata?_format=json");

            List<CapabilityStatementRestComponent> rest = cs.getRest();

            // Build Capabilities
            resource.getSpec().setFhirResources(new ArrayList<FhirResource>());
            for (CapabilityStatementRestComponent csr : rest) {

                for (CapabilityStatementRestResourceComponent resourceType : csr.getResource()) {
                    // System.out.println(" - " + resourceType.getType());
                    resource.getSpec().getFhirResources()
                            .add(new FhirResource(resourceType.getType() + "", "NA", "NA"));
                }

            }
            
            status.setFhirconfig(true);

        } catch (Exception ex) {
            ex.printStackTrace();
            status = new FhirConfigStatus();
            status.setMessage("Setting up FHIR: " + ex.getMessage());
            status.setState(FhirConfigStatus.State.ERROR);
            status.setError(true);
        }
        status.setFhirconfig(true);
        resource.setStatus(status);
        return resource;

    }

    public FhirConfig setupIntegration(FhirConfig resource) {
        FhirConfigStatus status = resource.getStatus();
        DefaultCamelKClient camelk = new DefaultCamelKClient(client.getConfiguration());

        String definition = String.join("\n",
               
                 
                "//camel-k: language=java dependency=camel-quarkus-amqp dependency=camel-quarkus-platform-http trait=service.auto=true property=quarkus.qpid-jms.url=amqp://localhost:15000 property=org.apache.camel=ERROR property=logger.org.apache.activemq.artemis.jms.level=ERROR dependency=mvn:org.apache.activemq:artemis-amqp-protocol:2.16.0.redhat-00022 dependency=mvn:org.apache.activemq:artemis-jms-client:2.16.0.redhat-00022 dependency=mvn:org.apache.activemq:artemis-commons:2.16.0.redhat-00022 dependency=mvn:org.apache.activemq:artemis-jms-server:2.16.0.redhat-00022 dependency=mvn:org.apache.activemq:artemis-core-client:2.16.0.redhat-00022 dependency=mvn:org.apache.activemq:artemis-journal:2.16.0.redhat-00022 dependency=mvn:org.apache.activemq:artemis-server:2.16.0.redhat-00022",
                "","",
                "import org.apache.camel.builder.RouteBuilder;",
                "import javax.enterprise.context.ApplicationScoped;",
                "import org.apache.activemq.artemis.api.core.RoutingType;",
                "import org.apache.activemq.artemis.core.config.Configuration;",
                "import org.apache.activemq.artemis.core.config.CoreAddressConfiguration;",
                "import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;",
                "import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;",
                "import org.apache.activemq.artemis.core.settings.impl.AddressSettings;",
                "import org.apache.camel.CamelContext;",
                "import org.apache.camel.EndpointInject;",
                "import org.apache.camel.builder.RouteBuilder;",
                "import org.apache.camel.component.amqp.AMQPComponent;",
                
                "import static org.apache.camel.component.amqp.AMQPConnectionDetails.AMQP_PORT;",
                "import static org.apache.camel.component.amqp.AMQPConnectionDetails.AMQP_SET_TOPIC_PREFIX;",
                "import static org.apache.camel.component.amqp.AMQPConnectionDetails.discoverAMQP;",
                "import java.util.Map;",
                "import java.util.HashMap;",
                "import org.apache.camel.model.dataformat.JsonLibrary;",
                "import java.util.Base64;",
                "\n",
                "@ApplicationScoped",
                "public class FhirConnector extends RouteBuilder {",
                " static int amqpPort = 61616;",
                "   static EmbeddedActiveMQ server = new EmbeddedActiveMQ();",
                "\n",
                "private String getKafkaTopicUri(String topic) {",
                "    return \"kafka:" + resource.getSpec().getCommonConfig().getKafkaPrefix()+"\"+topic+\"?lazyStartProducer=true\";",
                "}",
                "\n",
                "@Override",
                "public void configure() throws Exception {",
                "\n\n",
                
                "Configuration config = new ConfigurationImpl();",
                "AddressSettings addressSettings = new AddressSettings();",
                "addressSettings.setAutoCreateAddresses(true);",
                "config.addAcceptorConfiguration(\"amqp\", \"tcp://0.0.0.0:\" + amqpPort\n",
                "                                       + \"?tcpSendBufferSize=1048576;tcpReceiveBufferSize=1048576;protocols=AMQP;amqpCredits=1000;amqpMinCredits=300\");\n",
                "config.setPersistenceEnabled(false);",
                "config.addAddressesSetting(\"#\", addressSettings);",
                "config.setSecurityEnabled(false);",
                "server.setConfiguration(config);",
                "server.start();",
                "System.setProperty(AMQP_PORT, amqpPort + \"\");",
                "System.setProperty(AMQP_SET_TOPIC_PREFIX, \"true\");",
                "\n",
                //"getContext().addComponent(\"activemq\", AMQComponent.activeMQComponent(\"vm://localhost?broker.persistent=false\"));",
                "AMQPComponent amqp = AMQPComponent.amqpComponent(\"amqp://localhost:61616\");",
                
                "Map<String, String> eventPayload = new HashMap<String, String>();",
                "Map<String, String> preprocessor = new HashMap<String, String>();",
                "Map<String, String> postprocessor = new HashMap<String, String>();"
            );

            String common="";

            for (FhirResource fhirresource : resource.getSpec().getFhirResources()) {
                definition=definition+"preprocessor.put(\""+fhirresource.getName()+"\", \""+fhirresource.getPreProcessing()+"\");\n";
                definition=definition+"postprocessor.put(\""+fhirresource.getName()+"\", \""+fhirresource.getPostProcessing()+"\");\n";
            }

            common=String.join("\n",

                "  //", 
                "  //  Audit",
                "  // ",
                "   \n",
                "  from(\"direct:auditing\")",
                "    .routeId(\"KIC-KnowledgeInsightConformance\")",
                "    .setHeader(\"messageprocesseddate\").simple(\"${date:now:yyyy-MM-dd}\")",
                "    .setHeader(\"messageprocessedtime\").simple(\"${date:now:HH:mm:ss:SSS}\")",
                "    .setHeader(\"processingtype\").exchangeProperty(\"processingtype\")",
                "    .setHeader(\"industrystd\").exchangeProperty(\"industrystd\")",
                "    .setHeader(\"component\").exchangeProperty(\"componentname\")",
                "    .setHeader(\"messagetrigger\").exchangeProperty(\"messagetrigger\")",
                "    .setHeader(\"processname\").exchangeProperty(\"processname\")",
                "    .setHeader(\"auditdetails\").exchangeProperty(\"auditdetails\")",
                "    .setHeader(\"camelID\").exchangeProperty(\"camelID\")",
                "    .setHeader(\"exchangeID\").exchangeProperty(\"exchangeID\")",
                "    .setHeader(\"internalMsgID\").exchangeProperty(\"internalMsgID\")",
                "    .setHeader(\"bodyData\").exchangeProperty(\"bodyData\")",
                "    .convertBodyTo(String.class).to(getKafkaTopicUri(\"opsmgmt_platformtransactions\"));\n\n",
                "// Handle pre and post post processor events",
                "  from(\"knative:endpoint/processor-channel\")",
                "    .log(\"<-- Knative\")",
                "    .unmarshal().json(JsonLibrary.Jackson,Map.class)",
                "    .to(\"log:info?multiline=true&showAll=true\")",
                "    .routeId(\"ReceiveAndReply\")",
                "    .setHeader(\"JMSCorrelationId\",simple(\"${body[JMSCorrelationId]}\"))",
                "    .setHeader(\"JMSReplyTo\",simple(\"${body[JMSReplyTo]}\"))",
                "    .log(\"<-- Knative\")",
                "    .setBody(simple(\"${body[payload]}\"))",
                "    .process(exchange -> {",
                "      exchange.getIn().setBody(new String(Base64.getDecoder().decode(exchange.getIn().getBody(String.class).getBytes())));",
                "       })", 
                "    .to(\"log:info?multiline=true&showAll=true\")",
                "    .log(\"${in.header.JMSReplyTo}\")",
                "    .toD(\"amqp:temp-queue:${in.header.JMSReplyTo}?exchangePattern=InOnly\");",
                "    // .log(\"${body}\");",

                "   from(\"amqp:queue:process?exchangePattern=InOnly\")",
                "      .routeId(\"ReceiveJMS\")",
                "      .log(\" ---> JMS\")", 
                "      .to(\"log:info?multiline=true&showAll=true\")",
                "      .process(exchange -> { ",
                "          eventPayload.clear();",
                "          eventPayload.put(\"JMSCorrelationId\",exchange.getIn().getHeader(\"JMSCorrelationId\").toString());",
                "          eventPayload.put(\"JMSReplyTo\",exchange.getIn().getHeader(\"JMSReplyTo\").toString());",
                "          eventPayload.put(\"payload\",Base64.getEncoder().encodeToString(exchange.getIn().getBody(String.class).getBytes())); ",
                "          exchange.getIn().setBody(eventPayload); ",
                "       })",
                "       .marshal().json(JsonLibrary.Jackson,Map.class)",
                "       .convertBodyTo(String.class)",
                "      .log(\" ---> Knative\")",
                "      .to(\"log:info?multiline=true&showAll=true\")",
                "      .to(\"knative:endpoint/processor-channel\");"
                );
                definition=definition+common;
                

            
                definition=definition+"\n\n// Definition \n\n";
                definition=definition+"from(\"platform-http:/fhir/\")\n";
                definition=definition+"  .routeId(\"resource\")\n";
                

                common=String.join("\n",
                "   .to(\"log:info?multiline=true&showAll=true\")",
                "  .setProperty(\"resource\").simple(\"${header.CamelHttpURI}\")",
                "  .convertBodyTo(String.class)",
                "  .setProperty(\"appname\").constant(\"${resource}\")",
                "  .setProperty(\"industrystd\").constant(\"FHIR\")",
                "  .setProperty(\"messagetrigger\").constant(\"${resource}\")",
                "  .setProperty(\"camelID\").simple(\"${camelId}\")",
                "  .setProperty(\"component\").simple(\"${routeId}\")",
                "  .setProperty(\"exchangeID\").simple(\"${exchangeId}\")",
                "  .setProperty(\"internalMsgID\").simple(\"${id}\")",
                "  .setProperty(\"bodyData\").simple(\"${body}\")",
                "  .setProperty(\"processname\").constant(\"Input\")",
                //"  .setProperty(\"auditdetails\").simple(\"${resource}\")",
                "  .setProperty(\"processingtype\").constant(\"data\")\n");
                definition=definition+common;
                

                common=String.join("\n",
                "   // iDAAS KIC - Auditing Processing",
                "   .wireTap(\"direct:auditing\")",
                "  // Send To Topic",
                "   .convertBodyTo(String.class).to(getKafkaTopicUri(\"fhirsvr_\"+simple(\"${resource}\")+\"response\"))\n");
               

                /*if(!fhirresource.getPreProcessing().equals("NA")) {
                    definition=definition+common;
                    common=String.join("\n",
                    
                    "   .setHeader(\"ce-eventtype\",simple(\""+fhirresource.getPreProcessing()+"\"))",
                    "   .setHeader(\"ce-correlationid\",simple(\"${camelId}\"))",
                    "   .log(\"-----> Knative\")",
                    "   .setHeader(\"ce-type\").constant(\""+fhirresource.getPreProcessing()+"\")", 
                    "   .to(\"log:info?multiline=true&showAll=true\")",
                    
                    "   .to(\"amqp:queue:process?exchangePattern=InOut&acceptMessagesWhileStopping=true&disableTimeToLive=true&replyToType=Exclusive&asyncConsumer=true&concurrentConsumers=10\")",
                    "   .log(\"JMS response:\")",
                    "   .to(\"log:info?multiline=true&showAll=true\")"
                    );
                }

                if(resource.getSpec().getFhirProcessToFHIR()) {
                    definition=definition+common;
                    common=String.join("\n",
                    "\n   //Send to FHIR Server",
                    "   .setHeader(\"ContentType\",simple(\"application/json\"))",
                    "   .setHeader(\"CamelHttpURI\",simple(\"\"))",
                    "   .setHeader(\"CamelHttpPath\",simple(\"\"))",
                    "   .log(\"-----> Before FHIR\")",
                    "   .to(\"log:info?multiline=true&showAll=true\")",
                    "   .to(\""+resource.getSpec().getFhirServerUrl()+fhirresource.getName()+"?bridgeEndpoint=true\")",
                    "   //Process Response",
                    "   .convertBodyTo(String.class)",
                    "   .log(\"-----> After FHIR\")",
                    "   .to(\"log:info?multiline=true&showAll=true\")",
                    "   // set Auditing Properties",
                    "   .setProperty(\"processingtype\").constant(\"data\")",
                    "   .setProperty(\"appname\").constant(\""+resource.getSpec().getAppname()+"\")",
                    "   .setProperty(\"industrystd\").constant(\"FHIR\")",
                    "   .setProperty(\"messagetrigger\").constant(\""+fhirresource.getName()+"response\")",
                    "   .setProperty(\"component\").simple(\"${routeId}\")",
                    "   .setProperty(\"processname\").constant(\"Response\")",
                    "   .setProperty(\"camelID\").simple(\"${camelId}\")",
                    "   .setProperty(\"exchangeID\").simple(\"${exchangeId}\")",
                    "   .setProperty(\"internalMsgID\").simple(\"${id}\")",
                    "   .setProperty(\"bodyData\").simple(\"${body}\")",
                    "   .setBody(simple(\"${body}\"))",
                    "   .setProperty(\"auditdetails\").constant(\""+fhirresource.getName()+" FHIR response message received\")",
                    "   // iDAAS KIC - Auditing Processing",
                    "   .wireTap(\"direct:auditing\")");
                }

                if(!fhirresource.getPostProcessing().equals("NA")) {
                    definition=definition+common;
                    common=String.join("\n",
                    
                    "   .setHeader(\"eventType\",simple(\""+fhirresource.getPostProcessing()+"\"))",
                    "   .log(\"knative:endpoint/processor-channel\")",
                    "   .to(\"amqp:queue:process?exchangePattern=InOut&acceptMessagesWhileStopping=true\")"
                    );
                }*/
                definition=definition+common+";\n";


                
            
            definition=definition+ "\n   }\n}";

            

        //Source Code
        List<SourceSpec> sources=new ArrayList<SourceSpec>();
        SourceSpec sspec=new SourceSpec();
        sspec.setLanguage("java");
        sspec.setContent(definition);
        sspec.setName("FhirConnector.java");
        sources.add(sspec);

        //Dependencies
        List<String> dependencies=new ArrayList<String>();
        dependencies.add("camel:amqp");
        dependencies.add("camel:jackson");
        dependencies.add("camel:kafka");
        dependencies.add("mvn:org.apache.activemq:artemis-amqp-protocol:2.16.0.redhat-00022");
        dependencies.add("mvn:org.apache.activemq:artemis-jms-client:2.16.0.redhat-00022");
        dependencies.add("mvn:org.apache.activemq:artemis-commons:2.16.0.redhat-00022");
        
        dependencies.add("mvn:org.apache.activemq:artemis-core-client:2.16.0.redhat-00022");
        dependencies.add("mvn:org.apache.activemq:artemis-journal:2.16.0.redhat-00022");
        dependencies.add("mvn:org.apache.activemq:artemis-server:2.16.0.redhat-00022");
            
        //Properties
        //Configuration
        ConfigurationSpec configuration=new ConfigurationSpec("property","quarkus.qpid-jms.url=amqp://localhost:61616");
        ConfigurationSpec kafka_brokers=new ConfigurationSpec("property","camel.component.kafka.brokers="+resource.getSpec().getCommonConfig().getKafkaBrokerURL()+"");
        //ConfigurationSpec kafka_security=new ConfigurationSpec("property","camel.component.kafka.security-protocol=PLAINTEXT");
        //ConfigurationSpec kafka_sasl_mechnaism=new ConfigurationSpec("property","camel.component.kafka.sasl-mechanism=PLAIN");
        ConfigurationSpec camel_logger=new ConfigurationSpec("property","org.apache.camel=ERROR");


        //Specs
        IntegrationSpec ispec=new IntegrationSpec();
        ispec.setSources(sources);
        ispec.setDependencies(dependencies);
        
        //Map<String, TraitSpec> traits=new HashMap<String,TraitSpec>();
        //TraitSpec knativex=new TraitSpec();
       // knativex.setConfiguration(JSO"{\"knative-service.min-scale\": 1\"}");
        //traits.put("knative",knativex); 
        //ispec.setProfile("deployment");
        //ispec.setTraits(traits);
        
        
        ispec.getConfiguration().add(configuration);
        ispec.getConfiguration().add(kafka_brokers);
        ispec.getConfiguration().add(camel_logger);
        //ispec.getConfiguration().add(kafka_sasl_mechnaism);
        //ispec.getConfiguration().add(kafka_security);


        Integration fhirconnector=new Integration();
        fhirconnector.setKind("Integration");
        
        fhirconnector.setMetadata(new ObjectMeta());
        fhirconnector.getMetadata().setName("fhirconnector");
        fhirconnector.getMetadata().setNamespace(resource.getMetadata().getNamespace());
       
        
        fhirconnector.setSpec(ispec);
        // Create Integration
        camelk.v1().integrations().createOrReplace(fhirconnector);
        //Integration i = camelk.v1().integrations().inNamespace(resource.getMetadata().getNamespace())
        //        .load(new ByteArrayInputStream(definition.getBytes())).createOrReplace();
        
        System.out.println("Namespace:" + resource.getMetadata().getNamespace());
        if (status == null)
            status = new FhirConfigStatus();

        status.setMessage("Integration deployed");
        status.setState(FhirConfigStatus.State.CREATED);
        status.setError(false);
        status.setFhirintegration(true);
        
        resource.setStatus(status);
        return resource;
    }

}
