package com.looksee.pageBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;


@SpringBootApplication( scanBasePackages = {
	"com.looksee.pageBuilder"
})
@PropertySources({
	@PropertySource("classpath:application.properties")
})
public class Application {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	public static void main(String[] args)  {
		System.setProperty("webdriver.http.factory", "jdk-http-client");
		SpringApplication.run(Application.class, args);
	}
}
