package fr.achabitation.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AppPageController {
    @GetMapping({"/", "/app"})
    public String app() {
        return "redirect:/app/index.html";
    }
}
