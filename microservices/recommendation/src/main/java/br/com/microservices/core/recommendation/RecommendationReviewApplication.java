package br.com.microservices.core.recommendation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(value = {"se.magnus", "br.com.microservices.core.recommendation"})
public class RecommendationReviewApplication {

	private static final Logger LOG = LoggerFactory.getLogger(RecommendationReviewApplication.class);

	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = SpringApplication.run(RecommendationReviewApplication.class, args);
		String mongoDbHost = ctx.getEnvironment().getProperty("spring.data.mongodb.host");
		String mongoDbPort = ctx.getEnvironment().getProperty("spring.data.mongodb.port");

		LOG.info("Connected to MongoDb: " + mongoDbHost + ":" + mongoDbPort);
	}

}
