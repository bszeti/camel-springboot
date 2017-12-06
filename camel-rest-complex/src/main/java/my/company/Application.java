package my.company;

import my.company.model.HeaderValidationsPojo;
import my.company.utils.CustomMDCBreadCrumbIdUnitOfWork;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.converter.dozer.DozerTypeConverter;
import org.apache.camel.spi.UnitOfWork;
import org.apache.camel.spi.UnitOfWorkFactory;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.commons.dbcp2.BasicDataSourceFactory;
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

import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;

@SpringBootApplication
// load regular Spring XML file from the classpath
// Be careful with using "classpath:" or classpath*:" and fixed name or ant-style pattern. See PathMatchingResourcePatternResolver for details
// classpath:my.file returns the first file from classpath
// classpath*:my.file finds files from all classpath roots (/classes, /test-classes and jars)
// classpath:**/my*.file returns multiple files under the first classpath root (/classes OR /test-classes)
// classpath*:**/my*file returns matching files from all classpath roots
@ImportResource({"classpath:spring/application-context.xml","classpath*:spring/user-list*.xml"}) //
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
		public void beforeApplicationStart(CamelContext context) {
			//Add Dozer type converter using implicit mapping to map headers to pojo
			DozerTypeConverter dozerTypeConverter = new DozerTypeConverter(new DozerBeanMapper());
			context.getTypeConverterRegistry().addTypeConverter(HeaderValidationsPojo.class,Map.class, dozerTypeConverter);
			
			//Enable MDC logging
			context.setUseMDCLogging(true);
		}

        @Override
        public void afterApplicationStart(CamelContext context) {}
        };
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
	
}