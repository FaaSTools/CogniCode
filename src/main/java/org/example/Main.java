package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import io.github.cdimascio.dotenv.Dotenv;
import org.apache.jena.rdf.model.Resource;
import org.example.executor.LLMRequestExecutor;
import org.example.llm.LLMService;
import org.example.llm.LLMServiceFactory;
import org.example.model.CloudService;
import org.example.model.Intent;
import org.example.ontology.OntologyPathFinder;
import org.example.ontology.OntologyToServiceMapper;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        String openAiApiKey = "";
        ObjectMapper objectMapper = new ObjectMapper();
        String filePath = "./input_aws.json";

        if (args.length > 0) {
            filePath = args[0];
        }

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

        Path ontologyBasePath = Paths.get(intent.getServiceFilePath());

        List<Resource> serviceChain = OntologyPathFinder.findValidServiceChain(intent.getInputType(),
                List.of(intent.getInputLanguage()),
                intent.getOutputType(),
                List.of(intent.getOutputLanguage()),
                ontologyBasePath);

        if (serviceChain.isEmpty()) {
            System.out.println("No pipeline found that supports the desired transformation process.");
        } else {
            System.out.println("=== Chain ===");
            serviceChain.forEach(r -> System.out.println(r.getLocalName()));
        }

        List<CloudService> pipeline = OntologyToServiceMapper.mapFrom(serviceChain);

        LLMService llmService =
            LLMServiceFactory.getLLMService(intent.getLlmProvider(), openAiApiKey);
        LLMRequestExecutor executor =
                new LLMRequestExecutor(llmService, intent, pipeline);
        executor.process();

        if (intent.getCloudProvider().equalsIgnoreCase("aws")) {
            deleteS3BucketContents(intent.getBucketName());
        } else if (intent.getCloudProvider().equalsIgnoreCase("gcp")) {
            deleteGCPBucketContents(intent.getBucketName());
        }
    }

    public static void deleteGCPBucketContents(String bucketName) {
        Storage storage = StorageOptions.getDefaultInstance().getService();

        storage.list(bucketName, Storage.BlobListOption.pageSize(1000))
           .iterateAll()
           .forEach(blob -> storage.delete(blob.getBlobId()));

        System.out.printf("All objects in bucket %s were deleted.%n", bucketName);
    }


    private static void deleteS3BucketContents(String bucketName) {
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
