package XmlXsdToJAXB4SOAP.service;

import XmlXsdToJAXB4SOAP.component.*;
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

@Service
public class XSDService {

    @Autowired
    private XSDToJava xsdToJava;

    @Autowired
    private Zip zip;

    @Autowired
    private XmlToXsd xmlToXsd;

    @Autowired
    private JavaFileUpdater javaFileUpdater;

    @Autowired
    private CommentRemover commentRemover;

    @Value("${local-path}")
    private String local_path;

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom RANDOM = new SecureRandom();

    public ResponseEntity<InputStreamResource> convertXSD(List<MultipartFile> dosyalar) throws IOException {
        String outputDir = local_path;
        String randomDir = generateRandomString(10);
        String fullOutputDir = outputDir + File.separator + randomDir;

        for (MultipartFile dosya : dosyalar) {
            String fileName = dosya.getOriginalFilename();

            if (fileName == null) {
                return ResponseEntity.badRequest().body(null);
            }

            if (fileName.toLowerCase().endsWith(".xml")) {
                processXmlFile(dosya, fullOutputDir);
            } else if (fileName.toLowerCase().endsWith(".xsd")) {
                processXsdFile(dosya, fullOutputDir);
            } else {
                return ResponseEntity.badRequest().body(null);
            }
        }

        return createZipResponse(fullOutputDir);
    }

    private void processXmlFile(MultipartFile dosya, String fullOutputDir) throws IOException {
        File tempDir = new File(fullOutputDir + "\\xml");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        File tempFile = File.createTempFile("schema", ".xml", tempDir);
        dosya.transferTo(tempFile);

        String xsdDosya = xmlToXsd.convert(tempFile);
        List<String> generatedFiles = xsdToJava.convert(new File(xsdDosya), fullOutputDir);

        generatedFiles.forEach(tek -> {
            if(!tek.contains("Object")) {
                try {
                    javaFileUpdater.updateJavaFile(xsdDosya, fullOutputDir +"\\"+ tek);
                    commentRemover.removeCommentsAndEmptyLines(fullOutputDir +"\\"+ tek);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });


    }

    private void processXsdFile(MultipartFile dosya, String fullOutputDir) throws IOException {
        File tempDir = new File(fullOutputDir + "\\xsd");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        File tempFile = File.createTempFile("schema", ".xsd", tempDir);
        dosya.transferTo(tempFile);

        xsdToJava.convert(tempFile, fullOutputDir);
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
}
