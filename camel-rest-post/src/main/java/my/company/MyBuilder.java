package my.company;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

@Component
public class MyBuilder extends RouteBuilder {
	@Override
	public void configure() throws Exception {
		restConfiguration().component("servlet")
		.contextPath("/api")
		.bindingMode(RestBindingMode.json);
		
		rest("/")
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
