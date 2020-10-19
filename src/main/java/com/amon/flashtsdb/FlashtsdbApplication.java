package com.amon.flashtsdb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.oas.annotations.EnableOpenApi;

/**
 * @author amon
 */
@SpringBootApplication
@EnableOpenApi
public class FlashtsdbApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashtsdbApplication.class, args);
    }

}
