package org.example.model;

import java.util.List;

public class VisionService extends CloudService {

    public VisionService(String name, String provider, List<String> inputFormat,
                         List<String> outputFormat, List<String> features,
                         ServiceLimits serviceLimits) {
        super(name, provider, "VisionServices", inputFormat, outputFormat,
              features, serviceLimits);
    }
}
