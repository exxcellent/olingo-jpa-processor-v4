package org.apache.olingo.jpa.servlet.springboot.bean.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@ServletComponentScan // trigger auto detection of out servlet
@SpringBootApplication
public class SpringBootApp {

  public static void main(final String[] args) {
    SpringApplication.run(SpringBootApp.class, args);
  }
}
