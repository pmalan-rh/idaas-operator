package com.redhat.healthcare;

public class IDaaSConfigSpec {
    
  
    private CommonConfig commonConfig=new CommonConfig();
    private ConnectorConfig connectorConfig=new ConnectorConfig();

    public ConnectorConfig getConnectorConfig() {
        return this.connectorConfig;
    }

    public void setConnectorConfig(ConnectorConfig connectorConfig) {
        this.connectorConfig = connectorConfig;
    }

   

    

   
  
    public CommonConfig getCommonConfig() {
        return this.commonConfig;
    }

    public void setCommonConfig(CommonConfig commonConfig) {
        this.commonConfig = commonConfig;
    }

}
