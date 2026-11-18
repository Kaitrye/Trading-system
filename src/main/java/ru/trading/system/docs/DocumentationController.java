package ru.trading.system.docs;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DocumentationController {

  @GetMapping("/docs")
  String docs() {
    return "redirect:/swagger-ui.html";
  }
}
