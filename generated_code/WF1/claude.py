import boto3
import os
from botocore.exceptions import ClientError
from typing import List
import time

class PDFToMP3Converter:
    def __init__(self):
        # Initialize AWS clients
        self.textract = boto3.client('textract', region_name='us-east-1')
        self.polly = boto3.client('polly', region_name='us-east-1')
        self.s3 = boto3.client('s3', region_name='us-east-1')
        self.bucket_name = 'hello-12345'
        
    def read_pdf(self, file_path: str) -> bytes:
        """Read PDF file and return bytes"""
        try:
            with open(file_path, 'rb') as file:
                return file.read()
        except Exception as e:
            raise Exception(f"Error reading PDF file: {str(e)}")

    def extract_text_from_pdf(self, pdf_bytes: bytes) -> str:
        """Extract text from PDF using AWS Textract"""
        try:
            # Start async document analysis
            response = self.textract.start_document_text_detection(
                DocumentLocation={
                    'S3Object': {
                        'Bucket': self.bucket_name,
                        'Name': 'input.pdf'
                    }
                }
            )
            
            job_id = response['JobId']
            
            # Wait for the job to complete
            while True:
                response = self.textract.get_document_text_detection(JobId=job_id)
                status = response['JobStatus']
                if status in ['SUCCEEDED', 'FAILED']:
                    break
                time.sleep(5)
            
            if status == 'FAILED':
                raise Exception("Textract job failed")
            
            # Extract text from all blocks
            text = ""
            for item in response['Blocks']:
                if item['BlockType'] == 'LINE':
                    text += item['Text'] + " "
            
            return text.strip()
            
        except Exception as e:
            raise Exception(f"Error in Textract text extraction: {str(e)}")

    def convert_text_to_speech(self, text: str) -> bytes:
        """Convert text to speech using AWS Polly"""
        try:
            # Maximum text length for Polly is 3000 characters
            MAX_CHUNK_SIZE = 3000
            audio_chunks = []
            
            # Split text into chunks if needed
            if len(text) > MAX_CHUNK_SIZE:
                words = text.split()
                current_chunk = ""
                
                for word in words:
                    if len(current_chunk) + len(word) + 1 <= MAX_CHUNK_SIZE:
                        current_chunk += word + " "
                    else:
                        # Process current chunk
                        response = self.polly.synthesize_speech(
                            Text=current_chunk.strip(),
                            OutputFormat='mp3',
                            VoiceId='Joanna'
                        )
                        audio_chunks.append(response['AudioStream'].read())
                        current_chunk = word + " "
                
                # Process last chunk
                if current_chunk:
                    response = self.polly.synthesize_speech(
                        Text=current_chunk.strip(),
                        OutputFormat='mp3',
                        VoiceId='Joanna'
                    )
                    audio_chunks.append(response['AudioStream'].read())
            else:
                # Process entire text at once
                response = self.polly.synthesize_speech(
                    Text=text,
                    OutputFormat='mp3',
                    VoiceId='Joanna'
                )
                audio_chunks.append(response['AudioStream'].read())
            
            return b''.join(audio_chunks)
            
        except Exception as e:
            raise Exception(f"Error in Polly speech synthesis: {str(e)}")

    def save_audio(self, audio_data: bytes, output_path: str):
        """Save audio data to file"""
        try:
            with open(output_path, 'wb') as file:
                file.write(audio_data)
        except Exception as e:
            raise Exception(f"Error saving audio file: {str(e)}")

    def convert(self, input_path: str, output_path: str):
        """Main conversion process"""
        try:
            # Upload PDF to S3 (required for Textract)
            pdf_data = self.read_pdf(input_path)
            self.s3.put_object(Bucket=self.bucket_name, Key='input.pdf', Body=pdf_data)
            
            # Extract text from PDF
            text = self.extract_text_from_pdf(pdf_data)
            
            if not text:
                raise Exception("No text was extracted from the PDF")
            
            # Convert text to speech
            audio_data = self.convert_text_to_speech(text)
            
            # Save the audio file
            self.save_audio(audio_data, output_path)
            
            # Cleanup: Remove temporary S3 file
            self.s3.delete_object(Bucket=self.bucket_name, Key='input.pdf')
            
        except Exception as e:
            raise Exception(f"Conversion failed: {str(e)}")

def main():
    try:
        converter = PDFToMP3Converter()
        converter.convert('input.pdf', 'output.mp3')
        print("Conversion completed successfully!")
    except Exception as e:
        print(f"Error: {str(e)}")

if __name__ == "__main__":
    main()