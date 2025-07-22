import boto3
import time
import os
import json
from botocore.exceptions import ClientError
import math

class MediaPipelineProcessor:
    def __init__(self):
        self.s3_bucket = 'hello-12345'
        self.region = 'us-east-1'
        self.role_arn = f'arn:aws:iam::{717556240325}:role/mediaconvert_trust'
        
        # Initialize AWS clients
        self.s3_client = boto3.client('s3', region_name=self.region)
        
        # MediaConvert endpoint needs to be retrieved
        mediaconvert = boto3.client('mediaconvert', region_name=self.region)
        endpoints = mediaconvert.describe_endpoints()
        self.mediaconvert_client = boto3.client('mediaconvert', 
                                              region_name=self.region,
                                              endpoint_url=endpoints['Endpoints'][0]['Url'])
        
        self.transcribe_client = boto3.client('transcribe', region_name=self.region)

    def upload_to_s3(self, file_path, s3_key):
        try:
            self.s3_client.upload_file(file_path, self.s3_bucket, s3_key)
            return f's3://{self.s3_bucket}/{s3_key}'
        except ClientError as e:
            print(f"Error uploading to S3: {str(e)}")
            raise

    def convert_video_to_audio(self, input_s3_uri, output_key):
        job_settings = {
            "Inputs": [{
                "AudioSelectors": {
                    "Audio Selector 1": {"DefaultSelection": "DEFAULT"}
                },
                "TimecodeSource": "ZEROBASED",
                "FileInput": input_s3_uri
            }],
            "OutputGroups": [{
                "Name": "File Group",
                "Outputs": [{
                    "ContainerSettings": {"Container": "RAW"},
                    "AudioDescriptions": [{
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
                    }]
                }],
                "OutputGroupSettings": {
                    "Type": "FILE_GROUP_SETTINGS",
                    "FileGroupSettings": {
                        "Destination": f"s3://{self.s3_bucket}/{output_key}"
                    }
                }
            }]
        }

        try:
            job = self.mediaconvert_client.create_job(
                Role=self.role_arn,
                Settings=job_settings
            )
            
            job_id = job['Job']['Id']
            while True:
                status = self.mediaconvert_client.get_job(Id=job_id)['Job']['Status']
                if status in ['COMPLETE', 'ERROR', 'CANCELED']:
                    break
                time.sleep(5)
                
            if status != 'COMPLETE':
                raise Exception(f"MediaConvert job failed with status: {status}")
                
            return f"s3://{self.s3_bucket}/{output_key}.mp3"
            
        except ClientError as e:
            print(f"Error in MediaConvert: {str(e)}")
            raise

    def transcribe_audio(self, audio_uri):
        job_name = f"transcription-{int(time.time())}"
        try:
            self.transcribe_client.start_transcription_job(
                TranscriptionJobName=job_name,
                Media={'MediaFileUri': audio_uri},
                MediaFormat='mp3',
                LanguageCode='en-US'
            )

            while True:
                status = self.transcribe_client.get_transcription_job(
                    TranscriptionJobName=job_name
                )
                if status['TranscriptionJob']['TranscriptionJobStatus'] in ['COMPLETED', 'FAILED']:
                    break
                time.sleep(5)

            if status['TranscriptionJob']['TranscriptionJobStatus'] == 'COMPLETED':
                transcript_uri = status['TranscriptionJob']['Transcript']['TranscriptFileUri']
                
                # Get the transcript text
                import urllib.request
                response = urllib.request.urlopen(transcript_uri)
                data = json.loads(response.read())
                return data['results']['transcripts'][0]['transcript']
            else:
                raise Exception("Transcription job failed")

        except ClientError as e:
            print(f"Error in Transcribe: {str(e)}")
            raise

    def process_file(self, input_file_path):
        try:
            # Upload input file to S3
            input_key = os.path.basename(input_file_path)
            input_s3_uri = self.upload_to_s3(input_file_path, input_key)

            # Convert video to audio
            audio_output_key = f"audio-{int(time.time())}"
            audio_s3_uri = self.convert_video_to_audio(input_s3_uri, audio_output_key)

            # Transcribe audio to text
            transcript_text = self.transcribe_audio(audio_s3_uri)

            # Save transcript to local file
            output_file_path = 'output.txt'
            with open(output_file_path, 'w', encoding='utf-8') as f:
                f.write(transcript_text)

            print(f"Transcription saved to {output_file_path}")
            return transcript_text

        except Exception as e:
            print(f"Error in pipeline processing: {str(e)}")
            raise
        finally:
            # Cleanup S3 files
            try:
                self.s3_client.delete_object(Bucket=self.s3_bucket, Key=input_key)
                self.s3_client.delete_object(Bucket=self.s3_bucket, Key=f"{audio_output_key}.mp3")
            except:
                pass

def main():
    processor = MediaPipelineProcessor()
    try:
        result = processor.process_file('input.mp4')
        print("Processing completed successfully!")
    except Exception as e:
        print(f"Pipeline failed: {str(e)}")

if __name__ == "__main__":
    main()