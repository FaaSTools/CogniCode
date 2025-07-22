import boto3
import os
import time
import uuid
import json

# Constants
BUCKET_NAME = 'hello-12345'
REGION = 'us-east-1'
ROLE = 'mediaconvert_trust'
ACCOUNT_ID = '717556240325'
INPUT_FILE = 'input.mp3'
OUTPUT_FILE = 'output.mp3'
MAX_TRANS_JOB_SIZE = 87102 * 1024  # 87102 KB in bytes

# Initialize AWS clients
transcribe_client = boto3.client('transcribe', region_name=REGION)
translate_client = boto3.client('translate', region_name=REGION)
polly_client = boto3.client('polly', region_name=REGION)
s3_client = boto3.client('s3', region_name=REGION)

def upload_to_s3(file_name, bucket_name, object_name=None):
    if object_name is None:
        object_name = file_name
    s3_client.upload_file(file_name, bucket_name, object_name)
    return f"s3://{bucket_name}/{object_name}"

def transcribe_audio(job_name, file_uri, language_code='en-US'):
    try:
        response = transcribe_client.start_transcription_job(
            TranscriptionJobName=job_name,
            Media={'MediaFileUri': file_uri},
            MediaFormat='mp3',
            LanguageCode=language_code,
            OutputBucketName=BUCKET_NAME
        )
    except transcribe_client.exceptions.ConflictException:
        print("Job name conflict. Generating a new job name.")
        job_name = str(uuid.uuid4())
        response = transcribe_client.start_transcription_job(
            TranscriptionJobName=job_name,
            Media={'MediaFileUri': file_uri},
            MediaFormat='mp3',
            LanguageCode=language_code,
            OutputBucketName=BUCKET_NAME
        )

    while True:
        status = transcribe_client.get_transcription_job(TranscriptionJobName=job_name)
        if status['TranscriptionJob']['TranscriptionJobStatus'] in ['COMPLETED', 'FAILED']:
            break
        time.sleep(5)

    if status['TranscriptionJob']['TranscriptionJobStatus'] == 'FAILED':
        raise Exception("Transcription job failed.")

    transcript_file_uri = status['TranscriptionJob']['Transcript']['TranscriptFileUri']
    transcript_file_key = os.path.basename(transcript_file_uri)
    s3_client.download_file(BUCKET_NAME, transcript_file_key, 'transcript.json')
    with open('transcript.json') as f:
        transcript_data = json.load(f)
    return transcript_data['results']['transcripts'][0]['transcript']

def translate_text(text, source_language_code='en', target_language_code='de'):
    response = translate_client.translate_text(
        Text=text,
        SourceLanguageCode=source_language_code,
        TargetLanguageCode=target_language_code
    )
    return response['TranslatedText']

def polly_speech(text, output_format='mp3', voice_id='Hans'):
    response = polly_client.synthesize_speech(
        Text=text,
        OutputFormat=output_format,
        VoiceId=voice_id
    )
    with open(OUTPUT_FILE, 'wb') as f:
        f.write(response['AudioStream'].read())

def main():
    # Step 1: Upload input file to S3
    input_file_uri = upload_to_s3(INPUT_FILE, BUCKET_NAME)

    # Step 2: Transcribe audio using AWS Transcribe
    job_name = str(uuid.uuid4())  # Generate a unique job name
    try:
        transcription = transcribe_audio(job_name, input_file_uri)
    except Exception as e:
        print(f"Error in transcription: {e}")
        return

    # Step 3: Translate text using AWS Translate
    try:
        translation = translate_text(transcription)
    except Exception as e:
        print(f"Error in translation: {e}")
        return

    # Step 4: Convert text to speech using AWS Polly
    try:
        polly_speech(translation)
    except Exception as e:
        print(f"Error in speech synthesis: {e}")
        return

    print(f"Output file saved as {OUTPUT_FILE}")

if __name__ == "__main__":
    main()
