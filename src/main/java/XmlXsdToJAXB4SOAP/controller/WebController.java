package XmlXsdToJAXB4SOAP.controller;

import XmlXsdToJAXB4SOAP.component.*;
import XmlXsdToJAXB4SOAP.service.XSDService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
public class WebController {

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

    @Autowired
    private XSDService xsdService;

    @GetMapping("/")
    public String showForm() {
        return "index";
    }

}