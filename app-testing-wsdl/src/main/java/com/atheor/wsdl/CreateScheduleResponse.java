package com.atheor.wsdl;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
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
@XmlType(name = "CreateScheduleResponseType",
         namespace = "http://atheor.com/schedule",
         propOrder = {"scheduleId", "status", "message", "correlationId"})
@XmlRootElement(name = "CreateScheduleResponse",
                namespace = "http://atheor.com/schedule")
public class CreateScheduleResponse {

    @XmlElement(required = true, namespace = "http://atheor.com/schedule")
    private String scheduleId;

    @XmlElement(required = true, namespace = "http://atheor.com/schedule")
    private String status;

    @XmlElement(namespace = "http://atheor.com/schedule")
    private String message;

    @XmlElement(namespace = "http://atheor.com/schedule")
    private String correlationId;
}
