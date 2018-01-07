package io.foben.k8s.configoperator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class ConfigMapOperatorApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigMapOperatorApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ConfigMapOperatorApplication.class, args);
        LOGGER.info("ConfigMap Operator up and running");
    }
}
