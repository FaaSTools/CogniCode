package org.example.model;

import java.util.List;

public class TextService extends CloudService {

    public TextService(String name, String provider, List<String> inputFormat,
                       List<String> outputFormat, List<String> features,
                       ServiceLimits serviceLimits) {
        super(name, provider, "TextServices", inputFormat, outputFormat,
              features, serviceLimits);
    }
}

