package org.example.demo20.frontend.controller;

import org.example.demo20.frontend.client.OrganizationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    private final OrganizationClient organizationClient;

    public HomeController(OrganizationClient organizationClient) {
        this.organizationClient = organizationClient;
    }

    @GetMapping("/")
    public String index(Model model) {
        log.info("GET /");
        model.addAttribute("organizations", organizationClient.findAll());
        return "index";
    }
}
