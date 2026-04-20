package com.atheor.wsdl;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OperationListType",
         namespace = "http://atheor.com/schedule",
         propOrder = {"operation"})
public class OperationList {

    @XmlElement(name = "operation", namespace = "http://atheor.com/schedule")
    @Builder.Default
    private List<Operation> operation = new ArrayList<>();
}
