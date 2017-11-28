package my.company;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.UseAdviceWith;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import my.company.model.CountryPojo;
import my.company.model.UserPojo;

@RunWith(CamelSpringBootRunner.class)
@UseAdviceWith
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode=ClassMode.AFTER_CLASS) //classMode default value. Shutdown spring context after class (all tests are run using the same context)
public class RestTest extends Assert {

	//Local server port can be injected also available in the context as {{local.server.port}}.
	@LocalServerPort
	Integer port;

	@EndpointInject(uri = "mock:user")
	protected MockEndpoint resultEndpointUser;
	@EndpointInject(uri = "mock:country")
	protected MockEndpoint resultEndpointCountry;

	@Autowired
	protected ProducerTemplate template;

	@Autowired
	CamelContext context;

	@Before
	public void before() throws Exception {
		//Before is called for each methods, but we only want to run this once
		if (context.getStatus()==ServiceStatus.Stopped) {
			
			context.getRouteDefinition("post-user").adviceWith(context, new AdviceWithRouteBuilder() {
				@Override
				public void configure() throws Exception {
					weaveById("received-user").before().to(resultEndpointUser.getEndpointUri());
	
				}
			});
	
			context.getRouteDefinition("post-country").adviceWith(context, new AdviceWithRouteBuilder() {
				@Override
				public void configure() throws Exception {
					weaveById("received-country").before().to(resultEndpointCountry.getEndpointUri());
	
				}
			});
			
			//Manually start context after adviceWith
			context.start();
		}
	}

	@Test
	public void testMultiplePostTypes() throws Exception {

		UserPojo user = new UserPojo("My Name", 21);
		resultEndpointUser.expectedBodiesReceived(user);
		resultEndpointUser.expectedMessageCount(1);

		CountryPojo country = new CountryPojo();
		country.setCountry("England");
		country.setIso("EN");
		resultEndpointCountry.expectedBodiesReceived(country);
		resultEndpointCountry.expectedMessageCount(1);

		//Send a message to each post endpoint
		ExchangeBuilder builder = ExchangeBuilder.anExchange(context)
				.withHeader(Exchange.HTTP_METHOD, HttpMethod.POST)
				.withHeader(Exchange.ACCEPT_CONTENT_TYPE, MediaType.APPLICATION_JSON);
		Exchange outExchangeUser = builder.withBody("{\"age\": 21, \"name\": \"My Name\"}").build();
		Exchange outExchangeCountry = builder.withBody("{\"iso\": \"EN\", \"country\": \"England\"}").build();

		template.send("undertow:http://localhost:" + port + "/api/user", outExchangeUser);
		template.send("undertow:http://localhost:" + port + "/api/country", outExchangeCountry);

		resultEndpointCountry.assertIsSatisfied();
		resultEndpointUser.assertIsSatisfied();

	}

}
