package my.company.route;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Component;

@Component
public class RestConfiguration extends RouteBuilder {
	private final static Logger log = LoggerFactory.getLogger(RestConfiguration.class);
	
	//The properties bean autoconfigured by application properties
	@Autowired
	ServerProperties serverProperties;
	
	@Override
	public void configure() throws Exception {
		/************************
		 * Rest configuration. There should be only one in a CamelContext
		 ************************/
		restConfiguration().component("servlet") //Requires "CamelServlet" to be registered
			.bindingMode(RestBindingMode.json)
			//Customize in/out Jackson objectmapper, see JsonDataFormat. Two different instances): json.in.*, json.out.*
			.dataFormatProperty("json.in.moduleClassNames", "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule")
			.dataFormatProperty("json.out.include", "NON_NULL")
			.dataFormatProperty("json.out.disableFeatures", "WRITE_DATES_AS_TIMESTAMPS")
			.dataFormatProperty("json.out.moduleClassNames", "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule")
			
			
			//Enable swagger endpoint. It's actually served by a Camel route
			.apiContextPath("/swagger") //swagger endpoint path; Final URL: Camel path + apiContextPath: /api/swagger
			.apiContextRouteId("swagger") //id of route providing the swagger endpoint
			
			.contextPath("/api") //base.path swagger property; use the mapping URL set for CamelServlet camel.component.servlet.mapping.contextPath
			.apiProperty("api.title", "Example REST api")
			.apiProperty("api.version", "1.0")
			//.apiProperty("schemes", "" ) //Setting empty string as scheme to support relative urls
			.apiProperty("schemes", serverProperties.getSsl() != null && serverProperties.getSsl().isEnabled() ? "https" : "http" )
			.apiProperty("host", "") //Setting empty string as host so swagger-ui make relative url calls. By default 0.0.0.0 is used
			;
	}
	
}
