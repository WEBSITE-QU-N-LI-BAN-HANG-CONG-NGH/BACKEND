package com.webanhang.team_project.security;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {
    @GetMapping("/showMyLoggingPage")
    public String showMyLoggingPage() {
        return "fancy-login";
    }
}
