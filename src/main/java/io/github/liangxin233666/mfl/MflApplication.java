package io.github.liangxin233666.mfl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class MflApplication {

	public static void main(String[] args) {
		SpringApplication.run(MflApplication.class, args);
	}

}
