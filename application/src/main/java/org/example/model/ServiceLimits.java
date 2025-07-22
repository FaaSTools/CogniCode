package org.example.model;

import java.util.Map;

public class ServiceLimits {
    private Map<String, Map<String, Object>> limits; // Map of function name to limit details

    public ServiceLimits(Map<String, Map<String, Object>> limits) {
        this.limits = limits;
    }

    public Map<String, Map<String, Object>> getLimits() {
        return limits;
    }

    public void setLimits(Map<String, Map<String, Object>> limits) {
        this.limits = limits;
    }

    @Override
    public String toString() {
        return "ServiceLimits{" +
               "limits=" + limits +
               '}';
    }
}