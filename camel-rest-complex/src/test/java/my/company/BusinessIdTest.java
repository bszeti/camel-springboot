package my.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import my.company.model.ApiResponse;
import my.company.route.RestEndpoints;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.MockEndpointsAndSkip;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext
public class BusinessIdTest extends Assert {
	private static final Logger log = LoggerFactory.getLogger(BusinessIdTest.class);

	//@Autowired - works only in Camel 2.19.3+
	protected FluentProducerTemplate fluentProducerTemplate;

	@Autowired
	CamelContext context;
	
	@Autowired
	ObjectMapper objectmapper;

	@Before
	public void before() throws Exception {
		fluentProducerTemplate = context.createFluentProducerTemplate();
	}

	@Test
	public void noBusinessId() throws Exception {
		// Call get
		Exchange response = fluentProducerTemplate.to("undertow:http://localhost:{{local.server.port}}/api/user/1")
				.withHeader(Exchange.HTTP_METHOD, HttpMethod.GET)
				.withHeader(Exchange.ACCEPT_CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.send();
		
		assertEquals(200, response.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
		assertTrue(response.getIn().getHeader(RestEndpoints.HEADER_BUSINESSID,String.class).length() == 36);
	}

	@Test
	public void shortBusinessId() throws Exception {
		// Call get
		Exchange response = fluentProducerTemplate.to("undertow:http://localhost:{{local.server.port}}/api/user/1")
				.withHeader(Exchange.HTTP_METHOD, HttpMethod.GET)
				.withHeader(Exchange.ACCEPT_CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.withHeader(RestEndpoints.HEADER_BUSINESSID,"X")
				.send();
		
		assertEquals(500, response.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
		assertEquals("X",response.getIn().getHeader(RestEndpoints.HEADER_BUSINESSID,String.class));
		
		String responseBody = response.getIn().getBody(String.class); //Body is byte[] (or may be InputStream in case of other http clients)
		log.info("body: {}",responseBody);
		ApiResponse apiResponse = objectmapper.readValue(responseBody, ApiResponse.class); //Unmarshall manually with Jackson
		assertEquals("'businessId' length must be between 16 and 48", apiResponse.getMessage());
		
	}
	
	@Test
	public void testConfigProps() throws Exception {
		// Call get
		Exchange response = fluentProducerTemplate.to("undertow:http://localhost:{{local.server.port}}/configprops")
				.withHeader(Exchange.HTTP_METHOD, HttpMethod.GET)
				.withHeader(Exchange.ACCEPT_CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.send();
		
		assertEquals(200, response.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
	}

}
