package org.aws.gcrrp.datatos3.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.time.LocalDateTime;

@Controller
class HomeController {

    @GetMapping("/")
    String flagger(Model model) {
        model.addAttribute("now", LocalDateTime.now());
        return "index";
    }

    @GetMapping("/tab")
    String tab(Model model) {
        model.addAttribute("now", LocalDateTime.now());
        return "index";
    }

}
