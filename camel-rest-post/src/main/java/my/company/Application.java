package my.company;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.converter.dozer.DozerTypeConverter;
import org.apache.camel.spi.UnitOfWork;
import org.apache.camel.spi.UnitOfWorkFactory;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.commons.dbcp2.BasicDataSource;
import org.dozer.DozerBeanMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import my.company.model.HeadersPojo;
import my.company.utils.CustomMDCBreadCrumbIdUnitOfWork;

@SpringBootApplication
// load regular Spring XML file from the classpath that contains the Camel XML DSL
@ImportResource({"classpath:spring/camel-*.xml"})
public class Application {
	private final static Logger log = LoggerFactory.getLogger(Application.class);

    /**
     * A main method to start this application.
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * Register Camel servlet into the web container
     * This is only required before Camel v2.19 as it'a automatically done by ServletMappingAutoConfiguration afterwards.
     * See ServletMappingConfiguration mapping properties: camel.component.servlet.mapping
     */
//    @Bean
//    ServletRegistrationBean camelServletRegistrationBean(){
//    	ServletRegistrationBean servlet = new ServletRegistrationBean(new CamelHttpTransportServlet(), "/api/*");
//    	servlet.setName("CamelServlet"); //Name must be CamelServlet
//    	return servlet;
//
//    }
    
    /**
     * Customize CamelContext. 
     */
    @Bean
    CamelContextConfiguration contextConfiguration() {
      return new CamelContextConfiguration() {
		@Override
		public void afterApplicationStart(CamelContext context) {}

		@Override
		public void beforeApplicationStart(CamelContext context) {
			//Add Dozer type converter using implicit mapping to map headers to pojo
			DozerTypeConverter dozerTypeConverter = new DozerTypeConverter(new DozerBeanMapper());
			context.getTypeConverterRegistry().addTypeConverter(HeadersPojo.class,Map.class, dozerTypeConverter);
			
			//Enable MDC logging
			context.setUseMDCLogging(true);
			
			//Test log replace
			log.info("myPrimaryDataSource secret:MySecret");
		}
      };
    }
    
    /**
    * This is only a simple redirect to access swagger UI easier
    * from "/swagger-ui" to "/swagger-ui/index.html?url=/api/swagger&validatorUrl="
    */
    @Controller
    class SwaggerWelcome {
        @RequestMapping(
            "/swagger-ui"
        )
        public String redirectToUi() {
            return "redirect:/webjars/swagger-ui/index.html?url=/api/swagger&validatorUrl=";
        }
    }
    
    /**
     * Simple Spring rest endpoint
     *
     */
    @RestController
    class SpringRestService {
    	@RequestMapping("/")
    	public String hello() { return "Hello World"; }
    }
    
	/**
	 * Datasources. One mast be marked as primary for autowired. The BasicDataSource
	 * properties (including pooling) are set by configuration. destroyMethod is set to null to avoid notification of double shutdown
	 */
	@Bean(value = "myPrimaryDS", destroyMethod = "")
	@Primary
	@ConfigurationProperties("ds.primary")
	public DataSource myPrimaryDataSource() {
		return new BasicDataSource();
	}

	@Bean(value = "mySecondaryDS", destroyMethod = "")
	@ConfigurationProperties("ds.secondary")
	public DataSource mySecondaryDataSource() {
		return new BasicDataSource();
	}
	
	/**
	 * Enable custom unit of work. UnitOfWorkFactory bean is automatically picked up by Camel context
	 */
	@Bean
	UnitOfWorkFactory customUnitOfWorkFactory() {
		return new UnitOfWorkFactory() {
			@Override
			public UnitOfWork createUnitOfWork(Exchange exchange) {
				return new CustomMDCBreadCrumbIdUnitOfWork(exchange);
			}
		};
	}
	
	/**
	 * Example how to register a custom Jackson ObjectMapper for Spring (not used by Camel rest). Uncomment @Bean if needed.
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