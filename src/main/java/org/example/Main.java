package org.example;

import static org.example.config.OrchestratorModule.findPipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.example.config.ServiceRegistry;
import org.example.executor.LLMRequestExecutor;
import org.example.llm.LLMService;
import org.example.llm.LLMServiceFactory;
import org.example.model.CloudService;
import org.example.model.Intent;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

public class Main {

    public static void main(String[] args) {
        String openAiApiKey = "";
        ObjectMapper objectMapper = new ObjectMapper();
        String filePath = "./input.json";
        Intent intent = null;

        try {
            intent =
                objectMapper.readValue(new File(filePath), Intent.class);

            System.out.println(intent.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (intent != null && intent.getLlmProvider().toLowerCase().equals("openai")){
            Dotenv dotenv = Dotenv.load();
            openAiApiKey = dotenv.get("API_KEY");
        }

        List<CloudService> services =
            ServiceRegistry.createServicesFromJson(intent.getServiceFilePath());

        List<CloudService> pipeline =
            findPipeline(services, intent);

        if (pipeline.isEmpty()) {
            System.out.println(
                "No pipeline found that supports the desired transformation process.");
        } else {
            System.out.println("Found Pipeline:");
            for (CloudService service : pipeline) {
                System.out.println(service.getName());
            }
        }


        LLMService llmService =
            LLMServiceFactory.getLLMService(intent.getLlmProvider(), openAiApiKey);
        LLMRequestExecutor executor =
            new LLMRequestExecutor(llmService, intent, pipeline);
        executor.process();

        deleteBucketContents(intent.getBucketName());
    }
    private static void deleteBucketContents(String bucketName) {
        // Initialize the S3 client
        S3Client s3Client = S3Client.create();

        try {
            // List objects in the bucket
            ListObjectsV2Request listObjectsRequest =
                ListObjectsV2Request.builder()
                                    .bucket(bucketName)
                                    .build();

            ListObjectsV2Response listObjectsResponse =
                s3Client.listObjectsV2(listObjectsRequest);

            // Iterate through and delete each object
            for (S3Object s3Object : listObjectsResponse.contents()) {
                String key = s3Object.key();
                System.out.println("Deleting object: " + key);

                DeleteObjectRequest deleteObjectRequest =
                    DeleteObjectRequest.builder()
                                       .bucket(bucketName)
                                       .key(key)
                                       .build();

                s3Client.deleteObject(deleteObjectRequest);
            }

            System.out.println("All contents in the bucket '" + bucketName + "' have been deleted.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            s3Client.close();
        }
    }
}
