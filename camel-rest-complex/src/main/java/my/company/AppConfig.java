package my.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import my.company.utils.BasicDataSourceMixIn;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

/**
 * Additional beans
 * Beans usually can be configured in the Application.java but in some cases it can cause an infinite dependency loop for unit tests.
 * For example ObjectMapper customization with using RestTemplate in unit tests had that problem
 *
 */
@Configuration
public class AppConfig {
	/**
	 * Customize the Spring auto-configured Jackson ObjectMapper (not used by Camel rest).
	 * In this example we set marshaling rules for BasicDataSource.class
	 */
	@Bean
	public Jackson2ObjectMapperBuilderCustomizer customObjectMapper() {
		return new Jackson2ObjectMapperBuilderCustomizer() {
			@Override
			public void customize(Jackson2ObjectMapperBuilder jacksonObjectMapperBuilder) {
				jacksonObjectMapperBuilder
						.mixIn(BasicDataSource.class, BasicDataSourceMixIn.class)
						.indentOutput(true);
			}
		};
	}

	/**
	 * Example how skip auto-configuration and use a custom ObjectMapper.
	 * Also can use a Jackson2ObjectMapperBuilder bean for the same purpose.
	 */
	//@Bean
	ObjectMapper customObjectMapperForSpring() {
		ObjectMapper mapper = new ObjectMapper();
		JavaTimeModule javaTimeModule = new JavaTimeModule();
		javaTimeModule.addSerializer(new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("YYYY-MMM")));
		mapper.registerModule(javaTimeModule);
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		mapper.setDateFormat(new SimpleDateFormat("YYYY"));
		return mapper;
	}
}
