package XmlToJAXB.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String showForm(Model model) {
        model.addAttribute("successMessage", null);
        model.addAttribute("errorMessage", null);
        return "index";
    }
}
