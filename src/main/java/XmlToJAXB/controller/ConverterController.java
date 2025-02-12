package XmlToJAXB.controller;

import XmlToJAXB.component.*;
import XmlToJAXB.service.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class ConverterController {

    @Autowired
    Zip zip;

    @Autowired
    private Converter converter;

    @PostMapping("/convert")
    public ResponseEntity<InputStreamResource> convertXSD(@RequestParam("dosya") List<MultipartFile> file) throws Exception {
        return converter.convert(file);
    }
}