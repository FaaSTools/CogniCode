package org.example.model;

import java.util.List;

public class DocumentService extends CloudService {

    public DocumentService(String name, String provider,
                           List<String> inputFormat, List<String> outputFormat,
                           List<String> features, ServiceLimits serviceLimits) {
        super(name, provider, "DocumentServices", inputFormat, outputFormat,
              features, serviceLimits);
    }
}