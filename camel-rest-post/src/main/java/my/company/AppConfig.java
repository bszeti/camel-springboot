package my.company;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.BasicDataSourceFactory;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import my.company.utils.BasicDataSourceMixIn;

/**
 * Additional beans
 * Beans usually can be configured in the Application.java but in some cases it can cause an infinite dependency loop for unit tests.
 * For example ObjectMapper customization with using RestTemplate in unit tests had that problem
 *
 */
@Configuration
public class AppConfig {

	/**
	 * Two datasources. One mast be marked as primary for autowired.
	 * The BasicDataSource properties (including pooling) is set from configuration.
	 * destroyMethod is set to null to avoid notification of double shutdown
	 * 
	 * SpringBoot's spring.datasource.* auto-configuration works as long as only one datasource is required.
	 * This example shows a simple way to create two data sources with properties taken from the config file
	 * (The @ConfigurationProperties could also be used on the BasicDataSource instance directly, but that causes problems with the /configprops actuator endpoint)
	 */
	@Bean
	@ConfigurationProperties("cityInfo.datasource")
	public Properties primaryDataSourceProperties() {
		return new Properties();
	}
	
	@Bean(value = "cityInfoDS", destroyMethod = "") //Disable destroy method here to avoid warning for duplicated shutdown
	@Primary
	public DataSource primaryDataSource() throws Exception {
		return BasicDataSourceFactory.createDataSource(primaryDataSourceProperties());
	}

	@Bean
	@ConfigurationProperties("another.datasource")
	public Properties anotherDataSourceProperties() {
		return new Properties();
	}

	@Bean(destroyMethod = "")
	public DataSource anotherDataSource() throws Exception {
		return BasicDataSourceFactory.createDataSource(anotherDataSourceProperties());
	}


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
