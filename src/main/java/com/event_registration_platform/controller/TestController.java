package com.event_registration_platform.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/hello")
    public String hello(Authentication authentication) {
        return "Hello, " + authentication.getName();
    }

    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminOnly() {
        return "You are an ADMIN, access granted!";
    }
}