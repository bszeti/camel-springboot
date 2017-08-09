package my.company;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MyBuilder extends RouteBuilder {
	private final static Logger log = LoggerFactory.getLogger(MyBuilder.class);
	
	@Value("${server.ssl.enabled: false}")
	boolean sslEnabled;
	
	@Override
	public void configure() throws Exception {
		restConfiguration().component("servlet")
		.bindingMode(RestBindingMode.json)
		//Customize in/out Jackson objectmapper (two different instances): json.in.*, json.out.*
		.dataFormatProperty("json.out.include", "NON_NULL")
		.dataFormatProperty("json.out.disableFeatures", "WRITE_DATES_AS_TIMESTAMPS")
		
		//Swagger settings
		.contextPath("/api") //base.path; use the mapping set for CamelServlet
		.apiContextPath("/swagger") //swagger endpoint path; it will be under CamelServlet
		.apiContextRouteId("swagger") //id of route providing the swagger endpoint
		.apiProperty("api.title", "Example REST api")
		.apiProperty("api.version", "1.0")
		.apiProperty("schemes", sslEnabled ? "https" : "http" ) //Set https in swagger doc if ssl is enabled 
		.apiProperty("host", "") //Setting empty string as host so swagger-ui make relative url calls. By default 0.0.0.0 is used
		;
		
		rest()
		.post("/user").type(UserPojo.class)
			.responseMessage().code(200).endResponseMessage()
			.route().routeId("post-user")
			.log("User: ${body}")
			.endRest()
		.post("/country").type(CountryPojo.class)
			.responseMessage().code(200).endResponseMessage()
			.route().routeId("post-country")
			.log("Country: ${body}")
			.endRest()
		;
		
	}

}
