import boto3
import time
import os
from pydub import AudioSegment

# Define constants
BUCKET_NAME = 'hello-12345'
REGION_NAME = 'us-east-1'
ROLE_ARN = 'arn:aws:iam::717556240325:role/mediaconvert_trust'
INPUT_FILE_PATH = 'input.pdf'
OUTPUT_FILE_PATH = 'output.mp3'

# Initialize AWS clients
textract_client = boto3.client('textract', region_name=REGION_NAME)
polly_client = boto3.client('polly', region_name=REGION_NAME)
s3_client = boto3.client('s3', region_name=REGION_NAME)

# Upload the PDF to S3
def upload_to_s3(file_path, bucket_name, object_name):
    try:
        s3_client.upload_file(file_path, bucket_name, object_name)
        print(f"File {file_path} uploaded to {bucket_name}/{object_name}")
        return object_name
    except Exception as e:
        print(f"Error uploading to S3: {e}")
        return None

# Extract text from PDF using AWS Textract
def extract_text_from_pdf(s3_object_name):
    try:
        response = textract_client.start_document_text_detection(
            DocumentLocation={
                'S3Object': {
                    'Bucket': BUCKET_NAME,
                    'Name': s3_object_name
                }
            }
        )
        job_id = response['JobId']
        print(f"Textract job started with ID: {job_id}")

        while True:
            status = textract_client.get_document_text_detection(JobId=job_id)
            if status['JobStatus'] in ['SUCCEEDED', 'FAILED']:
                break
            print("Waiting for Textract job to complete...")
            time.sleep(5)

        if status['JobStatus'] == 'SUCCEEDED':
            text = ""
            for block in status['Blocks']:
                if block['BlockType'] == 'LINE':
                    text += block['Text'] + " "
            return text.strip()
        else:
            print(f"Textract job failed with status: {status['JobStatus']}")
            return None
    except Exception as e:
        print(f"Error in Textract: {e}")
        return None

# Split text into chunks that Polly can handle
def split_text(text, max_length=3000):
    words = text.split()
    chunks = []
    current_chunk = []
    current_length = 0

    for word in words:
        if current_length + len(word) + 1 > max_length:
            chunks.append(' '.join(current_chunk))
            current_chunk = [word]
            current_length = len(word)
        else:
            current_chunk.append(word)
            current_length += len(word) + 1

    if current_chunk:
        chunks.append(' '.join(current_chunk))

    return chunks

# Convert text to speech using AWS Polly
def convert_text_to_speech(text_chunks):
    audio_segments = []
    for i, chunk in enumerate(text_chunks):
        try:
            response = polly_client.synthesize_speech(
                Text=chunk,
                OutputFormat='mp3',
                VoiceId='Joanna'
            )
            audio_path = f'chunk_{i}.mp3'
            with open(audio_path, 'wb') as f:
                f.write(response['AudioStream'].read())
            audio_segments.append(AudioSegment.from_mp3(audio_path))
            os.remove(audio_path)  # Clean up temporary file
        except Exception as e:
            print(f"Error in Polly for chunk {i}: {e}")

    return audio_segments

# Merge audio segments into a single file
def merge_audio_segments(audio_segments, output_path):
    combined = audio_segments[0]
    for segment in audio_segments[1:]:
        combined += segment
    combined.export(output_path, format='mp3')
    print(f"Audio segments merged and saved as {output_path}")

# Main function to execute the pipeline
def main():
    # Step 1: Upload the PDF to S3
    s3_object_name = upload_to_s3(INPUT_FILE_PATH, BUCKET_NAME, 'input.pdf')
    if not s3_object_name:
        return

    # Step 2: Extract text from the PDF using AWS Textract
    extracted_text = extract_text_from_pdf(s3_object_name)
    if not extracted_text:
        return

    # Step 3: Split text into chunks that Polly can handle
    text_chunks = split_text(extracted_text)

    # Step 4: Convert the text chunks to speech using AWS Polly
    audio_segments = convert_text_to_speech(text_chunks)

    # Step 5: Merge the audio segments into a single file
    merge_audio_segments(audio_segments, OUTPUT_FILE_PATH)

if __name__ == "__main__":
    main()
