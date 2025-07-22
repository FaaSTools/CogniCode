import os
import time
import sys
import boto3

# Constants
PDF_FILE_PATH = 'input.pdf'
OUTPUT_MP3_PATH = 'output.mp3'
BUCKET_NAME = 'hello-12345'
REGION_NAME = 'us-east-1'
ROLE_ARN = 'arn:aws:iam::717556240325:role/mediaconvert_trust'

# Initialize Boto3 clients
textract_client = boto3.client('textract', region_name=REGION_NAME)
polly_client = boto3.client('polly', region_name=REGION_NAME)
s3_client = boto3.client('s3', region_name=REGION_NAME)

def extract_text_from_pdf(file_path):
    try:
        # Upload the PDF to S3
        s3_client.upload_file(file_path, BUCKET_NAME, 'input.pdf')

        # Start Textract job
        response = textract_client.start_document_text_detection(
            DocumentLocation={
                'S3Object': {
                    'Bucket': BUCKET_NAME,
                    'Name': 'input.pdf'
                }
            }
        )
        job_id = response['JobId']
        print(f"Started Textract job with ID: {job_id}")

        # Wait for Textract job to complete
        while True:
            response = textract_client.get_document_text_detection(JobId=job_id)
            status = response['JobStatus']
            if status in ['SUCCEEDED', 'FAILED']:
                break
            print("Waiting for Textract job to complete...")
            time.sleep(5)

        if status == 'SUCCEEDED':
            text = ''
            pages = []
            while True:
                for block in response['Blocks']:
                    if block['BlockType'] == 'LINE':
                        pages.append(block['Text'])
                if 'NextToken' in response:
                    response = textract_client.get_document_text_detection(JobId=job_id, NextToken=response['NextToken'])
                else:
                    break
            text = ' '.join(pages)
            return text
        else:
            raise Exception("Textract job failed")
    except Exception as e:
        print(f"Error extracting text from PDF: {e}")
        sys.exit(1)

def synthesize_speech(text):
    try:
        response = polly_client.start_speech_synthesis_task(
            OutputS3BucketName=BUCKET_NAME,
            OutputFormat='mp3',
            Text=text,
            VoiceId='Joanna'
        )
        task_id = response['SynthesisTask']['TaskId']
        print(f"Started Polly synthesis task with ID: {task_id}")

        # Wait for Polly synthesis task to complete
        while True:
            response = polly_client.get_speech_synthesis_task(TaskId=task_id)
            status = response['SynthesisTask']['TaskStatus']
            if status in ['completed', 'failed']:
                break
            print("Waiting for Polly synthesis task to complete...")
            time.sleep(5)

        if status == 'completed':
            s3_object_path = response['SynthesisTask']['OutputUri']
            return s3_object_path
        else:
            raise Exception("Polly synthesis task failed")
    except Exception as e:
        print(f"Error synthesizing speech: {e}")
        sys.exit(1)

def download_mp3_from_s3(s3_object_path, download_path):
    try:
        s3_object_key = s3_object_path.split(f"/{BUCKET_NAME}/")[1]
        s3_client.download_file(BUCKET_NAME, s3_object_key, download_path)
        print(f"MP3 downloaded to {download_path}")
    except Exception as e:
        print(f"Error downloading MP3: {e}")
        sys.exit(1)

def main():
    # Step 1: Extract text from PDF
    text = extract_text_from_pdf(PDF_FILE_PATH)

    # Step 2: Convert text to MP3 using Polly
    s3_object_path = synthesize_speech(text)

    # Step 3: Download MP3 from S3
    download_mp3_from_s3(s3_object_path, OUTPUT_MP3_PATH)

if __name__ == "__main__":
    main()
