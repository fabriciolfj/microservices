package br.com.microservices.core.recommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(value = {"se.magnus", "br.com.microservices.core.recommendation"})
public class RecommendationReviewApplication {

	public static void main(String[] args) {
		SpringApplication.run(RecommendationReviewApplication.class, args);
	}

}
