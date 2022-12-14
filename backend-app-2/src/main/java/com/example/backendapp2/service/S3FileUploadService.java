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

package com.example.backendapp2.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Objects;

@Service
public class S3FileUploadService {

    private static Logger logger = LoggerFactory.getLogger(S3FileUploadService.class);
    public static final String CC_DOCUMENT_BUCKET = "cc-documents-bucket";
    public static final String CC_AUDIO_BUCKET = "cc-audio-bucket";

    @Autowired
    private AmazonS3 s3Client;

    public String uploadFile(MultipartFile file) {
        File fileObj = convertMultiPartFileToFile(file);
        String fileName = file.getOriginalFilename();
        s3Client.putObject(new PutObjectRequest(CC_DOCUMENT_BUCKET, fileName, fileObj));
        return fileName;
    }

    private File convertMultiPartFileToFile(MultipartFile file) {
        File convertedFile = new File(Objects.requireNonNull(file.getOriginalFilename()));
        try (FileOutputStream fos = new FileOutputStream(convertedFile)) {
            fos.write(file.getBytes());
        } catch (IOException e) {
            logger.error("Exception while converting multipart file to file {}", e.getMessage());
        }
        return convertedFile;
    }

    public boolean search(String filename) {
        S3Objects s3Objects = S3Objects.inBucket(s3Client, CC_AUDIO_BUCKET);
        for (S3ObjectSummary object : s3Objects) {
            if (Objects.equals(object.getKey(), filename))
                return true;
        }
        return false;
    }

    public void downloadFile(String fileName) throws IOException {
        InputStream s3ObjectInputStream = s3Client.getObject(CC_AUDIO_BUCKET, fileName).getObjectContent();
        File file = new File(fileName.substring(5));
        int read = -1;
        try(InputStream reader = new BufferedInputStream(s3ObjectInputStream);
            OutputStream writer = new BufferedOutputStream(new FileOutputStream(file))) {
            while ((read = reader.read()) != -1) {
                writer.write(read);
            }
            writer.flush();
        } catch (Exception e) {
            logger.error("Exception while downloading file from S3 bucket {}", e.getMessage());
        }
        logger.info("File downloaded from S3 bucket {}", fileName);
    }
}
