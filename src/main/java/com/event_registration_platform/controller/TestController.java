package com.event_registration_platform.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;

public class TestController {

    @GetMapping("/hello")
    public String hello(Authentication authentication){
        return "Hello"+ authentication.getName();
    }
}
