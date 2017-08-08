package my.company;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

@Component
public class MyBuilder extends RouteBuilder {
	@Override
	public void configure() throws Exception {
		restConfiguration().component("servlet")
		.bindingMode(RestBindingMode.json)
		
		//Swagger settings
		.contextPath("/api") //basePath; mapping set for CamelServlet
		.apiContextPath("/swagger") //swagger endpoint path; it will be under CamelServlet
		.apiContextRouteId("swagger") //id of route providing the swagger endpoint
		.apiProperty("api.title", "Example REST api")
		.apiProperty("api.version", "1.0")
		.scheme("http,https")
		.host("localhost:8080")
		;
		
		rest().tag("services")
		.post("/user").type(UserPojo.class)
			.route().routeId("post-user")
			.log("User: ${body}")
			.endRest()
		.post("/country").type(CountryPojo.class)
			.route().routeId("post-country")
			.log("Country: ${body}")
			.endRest()
		;
		
	}

}
