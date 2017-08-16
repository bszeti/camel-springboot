package my.company;

import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

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