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

import XmlToJAXB.component.JavaFormatService;
import XmlToJAXB.exception.ProcessingException;
import XmlToJAXB.service.Handler;

@Controller
public class ConverterController {

    @Autowired
    Handler handler;

    @Autowired
    private JavaFormatService javaFormatService;

    @PostMapping("/convert")
    public ResponseEntity<Resource> convertFile(@RequestParam("dosya") MultipartFile file, Model model) {
        System.out.println("POST request received for file: " + file.getOriginalFilename());
    
        if (file.isEmpty()) {
            model.addAttribute("errorMessage", "Lütfen bir dosya yükleyin.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    
        try {
            File tempFile = new File(System.getProperty("java.io.tmpdir"), file.getOriginalFilename());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(file.getBytes());
            }
    
            String outputDir = System.getProperty("user.home") + "/tempfiles";
    
            handler.get(tempFile.getName()).convert(tempFile, outputDir);
    
            javaFormatService.formatAndSaveJavaFile(tempFile.getName(), outputDir);
    
            String originalFileName = file.getOriginalFilename();
            String outputFileName = toClassName(originalFileName.replace(".xml", ".java").replace(".json", ".java"));
            File outputFile = new File(outputDir, outputFileName);
    
            if (!outputFile.exists()) {
                System.err.println("ERROR: Output file not created: " + outputFile.getAbsolutePath());
                model.addAttribute("errorMessage", "Dosya oluşturulamadı.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
    
            System.out.println("Output file successfully created: " + outputFile.getAbsolutePath());
    
            Resource resource = new FileSystemResource(outputFile);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + outputFileName);
            headers.add("X-File-Name", outputFileName);
    
            return ResponseEntity.ok().headers(headers).body(resource);
    
        } catch (ProcessingException e) {
            System.err.println("ProcessingException occurred: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IOException e) {
            System.err.println("IOException occurred: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "Dosya işlenirken hata oluştu.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {  // Bu, yakalanmayan tüm hataları kapsar
            System.err.println("Unexpected Exception: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "Bilinmeyen bir hata oluştu.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private static String toClassName(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

}
