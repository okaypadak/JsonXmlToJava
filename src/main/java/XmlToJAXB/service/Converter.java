package XmlToJAXB.service;

import XmlToJAXB.component.JsonToJava;
import XmlToJAXB.component.XmlToJava;
import XmlToJAXB.component.Zip;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;

@Service
public class Converter {

    @Autowired
    private XmlToJava xmlToJava;

    @Autowired
    private JsonToJava jsonToJava;

    @Autowired
    private Zip zip;


    @Value("${local-path}")
    private String local_path;

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom RANDOM = new SecureRandom();

    public ResponseEntity<InputStreamResource> convert(List<MultipartFile> files) throws Exception {
        String outputDir = local_path;
        String randomDir = generateRandomString(10);
        String fullOutputDir = outputDir + File.separator + randomDir;

        File outputFolder = new File(fullOutputDir);

        if (outputFolder.exists()) {
            deleteDirectory(outputFolder);
        }

        outputFolder.mkdirs();

        if (files.size() == 1) {
            MultipartFile file = files.get(0);
            processFile(file, fullOutputDir);

            return createOneResponse(fullOutputDir, file);
        } else {
            for (MultipartFile file : files) {
                processFile(file, fullOutputDir);
            }

            return createZipResponse(fullOutputDir);
        }
    }

    private void processFile(MultipartFile file, String fullOutputDir) throws Exception {
        File tempDir = new File(fullOutputDir);

        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        File tempFile = new File(tempDir, Objects.requireNonNull(file.getOriginalFilename()));
        file.transferTo(tempFile);

        String fileName = file.getOriginalFilename().toLowerCase();

        if (fileName.endsWith(".xml")) {
            xmlToJava.convert(tempFile, fullOutputDir);
        } else if (fileName.endsWith(".json")) {
            jsonToJava.convert(tempFile, fullOutputDir);
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + fileName);
        }
    }


    private ResponseEntity<InputStreamResource> createOneResponse(String fullOutputDir, MultipartFile file) throws IOException {
        String fileName = Objects.requireNonNull(file.getOriginalFilename()).replaceAll("\\.(xml|json)$", ".java");

        String filePath = fullOutputDir + File.separator + fileName;
        File newFile = new File(filePath);

        if (!newFile.exists()) {
            throw new IOException("Java file not created: " + filePath);
        }

        InputStreamResource resource = new InputStreamResource(new FileInputStream(newFile));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(newFile.length()));

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }


    private ResponseEntity<InputStreamResource> createZipResponse(String fullOutputDir) throws IOException {
        String zipFilePath = fullOutputDir + ".zip";
        zip.zipDirectory(fullOutputDir, zipFilePath);

        File zipFile = new File(zipFilePath);
        InputStreamResource resource = new InputStreamResource(new FileInputStream(zipFile));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFile.getName() + "\"");
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(zipFile.length()));

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    private String generateRandomString(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }

    private void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
    }
}
