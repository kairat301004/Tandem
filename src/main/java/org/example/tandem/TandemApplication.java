package org.example.tandem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class TandemApplication {

    public static void main(String[] args) {
        SpringApplication.run(TandemApplication.class, args);
    }

}
