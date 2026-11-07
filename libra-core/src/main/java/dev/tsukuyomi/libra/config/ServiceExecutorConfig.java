package dev.tsukuyomi.libra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ServiceExecutorConfig {

    @Bean(destroyMethod = "close")
    public ExecutorService libraServiceExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

}
