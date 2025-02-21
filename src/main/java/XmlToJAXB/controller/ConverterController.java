package XmlToJAXB.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.JsonParseException;
import com.google.googlejavaformat.java.FormatterException;

import XmlToJAXB.component.JavaFormatService;
import XmlToJAXB.component.JsonToJava;
import XmlToJAXB.component.XmlToJava;
import XmlToJAXB.exception.XmlProcessingException;

@Controller
public class ConverterController {

    @Autowired
    private XmlToJava xmlToJava;

    @Autowired
    private JsonToJava jsonToJava;

    @Autowired
    private JavaFormatService javaFormatService;

    @PostMapping("/convert")
    public ResponseEntity<Resource> convertFile(@RequestParam("dosya") MultipartFile file, Model model) throws FormatterException {
        if (file.isEmpty()) {
            model.addAttribute("errorMessage", "Lütfen bir dosya yükleyin.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {

            File tempFile = new File(System.getProperty("java.io.tmpdir"), file.getOriginalFilename());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(file.getBytes());
            }

            String fileExtension = getFileExtension(file.getOriginalFilename());
            String outputDir = System.getProperty("java.io.tmpdir");


            if (fileExtension.equalsIgnoreCase("xml")) {
                xmlToJava.convert(tempFile, outputDir);
            } else if (fileExtension.equalsIgnoreCase("json")) {
                jsonToJava.convert(tempFile, outputDir);
            } else {
                model.addAttribute("errorMessage", "Desteklenmeyen dosya formatı.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            javaFormatService.formatAndSaveJavaFile(outputDir, tempFile.getName());


            String originalFileName = file.getOriginalFilename();
            String outputFileName = originalFileName.replace(".xml", ".java").replace(".json", ".java");
            File outputFile = new File(outputDir, outputFileName);

            if (!outputFile.exists()) {
                model.addAttribute("errorMessage", "Dosya oluşturulamadı.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            Resource resource = new FileSystemResource(outputFile);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + outputFileName);
            headers.add("X-File-Name", outputFileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (XmlProcessingException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        catch (JsonParseException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        catch (IOException e) {
            model.addAttribute("errorMessage", "Dosya işlenirken hata oluştu.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1) {
            return "";
        }
        return filename.substring(dotIndex + 1);
    }

}
