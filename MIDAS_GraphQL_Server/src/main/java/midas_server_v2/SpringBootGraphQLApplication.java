package midas_server_v2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.logging.Level;
import java.util.logging.Logger;


@SpringBootApplication //equal to: @SpringBootConfiguration, @ComponentScan, @EnableAutoConfiguration
public class SpringBootGraphQLApplication {
	private static Logger LOGGER = null;

	public static void main(String[] args) {
		SpringApplication.run(SpringBootGraphQLApplication.class, args);
		Logger logger = Logger.getLogger(SpringBootGraphQLApplication.class.getName());
	}
}
