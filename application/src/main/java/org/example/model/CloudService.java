package org.example.model;

import java.util.List;

public class CloudService {

    private String name;
    private String provider;
    private String serviceCategory;
    private List<String> inputFormat;
    private List<String> outputFormat;
    private List<String> features;
    private ServiceLimits serviceLimits;

    public CloudService(String name, String provider, String serviceCategory,
                        List<String> inputFormat, List<String> outputFormat,
                        List<String> features, ServiceLimits serviceLimits) {
        this.name = name;
        this.provider = provider;
        this.serviceCategory = serviceCategory;
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
        this.features = features;
        this.serviceLimits = serviceLimits;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getServiceCategory() {
        return serviceCategory;
    }

    public void setServiceCategory(String serviceCategory) {
        this.serviceCategory = serviceCategory;
    }

    public List<String> getInputFormat() {
        return inputFormat;
    }

    public void setInputFormat(List<String> inputFormat) {
        this.inputFormat = inputFormat;
    }

    public List<String> getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(List<String> outputFormat) {
        this.outputFormat = outputFormat;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }

    public ServiceLimits getServiceLimits() {
        return serviceLimits;
    }

    public void setServiceLimits(ServiceLimits serviceLimits) {
        this.serviceLimits = serviceLimits;
    }

    @Override
    public String toString() {
        return "CloudService{" +
               "name='" + name + '\'' +
               ", provider='" + provider + '\'' +
               ", serviceCategory='" + serviceCategory + '\'' +
               ", inputFormat=" + inputFormat +
               ", outputFormat=" + outputFormat +
               ", features=" + features +
               ", serviceLimits=" + serviceLimits +
               '}';
    }
}
