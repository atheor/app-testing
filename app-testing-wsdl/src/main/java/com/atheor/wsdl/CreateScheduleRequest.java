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
@XmlType(name = "CreateScheduleRequestType",
         namespace = "http://atheor.com/schedule",
         propOrder = {"scheduleId", "scheduleName", "startDate", "endDate", "operations"})
@XmlRootElement(name = "CreateScheduleRequest",
                namespace = "http://atheor.com/schedule")
public class CreateScheduleRequest {

    @XmlElement(required = true, namespace = "http://atheor.com/schedule")
    private String scheduleId;

    @XmlElement(required = true, namespace = "http://atheor.com/schedule")
    private String scheduleName;

    @XmlElement(required = true, namespace = "http://atheor.com/schedule")
    private String startDate;

    @XmlElement(required = true, namespace = "http://atheor.com/schedule")
    private String endDate;

    @XmlElement(namespace = "http://atheor.com/schedule")
    private OperationList operations;
}
