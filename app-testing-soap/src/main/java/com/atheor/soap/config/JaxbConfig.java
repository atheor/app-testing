package com.atheor.soap.config;

import com.atheor.wsdl.CreateScheduleRequest;
import com.atheor.wsdl.CreateScheduleResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

/**
 * Provides a shared, thread-safe {@link JAXBContext} for all SOAP-related classes.
 *
 * <p>JAXBContext is expensive to create and fully thread-safe after initialisation —
 * this class ensures it is built exactly once.
 */
public final class JaxbConfig {

    private static final JAXBContext INSTANCE;

    static {
        try {
            INSTANCE = JAXBContext.newInstance(
                    CreateScheduleRequest.class,
                    CreateScheduleResponse.class
            );
        } catch (JAXBException e) {
            throw new ExceptionInInitializerError("Failed to create shared JAXBContext: " + e.getMessage());
        }
    }

    private JaxbConfig() {
    }

    public static JAXBContext getContext() {
        return INSTANCE;
    }
}
