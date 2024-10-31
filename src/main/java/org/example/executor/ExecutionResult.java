package org.example.executor;

public class ExecutionResult {

    private final String output;
    private final String error;

    public ExecutionResult(String output, String error) {
        this.output = output;
        this.error = error;
    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }

    public boolean hasError() {
        return !error.isEmpty();
    }
}
