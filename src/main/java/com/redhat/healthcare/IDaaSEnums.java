package com.redhat.healthcare;

import java.util.Enumeration;

public class IDaaSEnums {
    public enum connector {FHIR,HL7,EDI};
    public enum State {
        CREATED,
        ALREADY_PRESENT,
        PROCESSING,
        ERROR,
        UNKNOWN
    }
    

    
}
