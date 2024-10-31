package org.example.executor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CodeExecutor {

    public static ExecutionResult executePythonCode(String pythonCode) {
        if (pythonCode.isEmpty()) {
            return new ExecutionResult("",
                                       "No code block found in the response.");
        }

        try {
            Path tempFile = Files.createTempFile("script", ".py");
            Files.writeString(tempFile, pythonCode);

            Process process = Runtime.getRuntime().exec(
                new String[]{"python", tempFile.toString()});
            String output = new String(process.getInputStream().readAllBytes());
            String error = new String(process.getErrorStream().readAllBytes());

            Files.delete(tempFile);
            return new ExecutionResult(output, error);
        } catch (IOException e) {
            return new ExecutionResult("", e.getMessage());
        }
    }

    public static void executeShellCode(String bashCode) {
        try {
            Process process =
                Runtime.getRuntime().exec(new String[]{"bash", "-c", bashCode});
            String output = new String(process.getInputStream().readAllBytes());
            String error = new String(process.getErrorStream().readAllBytes());

            if (!error.isEmpty()) {
                System.out.printf("Error executing bash command: %s%n", error);
            } else {
                System.out.printf(
                    "Bash command executed successfully: %s%n-------%n",
                    output);
            }
        } catch (IOException e) {
            System.out.printf("Error while executing bash code: %s%n",
                              e.getMessage());
        }
    }
}
