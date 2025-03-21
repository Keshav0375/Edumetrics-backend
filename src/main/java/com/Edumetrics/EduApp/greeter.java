package com.Edumetrics.EduApp;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class greeter {

    @RequestMapping("/")
    public String sayHello() {
        return "Hello world!, Welcome to Edumetrics";
    }
}
