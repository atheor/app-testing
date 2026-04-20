package com.atheor.wsdl;

import jakarta.xml.bind.annotation.XmlRegistry;

/**
 * JAXB ObjectFactory for the http://atheor.com/schedule namespace.
 * Provides factory methods to construct JAXB-annotated instances.
 */
@XmlRegistry
public class ObjectFactory {

    public ObjectFactory() {
    }

    public CreateScheduleRequest createCreateScheduleRequest() {
        return new CreateScheduleRequest();
    }

    public CreateScheduleResponse createCreateScheduleResponse() {
        return new CreateScheduleResponse();
    }

    public Operation createOperation() {
        return new Operation();
    }

    public OperationList createOperationList() {
        return new OperationList();
    }
}
