package com.franquicias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada de la aplicación (equivale al manage.py de Django). */
@SpringBootApplication
public class FranquiciasApplication {

    public static void main(String[] args) {
        SpringApplication.run(FranquiciasApplication.class, args);
    }
}
