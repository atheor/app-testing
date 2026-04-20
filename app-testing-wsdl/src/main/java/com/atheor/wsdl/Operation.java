package com.atheor.wsdl;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OperationType",
         namespace = "http://atheor.com/schedule",
         propOrder = {"operationId", "operationName", "operationType", "payload"})
public class Operation {

    @XmlElement(required = true, namespace = "http://atheor.com/schedule")
    private String operationId;

    @XmlElement(required = true, namespace = "http://atheor.com/schedule")
    private String operationName;

    @XmlElement(required = true, namespace = "http://atheor.com/schedule")
    private String operationType;

    @XmlElement(namespace = "http://atheor.com/schedule")
    private String payload;
}
