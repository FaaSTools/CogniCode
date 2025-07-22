package org.example.utlis;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtils {

    public static String readTextFile(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    public static byte[] readBinaryFile(String path) throws IOException {
        return Files.readAllBytes(Path.of(path));
    }

    public static long getFileSize(String path) throws IOException {
        return Files.size(Path.of(path));
    }

    public static String formatFileSize(long size) {
        final String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double fileSize = size;

        while (fileSize >= 1024 && unitIndex < units.length - 1) {
            fileSize /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", fileSize, units[unitIndex]);
    }
    public static String getInputFileDescription(String inputType, long inputFileSize,
                                           String inputFilePath,
                                           String inputFileTextContent) {
        StringBuilder description = new StringBuilder();
        description.append("Input File Information:\n");
        description.append("- File type: ").append(inputType).append("\n");
        description.append("- File size: ")
                   .append(FileUtils.formatFileSize(inputFileSize))
                   .append("\n");
        description.append("- File path: ").append(inputFilePath).append("\n");

        if (inputFileTextContent != null) {
            String preview = inputFileTextContent.length() > 1000
                             ? inputFileTextContent.substring(0, 1000) +
                               "... (truncated)"
                             : inputFileTextContent;
            description.append("- Content preview:\n").append(preview)
                       .append("\n");
        } else {
            description.append("- Binary file detected\n");
            if (inputType.toLowerCase().contains("pdf")) {
                description.append(
                    "- PDF handling instructions: Use boto3's AWS Textract client for text extraction\n");
            } else if (inputType.toLowerCase().contains("image")) {
                description.append(
                    "- Image handling instructions: Use boto3's AWS Rekognition or Textract as appropriate\n");
            }
        }

        return description.toString();
    }
}