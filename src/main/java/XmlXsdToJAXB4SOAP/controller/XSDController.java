package XmlXsdToJAXB4SOAP.controller;

import XmlXsdToJAXB4SOAP.component.*;
import XmlXsdToJAXB4SOAP.service.XSDService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;

@RestController
public class XSDController {

    @Autowired
    XSDToJava xsdToJava;

    @Autowired
    Zip zip;

    @Autowired
    XmlToXsd xmlToXsd;

    @Autowired
    JavaFileUpdater javaFileUpdater;

    @Autowired
    CommentRemover commentRemover;

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom RANDOM = new SecureRandom();


    @Autowired
    private XSDService xsdService;

    @PostMapping("/convert")
    public ResponseEntity<InputStreamResource> convertXSD(@RequestParam("dosya") List<MultipartFile> dosyalar) throws IOException {
        return xsdService.convertXSD(dosyalar);
    }
}