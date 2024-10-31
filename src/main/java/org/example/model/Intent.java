package org.example.model;

public class Intent {

    private String inputType;
    private String inputLanguage;
    private String outputType;
    private String outputLanguage;
    private String inputFilePath;
    private String bucketName;
    private String serviceFilePath;
    private String llmProvider;

    public String getInputType() {
        return inputType;
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
               '}';
    }

}