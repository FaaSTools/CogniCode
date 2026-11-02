## System prompt
```
You are a helpful assistant who generates and executes bash and Python code. 
You should adhere to the specifified services pipeline, their order, and their limitations.
Ensure each service's input/output requirements are met and properly handled in the code. 
```


## User prompt (Query) 
```
Generate executable Python code for the following transformation task:

Source Format: pdf in english
Target Format: mp3 in english

Input File Information:
- File type: pdf
- File size: 33,46 KB
- File path: input.pdf
- Binary file detected
- PDF handling instructions: Use boto3's AWS Textract client for text extraction

Detailed Service Pipeline Configuration:
The transformation must use the following AWS services in this exact order.
Each service has specific capabilities and limitations that must be respected:

1. AWS Textract
   Accepts: PDF, TIFF, PNG
   Produces: Text, JSON, CSV
   Must connect to: AWS Polly
2. AWS Polly
   Accepts: Text, SSML
   Produces: MP3, PCM


Overall Pipeline Requirements:
1. Initial input: pdf in english
2. Final output: mp3 in english
3. Each service must properly handle the output of the previous service and prepare input for the next service
4. Implement proper error handling and validation between service transitions

Additional Requirements:
- AWS credentials are already configured
- If you need a bucket, use the following: hello-12345
- As default region please use: us-east-1
- If you need a role use the following: mediaconvert_trust
- The account id is the following: 717556240325
- Implement proper error handling for each service
- Validate input/output at each step
- For transcription I only want the text as a result not the whole transcription json content
Please generate code that follows this service pipeline, handles all limitations, and annotate it correctly with ```python.
You need to work autonomously and I cannot exchange parts of the code.
If the limits are surpassed by the input, you need to try to split the input before calling the service and merge it afterwards,
But only do the split if needed, first try it without splitting it.
Merging together after splitting is very important and should also be done on audio outputs!
If possible, save the output locally.
```
