package com.webanhang.team_project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DemoController {
    @GetMapping("/")
    public String showHome() {
        return "home";
    }

    @GetMapping("/seller")
    public String showLeaderPage() {
        return "seller-page";
    }

    @GetMapping("/admin")
    public String showAdminPage() {
        return "admin-page";
    }

    @GetMapping("/access-denied")
    public String showAccessDenied() {
        return "access-denied";
    }
}
