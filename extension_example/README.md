# Service-extension example

This example adds one service to the ontology and shows its effect on composition planning.
It uses only the planning part of CogniCode, so it needs no LLM and no cloud access.

## Change

[`services_aws_extended.jsonld`](./services_aws_extended.jsonld) is a copy of
[`application/services_aws.jsonld`](../application/services_aws.jsonld) with one added entry for AWS Comprehend, a sentiment-analysis service
that accepts `dt:Text` and produces `dt:JSON`. The added entry is the only difference between the two files. No code was changed.

## Run

Requires JDK 21. From this folder:

```bash
javac -cp ../application/CogniCode.jar ExtensionDemo.java
java -cp ../application/CogniCode.jar:. ExtensionDemo
```

On Windows, use `;` instead of `:` in the classpath.

## Result

The composition plans of the three evaluated use cases are unchanged:

- UC1: `PDF -> AWSTextract -> Text -> AWSPolly -> MP3`
- UC2: `MP3 -> AWSTranscribe -> Text -> AWSTranslate -> Text -> AWSPolly -> MP3`
- UC3: `MP4 -> AWSElementalMediaConvert -> MP3 -> AWSTranscribe -> Text`

A new intent, English MP3 to sentiment JSON, has no plan with the original ontology. With the extended ontology, it resolves to

- `MP3 -> AWSTranscribe -> Text -> AWSComprehend -> JSON`

Data types and services alternate: Transcribe turns the MP3 into text, and Comprehend turns the text into a sentiment result in JSON.
The full output is in [`output.txt`](./output.txt).
