package XmlXsdToJAXB4SOAP.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import XmlXsdToJAXB4SOAP.service.XSDToJavaService;
import XmlXsdToJAXB4SOAP.service.XmlToXsdService;
import XmlXsdToJAXB4SOAP.service.ZipService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;

@RestController
public class XSDController {

    @Autowired
    XSDToJavaService xsdToJavaService;

    @Autowired
    ZipService zipService;

    @Autowired
    XmlToXsdService xmlToXsdService;

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom RANDOM = new SecureRandom();


    @PostMapping("/convert")
    public ResponseEntity<InputStreamResource> convertXSD(@RequestParam("dosya") List<MultipartFile> dosyalar) throws IOException {

        String outputDir = "C:\\generated";
        String randomDir = generateRandomString(10);
        String fullOutputDir = outputDir + File.separator + randomDir;


        for (MultipartFile dosya : dosyalar) {
            String fileName = dosya.getOriginalFilename();

            if (fileName == null) {
                return ResponseEntity.badRequest()
                        .body(null);
            }

            if (fileName.toLowerCase().endsWith(".xml")) {

                File tempDir = new File(fullOutputDir + "\\xml");

                if (!tempDir.exists()) {
                    tempDir.mkdirs();
                }

                File tempFile = File.createTempFile("schema", ".xml", tempDir);
                dosya.transferTo(tempFile);

                String xsdDosya = xmlToXsdService.convert(tempFile);
                xsdToJavaService.convert(new File(xsdDosya), fullOutputDir);

            } else if (fileName.toLowerCase().endsWith(".xsd")) {

                File tempDir = new File(fullOutputDir + "\\xsd");

                if (!tempDir.exists()) {
                    tempDir.mkdirs();
                }

                File tempFile = File.createTempFile("schema", ".xsd", tempDir);
                dosya.transferTo(tempFile);


                xsdToJavaService.convert(tempFile, fullOutputDir);

            } else {
                return ResponseEntity.badRequest()
                        .body(null);
            }
        }

        String zipFilePath = fullOutputDir + ".zip";
        zipService.zipDirectory(fullOutputDir, zipFilePath);

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