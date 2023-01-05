package com.redhat.healthcare;

public class CommonConfig {
    private String kafkaPrefix="idaas.";

    public String getKafkaPrefix() {
        return this.kafkaPrefix;
    }

    public void setKafkaPrefix(String kafkaPrefix) {
        this.kafkaPrefix = kafkaPrefix;
    }

    public String getKafkaBrokerURL() {
        return this.kafkaBrokerURL;
    }

    public void setKafkaBrokerURL(String kafkaBrokerURL) {
        this.kafkaBrokerURL = kafkaBrokerURL;
    }
    private String kafkaBrokerURL="kafka-svc,kafka-1-svc";
}
