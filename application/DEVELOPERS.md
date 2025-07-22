# CogniCode Developer Documentation

## Project Structure

```
src/main/java/org/example/
  ├── config/         # Service factory logic
  ├── executor/       # Pipeline and code execution
  ├── llm/            # LLM service integrations
  ├── model/          # Data models for services and intents
  ├── ontology/       # Ontology and service mapping logic
  └── utlis/          # Utility classes
```

- **Main.java**: Entry point, loads configuration, builds pipeline, and starts execution.
- **CogniCode.jar**: Compiled application, run with input JSON.

---

## Core Concepts

- **Ontology-driven**: Service composition is based on ontology files (`services_aws.jsonld`, `services_gcp.jsonld`).
- **LLM-powered**: Uses LLMs (OpenAI, Claude, Llama, Mistral) to generate and refine code for service pipelines.
- **Iterative Execution**: Automatically retries/refines code if errors are detected.

---

## Main Components

### 1. Pipeline Construction

- **Intent**: Loaded from `input_{provider}.json`, describes the desired transformation.
- **OntologyPathFinder**: Finds a valid service chain for the requested transformation.
- **OntologyToServiceMapper**: Maps ontology resources to concrete service objects.

### 2. LLM Integration

- **LLMServiceFactory**: Instantiates the correct LLM service based on configuration.
- **LLMRequestExecutor**: Orchestrates prompt creation, LLM calls, code extraction, and execution.

### 3. Code Execution

- **CodeExecutor**: Runs generated Python or shell code, captures output/errors, and feeds back to the LLM if needed.

---

## How the Pipeline Works

1. **Startup**: `Main.java` loads the input JSON and parses the intent.
2. **Service Chain**: Uses ontology to find a valid sequence of services for the transformation.
3. **LLM Prompting**: Builds a detailed prompt describing the pipeline and requirements.
4. **Code Generation**: LLM generates Python/bash code for the pipeline.
5. **Execution & Feedback**: Code is executed; errors are detected and sent back to the LLM for refinement (up to 10 iterations).
6. **Output**: Final result is saved to the project directory.

---

## Extending CogniCode

### Adding/Modifying Cloud Services

1. **Ontology Update**:  
   - Edit `services_aws.jsonld` or `services_gcp.jsonld` to define new services or update existing ones.
   - Follow the ontology structure described in `ontology.md`.

2. **Model Update**:  
   - If a new service category is needed, add a new subclass of `CloudService` in `org.example.model`.
   - Update `CloudServiceFactory` to handle the new category.

3. **Service Logic**:  
   - If custom logic is needed for a new service, implement it in the appropriate model or executor class.

### Adding/Modifying LLM Providers

1. **Implement LLM Service**:  
   - Create a new class in `org.example.llm` implementing the `LLMService` interface.
   - See `OpenAiLLMService`, `ClaudeLLMService`, etc. for examples.

2. **Register in Factory**:  
   - Update `LLMServiceFactory.java` to return your new service for the appropriate provider string.

3. **Configuration**:  
   - Update your `input_{provider}.json` to use the new LLM provider.

### Modifying the Pipeline Logic

- **LLMRequestExecutor**:  
  - Update prompt templates or feedback logic as needed.
  - Adjust error handling or iteration logic for new requirements.

- **CodeExecutor**:  
  - Extend to support new execution environments or languages if needed.

---

## Ontology and Service Mapping

- **OntologyPathFinder**:  
  - Responsible for finding valid service chains based on input/output types and languages.
  - Update if you change the ontology structure or add new mapping logic.

- **OntologyToServiceMapper**:  
  - Maps ontology resources to `CloudService` objects.
  - Update if you add new service properties or categories.

---

## Testing and Debugging

- **Run Locally**:  
  - Use the provided input files and run with `java -jar CogniCode.jar input_aws.json`.
- **Logs**:  
  - Check terminal output for LLM prompts, responses, and execution results.
- **Error Handling**:  
  - The system will iterate and refine code on errors; inspect logs for details.
- **Manual Inspection**:  
  - If output is flagged as error but appears correct, check the output files directly.

---

## Best Practices

- **Follow Ontology Standards**:  
  - Ensure all new services and types are properly defined in the ontology.
- **Keep Prompts Clear**:  
  - When updating LLM prompts, be explicit about requirements and constraints.
- **Iterative Development**:  
  - Test new services and LLM providers with small inputs before scaling up.
- **Cost Awareness**:  
  - Be mindful of cloud service costs, especially for large-scale conversions.

---

For more details on the ontology and service definitions, see `ontology.md`.
