package com.looksee.pageBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;


@SpringBootApplication(exclude = {
    // Exclude LookseeCoreAutoConfiguration to prevent circular import issue
    com.looksee.LookseeCoreAutoConfiguration.class
})
@ComponentScan(basePackages = {"com.looksee.*"})
@PropertySources({
	@PropertySource("classpath:application.properties")
})
@EnableNeo4jRepositories(basePackages = {
    "com.looksee.models.repository"
})
@EntityScan(basePackages = {
    "com.looksee.models",
	"com.looksee.gcp"
})
public class Application {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	public static void main(String[] args)  {
		System.setProperty("webdriver.http.factory", "jdk-http-client");
		SpringApplication.run(Application.class, args);
	}
}
