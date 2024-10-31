# ServelessIntent Project  

**ServelessIntent** is a tool designed in the course of a masters thesis to help developers seamlessly leverage Backend-as-a-Service (BaaS) solutions for automated file format and language conversion.  

## ✨ Key Features  

- Utilizes an **ontology-based** `services.json` file to define supported BaaS services.  
- **Automatically detects** and constructs a pipeline of BaaS services to convert files from one format/language to another.  
- Uses **LLM-powered automation** to generate and execute conversions dynamically.  
- Implements an **iterative feedback loop**, where failed conversions are analyzed, refined, and re-executed until successful.  

By streamlining the conversion process, **ServelessIntent** eliminates manual intervention, making it easier to integrate file and language transformation services into your applications.  

## 🚀 Getting Started  

This guide walks you through setting up **ServelessIntent**, configuring AWS access, and defining your desired file conversions.  

---

### 📂 Prerequisites  

Before using **ServelessIntent**, ensure you have the following:  

1. **AWS CLI Installed** – Follow the [AWS CLI installation guide](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html).  

2. **AWS Access & Permissions**  

   - AWS Access Key ID & Secret Key configured (`aws configure`).  
   - Proper IAM permissions to use AWS BaaS services, Bedrock, and your chosen LLM.  

3. **Example Files Provided**  

   - The repository includes `services.json` (predefined AWS services) and example inputs:  
     - `input.pdf`  
     - `input.mp3`  
     - `input.mp4`  
     - `input.txt`  
   - You can add your own input files, but **the file name must match the `inputFilePath` field** in `input.json`.  

4. **OpenAI API Key (Optional)**  

   - If using OpenAI, rename `.env.example` to `.env` and add your API key inside:  

     ```ini
     API_KEY=your-openai-api-key
     ```

---

### 🛠 Configuration  

All configuration is managed inside `input.json`. Below is an example:  

```json
{
  "inputType": "pdf",
  "inputLanguage": "english",
  "outputType": "mp3",
  "outputLanguage": "english",
  "inputFilePath": "input.pdf",
  "bucketName": "<your-bucket-name>",
  "serviceFilePath": "services.json",
  "llmProvider": "llama"
}
```

#### 🔹 Configuration Fields Explained  

| **Field**         | **Description**                                              |
| ----------------- | ------------------------------------------------------------ |
| `inputType`       | The format of the input file (e.g., `pdf`, `mp3`, `mp4`, `txt`). |
| `inputLanguage`   | Language of the input file (e.g., `english`, `german`).      |
| `outputType`      | Desired output file format (e.g., `mp3`, `txt`).             |
| `outputLanguage`  | Language for the output file.                                |
| `inputFilePath`   | Name of the input file (must match an existing file).        |
| `bucketName`      | AWS S3 bucket for storage (required for certain services).   |
| `serviceFilePath` | Path to `services.json` (leave as-is unless modifying services). |
| `llmProvider`     | LLM service to use (`openai`, `llama`, `claude`, `mistral`). |

---

### 🤖 Adjusting the LLM Model  

To specify different LLM versions, update the **`LLMServiceFactory.java`** file:  

```java
return switch (normalizedType) {
    case "openai" -> new OpenAiLLMService(apiKey);
    case "claude" -> new ClaudeLLMService(Region.US_EAST_1.toString(),
                                          "us.anthropic.claude-3-5-sonnet-20241022-v2:0");
    case "llama" -> new LlamaLLMService(Region.US_EAST_1.toString(),
                                        "us.meta.llama3-1-70b-instruct-v1:0");
    case "mistral" -> new Mistral2LLMService(Region.US_WEST_2.toString(),
                                              "mistral.mistral-large-2407-v1:0");
    default -> throw new IllegalArgumentException("Unknown LLM service type");
};
```

Modify the **model identifier** based on the version you wish to use.

## 🎯 Run the Application  

Once you have completed the configuration, you can run the Gradle project using the **IDE of your choice**.  

### 🔹 What to Expect in the Output  

- The terminal will display the **current iteration** of the application.  
- You'll see the **LLM responses**, including extracted Python code.  
- If an error occurs, you can inspect the output logs to understand what went wrong.  
- The final **output file** will be saved in the **root of the project folder**.  

---

## ⚠️ Disclaimer  

### 🔸 Potential Issues & How to Handle Them  

1. **Early Termination of Execution**  
   - The application may **incorrectly mark a run as successful** if the LLM does not wrap Python code inside triple backticks (`'''python`).  
   - The initial prompt explicitly asks the LLM to start its response with `'''python`, but if some models fail to do so, the issue is on their end.  

2. **False Error Detection**  
   - If the output contains **words like "Error" or "failed"**, the application might assume the execution failed, even if the output is actually correct.  
   - If everything **appears successful but the application flags an error**, check the project folder manually for a valid output file.  
   - This was a **trade-off decision** to avoid falsely marking incomplete executions as successful.  

3. **Transcription Output Issue**  
   - Occasionally, AWS Transcribe may return the **entire transcription_job.json** instead of just the transcript.  
   - If this happens, you’ll need to **manually extract the transcript text** from the file.  
   - This issue was observed with **Mistral and OpenAI** in some cases.  

---

## 💰 Avoiding High Costs  

- **AWS Translation Services** are significantly **more expensive** than other BaaS services.  
- Avoid using **large inputs** when running **text translations** to **minimize costs**.  

---

## ✅ Tested Conversion Workflows  

The following **three workflows** have been tested extensively and optimized. Other conversions may work but were not tested extensively so there could be flaws.

### 🔹 **Workflow 1: MP3 (Language 1) → Text (Language 2) → MP3**  

- **Speech-to-Text** (Transcription)  
- **Translate** (Text Translation)  
- **Text-to-Speech** (Generate Audio Output)  

### 🔹 **Workflow 2: MP4 → Text**  

- **Upload to S3** (Storage)  
- **Elemental MediaConvert** (Extract Audio from Video)  
- **Speech-to-Text** (Transcription)  

### 🔹 **Workflow 3: PDF → MP3**  

- **OCR** (Extract Text from PDF)  
- **Text-to-Speech** (Convert Text to Audio)  

---

