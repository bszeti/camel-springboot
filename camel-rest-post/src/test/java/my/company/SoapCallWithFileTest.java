package my.company;

import my.company.model.ApiResponse;
import my.company.model.CitiesResponse;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.MockEndpointsAndSkip;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.stream.Collectors;

@RunWith(CamelSpringBootRunner.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@MockEndpointsAndSkip("direct:getCityZips")
public class SoapCallWithFileTest extends Assert {
	
	@Autowired
	TestRestTemplate testRestTemplate; // Supports relative URLs to call the server running on random port

	@Autowired
	CamelContext context;
	
	//The assumption is that tests run sequentially or in separate JVMs so static variables can be used
	static String getCitiesByCountryResponseResource;
	static int getCitiesByCountryResponseCode = 200;

	@TestConfiguration
	static class TestSpringConfiguration {

		// Register a CXF servlet in the web container for the test
		@Bean
		public ServletRegistrationBean cxfServlet() {
			return new ServletRegistrationBean(new CXFServlet(), "/cxf-test/*");
		}

		@Bean
		public RouteBuilder createSoapService() {
			return new RouteBuilder() {
				@Override
				public void configure() throws Exception {
					from("cxf:/GlobalWeather?dataFormat=MESSAGE")
							.routeId("test-GlobalWeatherSoap")
							.choice()
							.when(header(SoapBindingConstants.SOAP_ACTION).contains("GetCitiesByCountry"))
								.process((e) -> {
									e.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, getCitiesByCountryResponseCode);
									e.getIn().setBody(getClass().getClassLoader().getResourceAsStream(getCitiesByCountryResponseResource));
									
								})
							.end()
							;
				}
			};
		}
	}

	@Test
	public void successfulResponse() throws Exception {
		getCitiesByCountryResponseResource = "cxf/GlobalWeatherSoap-succ.xml";
		
		ResponseEntity<CitiesResponse> citiesResponse = testRestTemplate.getForEntity("/api/country/TEST/cities",CitiesResponse.class);

		assertEquals(200, citiesResponse.getStatusCodeValue());
		assertEquals("TEST", citiesResponse.getBody().getCountry());
		//Here we sort the response so ordering causes no problems
		assertEquals(Arrays.asList("AA","BB"), citiesResponse.getBody().getCities().stream().map((c)->c.getName()).sorted().collect(Collectors.toList()));

	}
	
	@Test
	public void soapFaultResponse() throws Exception {
		getCitiesByCountryResponseResource = "cxf/GlobalWeatherSoap-fault.xml";
		
		ResponseEntity<ApiResponse> apiResponse = testRestTemplate.getForEntity("/api/country/TEST/cities",ApiResponse.class);
		assertEquals(500, apiResponse.getStatusCodeValue());
		assertEquals(5002, apiResponse.getBody().getCode());
		assertEquals("Error calling soap service: Test Fault", apiResponse.getBody().getMessage());

	}

}
