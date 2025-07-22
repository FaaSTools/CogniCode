import boto3
import time
import json
import os
import urllib.request

# Configuration
region = 'us-east-1'
bucket_name = 'hello-12345'
input_file = 'input.mp3'
output_transcription_file = 'transcription.txt'
translated_text_file = 'translated_text.txt'
final_output_mp3 = 'output.mp3'

# Initialize AWS clients
transcribe_client = boto3.client('transcribe', region_name=region)
translate_client = boto3.client('translate', region_name=region)
polly_client = boto3.client('polly', region_name=region)
s3_client = boto3.client('s3', region_name=region)

def upload_to_s3(file_name, bucket, object_name=None):
    if object_name is None:
        object_name = os.path.basename(file_name)
    try:
        s3_client.upload_file(file_name, bucket, object_name)
        print(f"File {file_name} uploaded to {bucket}/{object_name}.")
        return f"s3://{bucket}/{object_name}"
    except Exception as e:
        print(f"Failed to upload {file_name} to {bucket}. Error: {str(e)}")
        return None

def transcribe_audio(file_uri):
    transcription_job_name = f"TranscriptionJob-{int(time.time())}"
    try:
        transcribe_client.start_transcription_job(
            TranscriptionJobName=transcription_job_name,
            Media={'MediaFileUri': file_uri},
            MediaFormat='mp3',
            LanguageCode='en-US'
        )
        
        while True:
            status = transcribe_client.get_transcription_job(TranscriptionJobName=transcription_job_name)
            if status['TranscriptionJob']['TranscriptionJobStatus'] in ['COMPLETED', 'FAILED']:
                break
            print("Waiting for transcription to complete...")
            time.sleep(10)
        
        if status['TranscriptionJob']['TranscriptionJobStatus'] == 'COMPLETED':
            transcription_url = status['TranscriptionJob']['Transcript']['TranscriptFileUri']
            with urllib.request.urlopen(transcription_url) as response:
                transcription_data = json.loads(response.read())
                transcript_text = transcription_data['results']['transcripts'][0]['transcript']
                with open(output_transcription_file, 'w') as f:
                    f.write(transcript_text)
                print("Transcription completed.")
                return transcript_text
        else:
            print("Transcription failed.")
            return None
    except Exception as e:
        print(f"Error during transcription: {str(e)}")
        return None

def translate_text(text, source_lang='en', target_lang='de'):
    try:
        response = translate_client.translate_text(
            Text=text,
            SourceLanguageCode=source_lang,
            TargetLanguageCode=target_lang
        )
        translated_text = response['TranslatedText']
        with open(translated_text_file, 'w') as f:
            f.write(translated_text)
        print("Translation completed.")
        return translated_text
    except Exception as e:
        print(f"Error during translation: {str(e)}")
        return None

def synthesize_speech(text):
    try:
        response = polly_client.synthesize_speech(
            Text=text,
            OutputFormat='mp3',
            VoiceId='Hans'
        )
        with open(final_output_mp3, 'wb') as f:
            f.write(response['AudioStream'].read())
        print("Speech synthesis completed.")
    except Exception as e:
        print(f"Error during speech synthesis: {str(e)}")

def main():
    # Step 1: Upload the MP3 file to S3
    file_uri = upload_to_s3(input_file, bucket_name)
    if not file_uri:
        return
    
    # Step 2: Transcribe the audio to text
    transcript_text = transcribe_audio(file_uri)
    if not transcript_text:
        return
    
    # Step 3: Translate the transcribed text
    translated_text = translate_text(transcript_text)
    if not translated_text:
        return
    
    # Step 4: Synthesize the translated text to speech
    synthesize_speech(translated_text)

if __name__ == "__main__":
    main()
