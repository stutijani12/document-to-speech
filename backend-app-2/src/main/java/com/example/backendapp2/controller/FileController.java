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

package com.example.backendapp2.controller;

import com.example.backendapp2.service.S3FileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin("*")
@RestController
public class FileController {

    private static Logger logger = LoggerFactory.getLogger(FileController.class);
    private static final String MAP_KEY = "found";
    @Autowired
    private S3FileUploadService service;

    @PostMapping(value = "/file/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(@RequestParam MultipartFile file) throws IOException {
        Map<String, String> map = new HashMap<>();
        file.transferTo(getFilePath(file));
        uploadFileToS3(file);
        map.put("file_uploaded", "true");
        return map;
    }

    @GetMapping(value = "/file/find/")
    public Map<String, String> search(@RequestParam("filename") String filename) {
        Map<String, String> map = new HashMap<>();
        if (filename.length() > 1) {
            if (service.search("/tmp/" + filename)) {
                map.put(MAP_KEY, "audio file present");
            } else {
                map.put(MAP_KEY, "audio file not generated yet");
            }
        } else {
            map.put(MAP_KEY, "filename is empty");
        }
        return map;
    }

    @GetMapping("/file/download/")
    public ResponseEntity<Resource> download(@RequestParam("filename") String filename) {
        try {
            service.downloadFile("/tmp/" + filename);
        } catch (IOException e) {
            logger.error("Exception while downloading file {}", e.getMessage());
        }

        Path path = getAudioFilePath(filename);
        File file = new File(path.toString());
        if(file.exists()) {
            try {
                InputStream inputStream = new FileInputStream(file);
                InputStreamResource resource = new InputStreamResource(inputStream);
                long fileLength = file.length();
                return ResponseEntity.
                        ok().
                        contentLength(fileLength).
                        contentType(MediaType.MULTIPART_FORM_DATA).
                        body(resource);
            } catch (FileNotFoundException e) {
                logger.error("Exception file not found {}", e.getMessage());
            }
        } else {
            logger.info("File does not exist in S3 bucket yet");
        }
        return null;
    }

    private void uploadFileToS3(MultipartFile file) {
        service.uploadFile(file);
    }

    private Path getFilePath(MultipartFile file) {
        Path currentRelativePath = Paths.get("");
        String path = currentRelativePath.toAbsolutePath().toString();
        return Paths.get(path, file.getOriginalFilename());
    }

    private Path getAudioFilePath(String filename) {
        Path currentRelativePath = Paths.get("");
        String path = currentRelativePath.toAbsolutePath().toString();
        return Paths.get(path, filename);
    }
}
