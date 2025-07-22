import boto3
import time
import os
from botocore.exceptions import ClientError
import json

class AudioTransformer:
    def __init__(self):
        self.s3_client = boto3.client('s3')
        self.transcribe_client = boto3.client('transcribe')
        self.translate_client = boto3.client('translate')
        self.polly_client = boto3.client('polly')
        self.bucket_name = 'hello-12345'
        self.region = 'us-east-1'

    def upload_to_s3(self, file_path, s3_key):
        try:
            self.s3_client.upload_file(file_path, self.bucket_name, s3_key)
            return f's3://{self.bucket_name}/{s3_key}'
        except ClientError as e:
            print(f"Error uploading to S3: {str(e)}")
            raise

    def wait_for_transcription(self, job_name):
        while True:
            status = self.transcribe_client.get_transcription_job(
                TranscriptionJobName=job_name
            )
            if status['TranscriptionJob']['TranscriptionJobStatus'] in ['COMPLETED', 'FAILED']:
                return status
            time.sleep(5)

    def transcribe_audio(self, s3_uri):
        try:
            job_name = f'transcribe-job-{int(time.time())}'
            self.transcribe_client.start_transcription_job(
                TranscriptionJobName=job_name,
                Media={'MediaFileUri': s3_uri},
                MediaFormat='mp3',
                LanguageCode='en-US'
            )
            
            result = self.wait_for_transcription(job_name)
            
            if result['TranscriptionJob']['TranscriptionJobStatus'] == 'COMPLETED':
                transcript_uri = result['TranscriptionJob']['Transcript']['TranscriptFileUri']
                # Get the transcript content
                import urllib.request
                response = urllib.request.urlopen(transcript_uri)
                data = json.loads(response.read())
                return data['results']['transcripts'][0]['transcript']
            else:
                raise Exception("Transcription failed")
                
        except ClientError as e:
            print(f"Error in transcription: {str(e)}")
            raise

    def translate_text(self, text):
        try:
            response = self.translate_client.translate_text(
                Text=text,
                SourceLanguageCode='en',
                TargetLanguageCode='de'
            )
            return response['TranslatedText']
        except ClientError as e:
            print(f"Error in translation: {str(e)}")
            raise

    def synthesize_speech(self, text):
        try:
            response = self.polly_client.synthesize_speech(
                Text=text,
                OutputFormat='mp3',
                VoiceId='Marlene',  # German female voice
                LanguageCode='de-DE'
            )
            
            # Save the audio stream to a file
            if "AudioStream" in response:
                with open('output.mp3', 'wb') as file:
                    file.write(response['AudioStream'].read())
                return 'output.mp3'
        except ClientError as e:
            print(f"Error in speech synthesis: {str(e)}")
            raise

    def transform_audio(self, input_file):
        try:
            # Upload to S3
            s3_key = f'input/{os.path.basename(input_file)}'
            s3_uri = self.upload_to_s3(input_file, s3_key)
            
            # Transcribe
            print("Transcribing audio...")
            transcript = self.transcribe_audio(s3_uri)
            print("Transcription completed")
            
            # Translate
            print("Translating text...")
            translated_text = self.translate_text(transcript)
            print("Translation completed")
            
            # Synthesize
            print("Synthesizing speech...")
            output_file = self.synthesize_speech(translated_text)
            print(f"Audio synthesis completed. Output saved as: {output_file}")
            
            # Clean up S3
            self.s3_client.delete_object(Bucket=self.bucket_name, Key=s3_key)
            
            return output_file
            
        except Exception as e:
            print(f"Error in transformation pipeline: {str(e)}")
            raise

def main():
    try:
        transformer = AudioTransformer()
        input_file = 'input.mp3'
        
        # Validate input file
        if not os.path.exists(input_file):
            raise FileNotFoundError("Input file not found")
        
        output_file = transformer.transform_audio(input_file)
        print(f"Transformation completed successfully. Output file: {output_file}")
        
    except Exception as e:
        print(f"Error: {str(e)}")

if __name__ == "__main__":
    main()