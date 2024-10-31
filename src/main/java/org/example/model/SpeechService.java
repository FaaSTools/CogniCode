package org.example.model;

import java.util.List;

public class SpeechService extends CloudService {

    public SpeechService(String name, String provider, List<String> inputFormat,
                         List<String> outputFormat, List<String> features,
                         ServiceLimits serviceLimits) {
        super(name, provider, "SpeechServices", inputFormat, outputFormat,
              features, serviceLimits);
    }
}
