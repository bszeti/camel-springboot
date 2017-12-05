package my.company;

import my.company.model.CountryApiPojo;
import my.company.model.UserApiPojo;
import org.apache.camel.*;
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
import org.springframework.test.context.ActiveProfiles;

@RunWith(CamelSpringBootRunner.class)
@UseAdviceWith //The context won't start automatically
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode=ClassMode.AFTER_CLASS) //classMode default value. Shutdown spring context after class (all tests are run using the same context)
public class PostTest extends Assert {

	//Local server port can be injected also available in the context as {{local.server.port}}.
	@LocalServerPort
	Integer port;

	@EndpointInject(uri = "mock:user")
	protected MockEndpoint resultEndpointUser;
	@EndpointInject(uri = "mock:country")
	protected MockEndpoint resultEndpointCountry;

	@Autowired
	protected ProducerTemplate template;
	//or can use FluentProducerTemplate
	@Produce(uri = "undertow:http://localhost:{{local.server.port}}/api/user")
	FluentProducerTemplate userApiFluentProducer;

	@Autowired
	CamelContext context;

	@Before
	public void before() throws Exception {
		//Before is called for each methods, but we only want to run this once as the context is created once for the whole class
		if (context.getStatus()==ServiceStatus.Stopped) {
			
			context.getRouteDefinition("post-user").adviceWith(context, new AdviceWithRouteBuilder() {
				@Override
				public void configure() throws Exception {
					weaveById("received-user").before()
							.to("log:my.company.PostTest?showAll=true&multiline=true")
							.to(resultEndpointUser.getEndpointUri());
	
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
		} else {
			//If context was already started (there was an earlier test run) then reset mock endpoints otherwise the tests can interfere
			resultEndpointUser.reset();
			resultEndpointCountry.reset();
		}
	}


	@Test
	public void postUserApi() throws Exception {
		UserApiPojo user = new UserApiPojo("Test User", 20);
		resultEndpointUser.expectedBodiesReceived(user);
		resultEndpointUser.expectedMessageCount(1);

		//Send a message to user api post endpoint using the FluentProducerTemplate
		userApiFluentProducer.withHeader(Exchange.HTTP_METHOD, HttpMethod.POST)
				.withHeader(Exchange.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.withHeader(Exchange.ACCEPT_CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.withBody("{\"age\": 20, \"name\": \"Test User\"}")
				.send();

		resultEndpointUser.assertIsSatisfied();

	}

	@Test
	public void testMultiplePostTypes() throws Exception {

		UserApiPojo user = new UserApiPojo("My Name", 21);
		resultEndpointUser.expectedBodiesReceived(user);
		resultEndpointUser.expectedMessageCount(1);

		CountryApiPojo country = new CountryApiPojo();
		country.setCountry("England");
		country.setIso("EN");
		resultEndpointCountry.expectedBodiesReceived(country);
		resultEndpointCountry.expectedMessageCount(1);

		//Send a message to each post endpoint
		ExchangeBuilder builder = ExchangeBuilder.anExchange(context)
				.withHeader(Exchange.HTTP_METHOD, HttpMethod.POST)
				.withHeader(Exchange.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.withHeader(Exchange.ACCEPT_CONTENT_TYPE, MediaType.APPLICATION_JSON);
		Exchange outExchangeUser = builder.withBody("{\"age\": 21, \"name\": \"My Name\"}").build();
		Exchange outExchangeCountry = builder.withBody("{\"iso\": \"EN\", \"country\": \"England\"}").build();

		template.send("undertow:http://localhost:" + port + "/api/user", outExchangeUser);
		template.send("undertow:http://localhost:" + port + "/api/country", outExchangeCountry);

		resultEndpointCountry.assertIsSatisfied();
		resultEndpointUser.assertIsSatisfied();

	}

}
