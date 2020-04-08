package br.com.microservices.eurekaserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EurekaServerApplicationTests {

	@Test
	public void contextLoads() {
	}

	@Value("${app.eureka-username}")
	private String username;

	@Value("${app.eureka-password}")
	private String password;

	@Autowired
	public void setTestRestTemplate(TestRestTemplate testRestTemplate) {
		this.testRestTemplate = testRestTemplate.withBasicAuth(username, password);
	}

	private TestRestTemplate testRestTemplate;

	@Test
	public void catalogLoads() {

		String expectedReponseBody = "{\"applications\":{\"versions__delta\":\"1\",\"apps__hashcode\":\"\",\"application\":[]}}";
		ResponseEntity<String> entity = testRestTemplate.getForEntity("/eureka/apps", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals(expectedReponseBody, entity.getBody());
	}

	@Test
	public void healthy() {
		String expectedReponseBody = "{\"status\":\"UP\"}";
		ResponseEntity<String> entity = testRestTemplate.getForEntity("/actuator/health", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals(expectedReponseBody, entity.getBody());
	}

}
