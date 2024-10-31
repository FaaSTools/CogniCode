package org.example.config;

import java.util.List;
import org.example.model.CloudService;
import org.example.model.DocumentService;
import org.example.model.MediaService;
import org.example.model.ServiceLimits;
import org.example.model.SpeechService;
import org.example.model.TextService;
import org.example.model.VisionService;

public class CloudServiceFactory {

    public static CloudService createService(String type, String name,
                                             String provider,
                                             List<String> inputFormat,
                                             List<String> outputFormat,
                                             List<String> features,
                                             ServiceLimits serviceLimits) {
        switch (type) {
            case "TextServices":
                return new TextService(name, provider, inputFormat,
                                       outputFormat, features, serviceLimits);
            case "SpeechServices":
                return new SpeechService(name, provider, inputFormat,
                                         outputFormat, features, serviceLimits);
            case "VisionServices":
                return new VisionService(name, provider, inputFormat,
                                         outputFormat, features, serviceLimits);
            case "DocumentServices":
                return new DocumentService(name, provider, inputFormat,
                                           outputFormat, features,
                                           serviceLimits);
            case "MediaServices":
                return new MediaService(name, provider, inputFormat,
                                        outputFormat, features, serviceLimits);
            default:
                throw new IllegalArgumentException(
                    "Unbekannter Service-Typ: " + type);
        }
    }
}
