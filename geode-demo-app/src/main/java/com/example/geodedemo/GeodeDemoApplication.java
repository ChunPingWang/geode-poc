package com.example.geodedemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.gemfire.config.annotation.EnableEntityDefinedRegions;
import org.springframework.data.gemfire.config.annotation.EnablePdx;
import org.springframework.data.gemfire.repository.config.EnableGemfireRepositories;

@SpringBootApplication
@EnablePdx
@EnableEntityDefinedRegions(basePackages = "com.example.geodedemo.entity")
@EnableGemfireRepositories(basePackages = "com.example.geodedemo.repository")
public class GeodeDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeodeDemoApplication.class, args);
    }
}
