package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Intent {

    private String inputType;
    private String inputLanguage;
    private String outputType;
    private String outputLanguage;
    private String inputFilePath;
    private String bucketName;
    private String serviceFilePath;
    private String llmProvider;
    private String cloudProvider;

    public String getInputType() {
        return inputType;
    }

    @JsonIgnore
    public String getInputTypeWithoutUri() {
        if (inputType != null && inputType.contains("#")) {
            return inputType.substring(inputType.indexOf('#') + 1);
        }
        return inputType;
    }

    @JsonIgnore
    public String getOutputTypeWithoutUri() {
        if (outputType != null && outputType.contains("#")) {
            return outputType.substring(outputType.indexOf('#') + 1);
        }
        return outputType;
    }

    @JsonIgnore
    public String getInputLanguageWithoutUri() {
        if (inputLanguage != null && inputLanguage.contains("=")) {
            return inputLanguage.substring(inputLanguage.indexOf('=') + 1);
        }
        return inputLanguage;
    }

    @JsonIgnore
    public String getOutputLanguageWithoutUri() {
        if (outputLanguage != null && outputLanguage.contains("=")) {
            return outputLanguage.substring(outputLanguage.indexOf('=') + 1);
        }
        return outputLanguage;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getInputLanguage() {
        return inputLanguage;
    }

    public void setInputLanguage(String inputLanguage) {
        this.inputLanguage = inputLanguage;
    }

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public String getOutputLanguage() {
        return outputLanguage;
    }

    public void setOutputLanguage(String outputLanguage) {
        this.outputLanguage = outputLanguage;
    }

    public String getInputFilePath() {
        return inputFilePath;
    }

    public void setInputFilePath(String inputFilePath) {
        this.inputFilePath = inputFilePath;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getServiceFilePath() {
        return serviceFilePath;
    }

    public void setServiceFilePath(String serviceFilePath) {
        this.serviceFilePath = serviceFilePath;
    }

    public String getLlmProvider() {
        return llmProvider;
    }

    public void setLlmProvider(String llmProvider) {
        this.llmProvider = llmProvider;
    }

    public String getCloudProvider() {
        return cloudProvider;
    }

    public void setCloudProvider(String cloudProvider) {
        this.cloudProvider = cloudProvider;
    }

    @Override
    public String toString() {
        return "Intent{" +
               "inputType='" + inputType + '\'' +
               ", inputLanguage='" + inputLanguage + '\'' +
               ", outputType='" + outputType + '\'' +
               ", outputLanguage='" + outputLanguage + '\'' +
               ", inputFilePath='" + inputFilePath + '\'' +
               ", bucketName='" + bucketName + '\'' +
               ", serviceFilePath='" + serviceFilePath + '\'' +
               ", llmProvider='" + llmProvider + '\'' +
               ", cloudProvider='" + cloudProvider + '\'' +
               '}';
    }

}