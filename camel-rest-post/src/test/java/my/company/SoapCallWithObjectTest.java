package my.company;

import my.company.model.ApiResponse;
import my.company.model.CitiesResponse;
import my.company.route.RestEndpoints;
import net.webservicex.GlobalWeatherSoap;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.MockEndpointsAndSkip;
import org.apache.commons.collections.CollectionUtils;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
public class SoapCallWithObjectTest extends Assert {

	@Autowired
	TestRestTemplate testRestTemplate; // Supports relative URLs to call the server running on random port

	@Autowired
	CamelContext context;

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
					from("cxf:/GlobalWeather?serviceClass=" + GlobalWeatherSoap.class.getName())
							.routeId("test-GlobalWeatherSoap")
							.toD("direct:${header." + CxfConstants.OPERATION_NAME + "}");

					from("direct:GetCitiesByCountry")
						.setProperty("country",simple("${body[0]}",String.class)) //The method arguments are in a org.apache.cxf.message.MessageContentsList
						.process((e) -> {
							switch(e.getProperty("country",String.class)) {
							case "TEST":
								e.getIn().setBody("<NewDataSet><Table><Country>TEST</Country><City>AA</City></Table><Table><Country>TEST</Country><City>BB</City></Table></NewDataSet>");
								break;
							default:
								e.getIn().setBody("<NewDataSet/>");
							}
						});

				}

			};
		}
	}

	@Before
	public void before() throws Exception {
	}

	@Test
	public void successfulResponse() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.set(RestEndpoints.HEADER_BUSINESSID, "successfulResponse-test");
		ResponseEntity<CitiesResponse> citiesResponse = testRestTemplate.exchange("/api/country/TEST/cities",
				HttpMethod.GET, new HttpEntity(headers), CitiesResponse.class);

		assertEquals(200, citiesResponse.getStatusCodeValue());
		assertEquals("successfulResponse-test", citiesResponse.getHeaders().getFirst(RestEndpoints.HEADER_BUSINESSID));
		assertEquals("TEST", citiesResponse.getBody().getCountry());
		// Check if lists are equal ignoring the ordering of elements
		assertTrue(CollectionUtils.isEqualCollection(
				Arrays.asList("AA","BB"),
				citiesResponse.getBody().getCities().stream().map((c)->c.getName()).collect(Collectors.toList())
		));

	}
	
	@Test
	public void emptyCitiesResponse() throws Exception {
		ResponseEntity<ApiResponse> apiResponse = testRestTemplate.getForEntity("/api/country/EMPTY/cities",ApiResponse.class);

		assertEquals(404, apiResponse.getStatusCodeValue());
		assertEquals(4000, apiResponse.getBody().getCode());
		assertEquals("Not found", apiResponse.getBody().getMessage());

	}

}
