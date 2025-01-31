package XmlXsdToJAXB4SOAP.controller;

import XmlXsdToJAXB4SOAP.component.*;
import XmlXsdToJAXB4SOAP.service.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @Autowired
    Zip zip;

    @Autowired
    private Converter converter;

    @GetMapping("/")
    public String showForm() {
        return "index";
    }

}