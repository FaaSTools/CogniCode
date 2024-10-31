package org.example.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.CloudService;
import org.example.model.ServiceLimits;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServiceRegistry {

    public static List<CloudService> createServicesFromJson(String filePath) {
        List<CloudService> serviceList = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            List<Map<String, Object>> services =
                objectMapper.readValue(new File(filePath), List.class);

            for (Map<String, Object> serviceMap : services) {
                String category = (String) serviceMap.get("category");
                String name = (String) serviceMap.get("name");
                String provider = (String) serviceMap.get("provider");
                List<String> inputFormats =
                    (List<String>) serviceMap.get("inputFormats");
                List<String> outputFormats =
                    (List<String>) serviceMap.get("outputFormats");
                List<String> capabilities =
                    (List<String>) serviceMap.get("capabilities");
                Map<String, Map<String, Object>> limitsMap =
                    (Map<String, Map<String, Object>>) serviceMap.get("limits");

                ServiceLimits serviceLimits = new ServiceLimits(limitsMap);

                CloudService cloudService = CloudServiceFactory.createService(
                    category,
                    name,
                    provider,
                    inputFormats,
                    outputFormats,
                    capabilities,
                    serviceLimits
                );

                serviceList.add(cloudService);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to read or parse the JSON file.");
        }

        return serviceList;
    }
}