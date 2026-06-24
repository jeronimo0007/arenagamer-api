package com.arenagamer.api;

import com.arenagamer.api.config.DotEnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class ArenaGamerApplication {

    public static void main(String[] args) {
        DotEnvLoader.loadIfPresent();
        SpringApplication.run(ArenaGamerApplication.class, args);
    }
}
