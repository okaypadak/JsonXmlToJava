package XmlXsdToJAXB4SOAP.controller;

import XmlXsdToJAXB4SOAP.component.*;
import XmlXsdToJAXB4SOAP.service.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
public class XSDController {

    @Autowired
    Zip zip;

    @Autowired
    private Converter converter;

    @PostMapping("/convert")
    public ResponseEntity<InputStreamResource> convertXSD(@RequestParam("dosya") List<MultipartFile> dosyalar) throws Exception {
        return converter.convertXSD(dosyalar);
    }
}