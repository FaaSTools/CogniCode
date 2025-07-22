import boto3
import os
import sys
import time
from datetime import datetime
from botocore.exceptions import BotoCoreError, ClientError
import requests  # For downloading the transcription from URL

# Define constants
S3_BUCKET = "hello-12345"
REGION = "us-east-1"
ROLE = "arn:aws:iam::717556240325:role/mediaconvert_trust"
INPUT_FILE_PATH = "input.mp4"
OUTPUT_AUDIO_FILENAME = "output_audio"
OUTPUT_TRANSCRIPTION_FILENAME = "output_transcription.txt"

def upload_file_to_s3(file_path, bucket, key):
    s3_client = boto3.client('s3', region_name=REGION)
    try:
        s3_client.upload_file(file_path, bucket, key)
        print(f"Uploaded {file_path} to s3://{bucket}/{key}")
    except (BotoCoreError, ClientError) as e:
        print(f"Error uploading file to S3: {e}")
        sys.exit(1)

def get_mediaconvert_endpoint():
    mediaconvert_client = boto3.client('mediaconvert', region_name=REGION)
    try:
        response = mediaconvert_client.describe_endpoints()
        endpoint_url = response['Endpoints'][0]['Url']
        print(f"MediaConvert endpoint URL: {endpoint_url}")
        return endpoint_url
    except (BotoCoreError, ClientError) as e:
        print(f"Error discovering MediaConvert endpoint: {e}")
        sys.exit(1)

def create_media_convert_job(endpoint_url, input_s3_path, output_s3_path):
    mediaconvert_client = boto3.client('mediaconvert', region_name=REGION, endpoint_url=endpoint_url)
    try:
        response = mediaconvert_client.create_job(
            Role=ROLE,
            Settings={
                "Inputs": [
                    {
                        "AudioSelectors": {
                            "Audio Selector 1": {
                                "DefaultSelection": "DEFAULT"
                            }
                        },
                        "TimecodeSource": "ZEROBASED",
                        "FileInput": input_s3_path
                    }
                ],
                "OutputGroups": [
                    {
                        "Name": "File Group",
                        "Outputs": [
                            {
                                "ContainerSettings": {
                                    "Container": "RAW"
                                },
                                "AudioDescriptions": [
                                    {
                                        "AudioSourceName": "Audio Selector 1",
                                        "CodecSettings": {
                                            "Codec": "MP3",
                                            "Mp3Settings": {
                                                "Bitrate": 96000,
                                                "SampleRate": 48000,
                                                "RateControlMode": "CBR",
                                                "Channels": 2
                                            }
                                        }
                                    }
                                ]
                            }
                        ],
                        "OutputGroupSettings": {
                            "Type": "FILE_GROUP_SETTINGS",
                            "FileGroupSettings": {
                                "Destination": output_s3_path
                            }
                        }
                    }
                ]
            }
        )
        job_id = response['Job']['Id']
        print(f"MediaConvert job created with ID: {job_id}")
        return job_id
    except (BotoCoreError, ClientError) as e:
        print(f"Error creating MediaConvert job: {e}")
        sys.exit(1)

def wait_for_media_convert_job(mediaconvert_client, job_id):
    try:
        while True:
            response = mediaconvert_client.get_job(Id=job_id)
            status = response['Job']['Status']
            if status in ['COMPLETE', 'ERROR']:
                return status
            print(f"MediaConvert job status: {status}")
            time.sleep(15)  # Adding a delay to reduce the frequency of status checks
    except (BotoCoreError, ClientError) as e:
        print(f"Error checking MediaConvert job status: {e}")
        sys.exit(1)

def transcribe_audio_file(s3_audio_path):
    transcribe_client = boto3.client('transcribe', region_name=REGION)
    # Generate a unique job name using timestamp
    job_name = f"transcription_job_{datetime.now().strftime('%Y%m%d%H%M%S')}"
    try:
        transcribe_client.start_transcription_job(
            TranscriptionJobName=job_name,
            Media={'MediaFileUri': s3_audio_path},
            MediaFormat='mp3',
            LanguageCode='en-US'
        )

        # Wait for the transcription job to complete
        while True:
            status = transcribe_client.get_transcription_job(TranscriptionJobName=job_name)
            if status['TranscriptionJob']['TranscriptionJobStatus'] in ['COMPLETED', 'FAILED']:
                break
            print("Waiting for transcription job to complete...")
            time.sleep(15)  # Adding a delay between status checks

        if status['TranscriptionJob']['TranscriptionJobStatus'] == 'COMPLETED':
            transcript_uri = status['TranscriptionJob']['Transcript']['TranscriptFileUri']
            # Download the transcription from the provided URI
            transcription_response = requests.get(transcript_uri)
            transcription = transcription_response.json()['results']['transcripts'][0]['transcript']
            print("Transcription completed successfully.")
            return transcription
    except (BotoCoreError, ClientError) as e:
        print(f"Error in transcription process: {e}")
        sys.exit(1)

def save_transcription_to_file(transcription, file_path):
    try:
        with open(file_path, 'w') as file:
            file.write(transcription)
        print(f"Transcription saved to {file_path}")
    except IOError as e:
        print(f"Error saving transcription to file: {e}")
        sys.exit(1)

def main():
    # Step 1: Upload input file to S3
    input_s3_key = os.path.basename(INPUT_FILE_PATH)
    upload_file_to_s3(INPUT_FILE_PATH, S3_BUCKET, input_s3_key)

    # Step 2: Discover MediaConvert endpoint
    endpoint_url = get_mediaconvert_endpoint()

    # Step 3: Create MediaConvert job
    input_s3_path = f"s3://{S3_BUCKET}/{input_s3_key}"
    output_s3_path = f"s3://{S3_BUCKET}/{OUTPUT_AUDIO_FILENAME}"
    job_id = create_media_convert_job(endpoint_url, input_s3_path, output_s3_path)

    # Step 4: Wait for MediaConvert job to complete
    status = wait_for_media_convert_job(boto3.client('mediaconvert', region_name=REGION, endpoint_url=endpoint_url), job_id)
    if status != 'COMPLETE':
        print("MediaConvert job did not complete successfully.")
        sys.exit(1)

    # Step 5: Transcribe audio file
    output_audio_s3_path = f"{output_s3_path}.mp3"
    transcription_text = transcribe_audio_file(output_audio_s3_path)

    # Step 6: Save transcription to file
    save_transcription_to_file(transcription_text, OUTPUT_TRANSCRIPTION_FILENAME)

if __name__ == "__main__":
    main()

