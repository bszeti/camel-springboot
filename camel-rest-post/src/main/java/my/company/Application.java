package my.company;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.apache.camel.converter.dozer.DozerTypeConverter;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.dozer.DozerBeanMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import my.company.model.HeadersPojo;

@SpringBootApplication
// load regular Spring XML file from the classpath that contains the Camel XML DSL
@ImportResource({"classpath:spring/camel-context.xml"})
public class Application {

    /**
     * A main method to start this application.
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * Register Camel into the web container
     */
    @Bean
    ServletRegistrationBean servletRegistrationBean(){
    	ServletRegistrationBean servlet = new ServletRegistrationBean(new CamelHttpTransportServlet(), "/api/*");
    	servlet.setName("CamelServlet"); //Name must be CamelServlet
    	return servlet;
    }
    
    @Bean
    /**
     * Customize CamelContext. 
     * Add Dozer to convert map to pojo.
     */
    CamelContextConfiguration contextConfiguration() {
      return new CamelContextConfiguration() {
		@Override
		public void afterApplicationStart(CamelContext context) {}

		@Override
		public void beforeApplicationStart(CamelContext context) {
			//Add Dozer type converter using implicit mapping to map headers to pojo
			DozerTypeConverter dozerTypeConverter = new DozerTypeConverter(new DozerBeanMapper());
			context.getTypeConverterRegistry().addTypeConverter(HeadersPojo.class,Map.class, dozerTypeConverter);
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
    
   
    
}