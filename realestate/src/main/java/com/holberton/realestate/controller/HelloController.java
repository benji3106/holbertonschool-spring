package com.holberton.realestate.controller;

import com.holberton.realestate.dto.HelloResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/api/hello")
    public HelloResponse hello(@RequestParam(name = "name", defaultValue = "World") String name) {
        return new HelloResponse("Hello, " + name + "!");
    }
}