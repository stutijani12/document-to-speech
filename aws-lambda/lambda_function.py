/*
 * Copyright 2022 Ruchi Dhore
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

import json
import urllib.parse
import boto3
from botocore.exceptions import BotoCoreError, ClientError
from datetime import datetime

CONFIDENCE_LIMIT = 80
S3_AUDIO_BUCKET = 'cc-audio-bucket'
S3_DOCUMENT_BUCKET = 'cc-documents-bucket'

s3 = boto3.client('s3')
s3Resource = boto3.resource('s3')
textract = boto3.client('textract')
translate = boto3.client('translate')
polly = boto3.client('polly')
dynamodb = boto3.client('dynamodb')

language_codes = dict({
    "en": "en-US",
    "hi": "hi-IN",
    "zh": "cmn-CN"
})
language_voice = dict({
    "eng": "Joanna",
    "hindi": "Kajal",
    "chinese": "Zhiyu"
})
language_map = dict({
    "en-US": "english",
    "hi-IN": "hindi",
    "cmn-CN": "chinese"
})

def lambda_handler(event, context):
    # Get the object from the event
    bucket = event['Records'][0]['s3']['bucket']['name']
    key = urllib.parse.unquote_plus(event['Records'][0]['s3']['object']['key'], encoding='utf-8')
    try:
        response = s3.get_object(Bucket=bucket, Key=key)
        print("CONTENT TYPE: " + response['ContentType'])
        
        extracted_text = extract_text(bucket, key)
        
        temp = key.split(".")
        audio_file_name = temp[0]
        
        hindi_translation = translate_text(extracted_text, "hi")
        chinese_translation = translate_text(extracted_text, "zh")
        
        eng_file_name = convert_text_to_audio(extracted_text, language_codes["en"], language_voice["eng"], audio_file_name)
        hindi_file_name = convert_text_to_audio(hindi_translation, language_codes["hi"], language_voice["hindi"], audio_file_name)
        chinese_file_name = convert_text_to_audio(chinese_translation, language_codes["zh"], language_voice["chinese"], audio_file_name)
        
        store_file_in_bucket(eng_file_name)
        store_file_in_bucket(hindi_file_name)
        store_file_in_bucket(chinese_file_name)
        
        store_log(key, eng_file_name)
        store_log(key, hindi_file_name)
        store_log(key, chinese_file_name)
        
    except Exception as e:
        print(e)
        print('Error getting object {} from bucket {}. Make sure they exist and your bucket is in the same region as this function.'.format(key, bucket))
        raise e
    
# Extract text from the file
def extract_text(bucket, key):
    textract_response = textract.detect_document_text(
    Document = {'S3Object': {'Bucket': bucket, 'Name': key}})
        
    # Get the text blocks from textract response
    extracted_lines = []
    blocks = textract_response['Blocks']
    for block in textract_response['Blocks']:
        if block['BlockType'] == 'LINE' and block['Confidence'] >= CONFIDENCE_LIMIT:
            extracted_lines.append(block['Text']) 
        
    # Create a single text input
    extracted_text = ""
    for t in extracted_lines:
        extracted_text += t
        
    return extracted_text
    
# Translate text from english to target language
def translate_text(input_text, target_language):
    hindi_translate_response = translate.translate_text(
        Text = input_text,
        SourceLanguageCode = "en",
        TargetLanguageCode = target_language)
            
    hindi_translation = hindi_translate_response.get("TranslatedText")
    return hindi_translation
    
# Request speech synthesis and store audio response from polly to audio file
def convert_text_to_audio(input_text, language_code, voice_id, audio_file_name):
    try:
        polly_response = polly.synthesize_speech(
            Text = input_text,
            LanguageCode = language_code,
            OutputFormat = "mp3",
            VoiceId = voice_id,
            Engine = "neural")
    except (BotoCoreError, ClientError) as error:
        print("Error while converting to audio: " + error)

    language_prefix = language_map[language_code]

    file_name = "/tmp/" + audio_file_name + "_" + language_prefix + ".mp3"
    print("Audio file name :: " + file_name)
    if "AudioStream" in polly_response:
        print("Audio stream present")
        file = open(file_name, 'wb')
        file.write(polly_response['AudioStream'].read())
        file.close()
    else:
        print("Error, no AudioStream present in response")
        return
        
    return file_name
    
# Store audio file in destination S3 bucket
def store_file_in_bucket(file_name):
    s3Resource.Bucket(S3_AUDIO_BUCKET).upload_file(file_name, file_name)

# Store log in dynamodb table
def store_log(key, audio_file_name):
    current_timestamp = datetime.now()
    timestamp = current_timestamp.strftime("%c")
    message = key + " converted to " + audio_file_name + "_audio.mp3"
    dynamodb.put_item(TableName='Logs', Item={'Timestamp': {'S':timestamp}, 'Message': {'S':message}})