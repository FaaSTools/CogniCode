package org.example.model;

import java.util.List;

public class MediaService extends CloudService {

    public MediaService(String name, String provider, List<String> inputFormat,
                        List<String> outputFormat, List<String> features,
                        ServiceLimits serviceLimits) {
        super(name, provider, "MediaServices", inputFormat, outputFormat,
              features, serviceLimits);
    }
}
