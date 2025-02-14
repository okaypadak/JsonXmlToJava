package XmlToJAXB.controller;

import XmlToJAXB.component.XmlToJava;
import XmlToJAXB.exception.XmlProcessingException;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Controller
public class ConverterController {

    @Autowired
    private XmlToJava xmlToJava;

    @PostMapping("/convert")
    public ResponseEntity<Resource> convertFile(@RequestParam("dosya") MultipartFile file, Model model) {
        if (file.isEmpty()) {
            model.addAttribute("errorMessage", "Lütfen bir dosya yükleyin.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            // Dosyayı geçici bir yere kaydet
            File tempFile = new File(System.getProperty("java.io.tmpdir"), file.getOriginalFilename());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(file.getBytes());
            }

            String outputDir = System.getProperty("java.io.tmpdir");
            xmlToJava.convert(tempFile, outputDir);


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
        } catch (IOException e) {
            model.addAttribute("errorMessage", "Dosya işlenirken hata oluştu.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
