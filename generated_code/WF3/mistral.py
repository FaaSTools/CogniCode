import boto3
import time
import os
import json
import uuid

# AWS configuration
s3_bucket = 'hello-12345'
region = 'us-east-1'
account_id = '717556240325'
role = f'arn:aws:iam::{account_id}:role/mediaconvert_trust'

# Initialize boto3 clients
mediaconvert_client = boto3.client('mediaconvert', region_name=region, endpoint_url='https://mediaconvert.{region}.amazonaws.com'.format(region=region))
transcribe_client = boto3.client('transcribe', region_name=region)
s3_client = boto3.client('s3', region_name=region)

# Input and output file paths
input_filename = 'input.mp4'
output_filename_without_file_extension = 'output'
output_filename_with_file_extension = output_filename_without_file_extension + '.mp3'

# Upload input file to S3
try:
    s3_client.upload_file(input_filename, s3_bucket, input_filename)
    print(f"Uploaded {input_filename} to S3 bucket {s3_bucket}")
except Exception as e:
    print(f"Error uploading file to S3: {e}")
    raise

# MediaConvert job settings
job_settings = {
    "Inputs": [
        {
            "AudioSelectors": {
                "Audio Selector 1": {
                    "DefaultSelection": "DEFAULT"
                }
            },
            "TimecodeSource": "ZEROBASED",
            "FileInput": f"s3://{s3_bucket}/{input_filename}"
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
                    "Destination": f"s3://{s3_bucket}/{output_filename_without_file_extension}"
                }
            }
        }
    ]
}

# Create MediaConvert job
try:
    response = mediaconvert_client.create_job(
        Role=role,
        Settings=job_settings
    )
    job_id = response['Job']['Id']
    print(f"MediaConvert job created with ID: {job_id}")
except Exception as e:
    print(f"Error creating MediaConvert job: {e}")
    raise

# Wait for MediaConvert job to complete
while True:
    try:
        job_status = mediaconvert_client.get_job(Id=job_id)
        if job_status['Job']['Status'] == 'COMPLETE':
            print("MediaConvert job completed successfully")
            break
        elif job_status['Job']['Status'] == 'ERROR':
            raise Exception(f"MediaConvert job failed with error: {job_status['Job']['StatusDetail']}")
        time.sleep(30)
    except Exception as e:
        print(f"Error checking MediaConvert job status: {e}")
        raise

# Download the converted audio file from S3
try:
    s3_client.download_file(s3_bucket, output_filename_with_file_extension, output_filename_with_file_extension)
    print(f"Downloaded converted audio file: {output_filename_with_file_extension}")
except Exception as e:
    print(f"Error downloading converted audio file from S3: {e}")
    raise

# Start transcription job
transcribe_job_name = 'transcribe_job_' + str(uuid.uuid4())
try:
    transcribe_job_response = transcribe_client.start_transcription_job(
        TranscriptionJobName=transcribe_job_name,
        Media={'MediaFileUri': f's3://{s3_bucket}/{output_filename_with_file_extension}'},
        MediaFormat='mp3',
        LanguageCode='en-US',
        OutputBucketName=s3_bucket
    )
    print(f"Transcribe job started with name: {transcribe_job_name}")
except Exception as e:
    print(f"Error starting Transcribe job: {e}")
    raise

# Wait for Transcribe job to complete
while True:
    try:
        transcribe_job_status = transcribe_client.get_transcription_job(TranscriptionJobName=transcribe_job_name)
        if transcribe_job_status['TranscriptionJob']['TranscriptionJobStatus'] == 'COMPLETED':
            print("Transcribe job completed successfully")
            break
        elif transcribe_job_status['TranscriptionJob']['TranscriptionJobStatus'] == 'FAILED':
            raise Exception(f"Transcribe job failed with error: {transcribe_job_status['TranscriptionJob']['FailureReason']}")
        time.sleep(30)
    except Exception as e:
        print(f"Error checking Transcribe job status: {e}")
        raise

# Download the transcription result from S3
transcription_result_key = transcribe_job_name + '.json'
try:
    s3_client.download_file(s3_bucket, transcription_result_key, transcription_result_key)
    print(f"Downloaded transcription result: {transcription_result_key}")
except Exception as e:
    print(f"Error downloading transcription result from S3: {e}")
    raise

# Extract text from transcription result
try:
    with open(transcription_result_key, 'r') as f:
        transcription_result = json.load(f)
    transcription_text = transcription_result['results']['transcripts'][0]['transcript']
    print("Extracted transcription text")
except Exception as e:
    print(f"Error extracting transcription text: {e}")
    raise

# Save the transcription text locally
try:
    with open('transcription_text.txt', 'w') as f:
        f.write(transcription_text)
    print("Transcription text saved to transcription_text.txt")
except Exception as e:
    print(f"Error saving transcription text locally: {e}")
    raise
