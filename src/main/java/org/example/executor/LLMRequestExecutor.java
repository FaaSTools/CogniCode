package org.example.executor;

import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import io.github.cdimascio.dotenv.Dotenv;
import org.example.llm.LLMService;
import org.example.model.CloudService;
import org.example.model.Intent;
import org.example.utlis.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LLMRequestExecutor {

    private static final Dotenv DOTENV = Dotenv.load();
    private static final int MAX_ITERATIONS = 10;
    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant who generates and executes bash and Python code.
            You should adhere to the specified services pipeline, their order, and their limitations.
            Ensure each service's input/output requirements are met and properly handled in the code.
            """;
    private static final String VIDEO_CONVERSION_JOB_SETTINGS_AWS = """
            {
                "Inputs": [
                    {
                        "AudioSelectors": {
                            "Audio Selector 1": {
                                "DefaultSelection": "DEFAULT"
                            }
                        },
                        "TimecodeSource": "ZEROBASED",
                        "FileInput": f"s3://{s3_bucket}/{input_filename}"
                    }
                ],
                "OutputGroups": [
                    {
                        "Name": "File Group",
                        "Outputs": [
                            {
                                "ContainerSettings": {
                                    "Container": "RAW"
                                },
                                "AudioDescriptions": [
                                    {
                                        "AudioSourceName": "Audio Selector 1",
                                        "CodecSettings": {
                                            "Codec": "MP3",
                                            "Mp3Settings": {
                                                "Bitrate": 96000,
                                                "SampleRate": 48000,
                                                "RateControlMode": "CBR",
                                                "Channels": 2
                                            }
                                        }
                                    }
                                ]
                            }
                        ],
                        "OutputGroupSettings": {
                            "Type": "FILE_GROUP_SETTINGS",
                            "FileGroupSettings": {
                                "Destination": f"s3://{s3_bucket}/{output_filename_without_file_extension}"
                            }
                        }
                    }
                ]
            }
            It is very important for the output filename to not include the file ending .mp3 as it is automatically attached.
            """;

    private static final String VIDEO_CONVERSION_JOB_SETTINGS_GCP = """
                  job_config = {
                             "config": {
                                 "inputs": [{
                                     "key": "input0",
                                     "uri": input_uri
                                 }],
                                 "elementaryStreams": [{
                                     "key": "audio-stream0",
                                     "audioStream": {
                                         "codec": "mp3",
                                         "bitrateBps": 64000,
                                         "channelCount": 2
                                     }
                                 }],
                                 "muxStreams": [{
                                     "key": "mp3-stream0",
                                     "container": "mp3",
                                     "elementaryStreams": ["audio-stream0"]
                                 }],
                                 "output": {
                                     "uri": output_uri
                                 }
                             }
                         }
            """;

    private static final String AWS_QUERY_TEMPLATE = """
            Generate executable Python code for the following transformation task:
            \s
            Source Format: %s in %s
            Target Format: %s in %s
            \s
            %s
            Detailed Service Pipeline Configuration:
            %s
            Additional Requirements:
            - AWS credentials are already configured
            - If you need a bucket, use the following: %s
            - As default region please use: us-east-1
            - The account id is the following: %s
            - If you need a role use the following: %s
            - Implement proper error handling for each service
            - Validate input/output at each step
            - For transcription I only want the text as a result not the whole transcription json content
            Please generate code that follows this service pipeline, handles all limitations, and annotate it correctly with ```python.
            You need to work autonomously and I cannot exchange parts of the code.
            If the limits are surpassed by the input, you need to try to split the input before calling the service and merge it afterwards,\s
            But only do the split if needed, first try it without splitting it.
            Merging together after splitting is very important and should also be done on audio outputs!
            If possible, save the output locally.
            """;

    private static final String GCP_QUERY_TEMPLATE = """
            Generate executable Python code for the following transformation task:
                        
            Source Format: %s in %s
            Target Format: %s in %s
                        
            %s
            Detailed Service Pipeline Configuration:
            %s
            Additional Requirements:
            - Assume you are in a constrained environment, do not use any local tools, such as ffpmeg or ML libraries
            - GCP credentials are already configured
            - GCP service account credentials are given in the file "service-account.json"
            - If you need a cloud storage, use the following: %s
            - The GCP project id is the following: %s
            - Implement proper error handling for each service
            - Validate input/output at each step
            - For transcription I only want the text as a result not the whole transcription json content
            Please generate code that follows this service pipeline, handles all limitations, and annotate it correctly with ```python.
            You need to work autonomously and I cannot exchange parts of the code.
            If the limits are surpassed by the input, you need to try to split the input before calling the service and merge it afterwards, 
            But only do the split if needed, first try it without splitting it.
            Merging together after splitting is very important and should also be done on audio outputs!
            If possible, save the output locally.
            """;

    private final List<ChatMessage> messages;
    private final List<CloudService> pipeline;
    private final String inputType;
    private final String inputLanguage;
    private final String outputType;
    private final String outputLanguage;
    private final String bucketName;
    private final String inputFilePath;
    private final LLMService llmService;
    private final String cloudProvider;
    private String inputFileTextContent;
    private long inputFileSize;

    public LLMRequestExecutor(LLMService llmService, Intent intent, List<CloudService> pipeline) {
        this.llmService = llmService;
        this.messages = new ArrayList<>();
        this.pipeline = pipeline;
        this.inputType = intent.getInputTypeWithoutUri();
        this.inputLanguage = intent.getInputLanguageWithoutUri();
        this.outputType = intent.getOutputTypeWithoutUri();
        this.outputLanguage = intent.getOutputLanguageWithoutUri();
        this.bucketName = intent.getBucketName();
        this.inputFilePath = intent.getInputFilePath();
        this.cloudProvider = intent.getCloudProvider();

        loadInputFile();
    }

    private void loadInputFile() {
        try {
            if (inputFilePath == null || inputFilePath.isEmpty()) {
                throw new IllegalArgumentException("Input file path is not specified");
            }

            Path path = Path.of(inputFilePath);
            inputFileSize = FileUtils.getFileSize(path.toString());

            if (isTextBasedFormat(inputType)) {
                try {
                    inputFileTextContent = FileUtils.readTextFile(path.toString());
                } catch (Exception e) {
                    inputFileTextContent = null;
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read input file: " + e.getMessage(), e);
        }
    }

    public void process() {
        messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), SYSTEM_PROMPT));

        // Generate initial prompt with detailed pipeline information
        String pipelineInfo = generateDetailedPipelineDescription();
        String fileInfo = FileUtils.getInputFileDescription(inputType, inputFileSize, inputFilePath, inputFileTextContent);

        String initialPrompt = String.format(this.cloudProvider.equalsIgnoreCase("aws") ? AWS_QUERY_TEMPLATE : GCP_QUERY_TEMPLATE, inputType, inputLanguage, outputType, outputLanguage, fileInfo, pipelineInfo, bucketName, this.cloudProvider.equalsIgnoreCase("aws") ? DOTENV.get("AWS_ACCOUNT_ID") : DOTENV.get("GCP_PROJECT_ID"), this.cloudProvider.equalsIgnoreCase("aws") ? DOTENV.get("AWS_ROLE") : "");

        System.out.println(initialPrompt);

        messages.add(new ChatMessage(ChatMessageRole.USER.value(), initialPrompt));

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            System.out.printf("Iteration %d: Sending prompt to LLM...%n", i + 1);

            String responseText = callLLMService();
            System.out.printf("Received response:%n%s%n-------%n", responseText);

            extractAndExecuteShellOrBashCode(responseText);

            String pythonCode = extractPythonCode(responseText);
            System.out.printf("%n-------%nExtracted Python code:%n%s%n-------%n", pythonCode);

            messages.add(new ChatMessage(ChatMessageRole.ASSISTANT.value(), pythonCode));

            ExecutionResult result = CodeExecutor.executePythonCode(pythonCode);

            System.out.println("Output being checked: " + result.getOutput());
            if (result.hasError()) {
                System.out.printf("Error during execution: %s%n", result.getError());
                messages.add(new ChatMessage(ChatMessageRole.USER.value(), String.format("""
                        Error during execution: %s
                                                                                   
                        Please fix the code while ensuring you:
                        1. Follow the exact service pipeline
                        2. Respect all service limitations
                        3. Handle the specific error encountered
                        4. Always annotate code as ```python
                        5. I cannot exchange parts of the code
                                                                                   
                        Service Pipeline Reference:
                        %s
                        """, result.getError(), pipelineInfo)));
            } else if (result.getOutput().toLowerCase().contains("error") || result.getOutput().toLowerCase().contains("failed") || result.getOutput().toLowerCase().contains("fail")) {
                System.out.printf("Error during execution: %s%n", result.getOutput());
                messages.add(new ChatMessage(ChatMessageRole.USER.value(), String.format("""
                        Error during execution: %s
                                                                                   
                        Please fix the code while ensuring you:
                        1. Follow the exact service pipeline
                        2. Respect all service limitations
                        3. Handle the specific error encountered
                                                                                   
                        Service Pipeline Reference:
                        %s
                        """, result.getOutput(), pipelineInfo)));
            } else {
                System.out.printf("Execution result: %s%n-------%n", result.getOutput());
                System.out.println("Python code executed successfully.");
                System.out.println("Number of needed iterations: " + (i + 1));
                messages.add(new ChatMessage(ChatMessageRole.USER.value(), "The code executed successfully. The result was: " + result.getOutput()));
                break;
            }
        }
    }

    private boolean isTextBasedFormat(String type) {
        return type.contains("text") || type.contains("json") || type.contains("xml") || type.contains("csv") || type.contains("yaml") || type.contains("html");
    }

    private String generateDetailedPipelineDescription() {
        if (pipeline == null || pipeline.isEmpty()) {
            return String.format("No specific %s services are required.", cloudProvider.toUpperCase());
        }

        StringBuilder description = new StringBuilder();
        description.append("The transformation must use the following ").append(cloudProvider.toUpperCase()).append(" services in this exact order.").append("Each service has specific capabilities and limitations that must be respected:\n");

        boolean videoConversionNeeded = false;

        for (int i = 0; i < pipeline.size(); i++) {
            CloudService service = pipeline.get(i);
            description.append(String.format("%d. %s%n", i + 1, service.getName()));

            // Input Capabilities
            description.append("   Accepts: ");
            if (service.getInputFormat() != null && !service.getInputFormat().isEmpty()) {
                description.append(String.join(", ", service.getInputFormat()));
            } else {
                description.append("No specific input type restrictions");
            }
            description.append("\n");

            // Output Capabilities
            description.append("   Produces: ");
            if (service.getOutputFormat() != null &&
                !service.getOutputFormat().isEmpty()) {
                description.append(
                    String.join(", ", service.getOutputFormat()));
            } else {
                description.append("No specific output type restrictions");
            }
            description.append("\n");

            // Detect video conversion requirement
            if (service.getName().toLowerCase().contains("mediaconvert") &&
                (service.getInputFormat().contains("MP4") ||
                 service.getOutputFormat().contains("MP3"))) {
                videoConversionNeeded = true;
            }

            // Add connection information to the next service if not the last service
            if (i < pipeline.size() - 1) {
                CloudService nextService = pipeline.get(i + 1);
                description.append("   Must connect to: ")
                           .append(nextService.getName());
            }

            description.append("\n");
        }

        description.append("Overall Pipeline Requirements:\n");
        description.append("1. Initial input: ").append(inputType)
                   .append(" in ").append(inputLanguage).append("\n");
        description.append("2. Final output: ").append(outputType)
                   .append(" in ").append(outputLanguage).append("\n");
        description.append(
                       "3. Each service must properly handle the output of the previous service ")
                   .append("and prepare input for the next service\n");
        description.append(
            "4. Implement proper error handling and validation between service transitions\n");

        // Append job settings if video conversion is needed
        if (videoConversionNeeded && false) {
            description.append("\nVideo Conversion Example Job Settings:\n").append(cloudProvider.equalsIgnoreCase("aws") ? VIDEO_CONVERSION_JOB_SETTINGS_AWS : VIDEO_CONVERSION_JOB_SETTINGS_GCP).append("\n");
        }

        return description.toString();
    }

    private String callLLMService() {
        return llmService.sendMessages(messages);
    }

    private void extractAndExecuteShellOrBashCode(String responseText) {
        Pattern pattern = Pattern.compile("```(bash|shell)(.*?)```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(responseText);

        while (matcher.find()) {
            String shellCode = matcher.group(2).trim();
            System.out.printf("Executing bash code:%n%s%n", shellCode);
            CodeExecutor.executeShellCode(shellCode);
        }
    }

    private String extractPythonCode(String responseText) {
        Pattern pattern = Pattern.compile("```python(.*?)```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(responseText);

        StringBuilder pythonCode = new StringBuilder();
        while (matcher.find()) {
            pythonCode.append(matcher.group(1).trim()).append("\n");
        }
        return pythonCode.toString();
    }
}