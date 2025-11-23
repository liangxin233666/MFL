package io.github.liangxin233666.mfl;

import io.github.liangxin233666.mfl.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableCaching
@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
@EnableAsync

public class MflApplication {

	public static void main(String[] args) {
		SpringApplication.run(MflApplication.class, args);
	}

}
