package my.company;

import my.company.model.CitiesResponse;
import my.company.model.CountryApiPojo;
import my.company.model.UserApiPojo;
import net.webservicex.GlobalWeatherSoap;
import org.apache.camel.*;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.UseAdviceWith;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {"service.url=undertow:http://localhost:{{local.server.port}}/api"})
@ActiveProfiles({"test"})
@DirtiesContext(classMode=ClassMode.AFTER_CLASS) //classMode default value. Shutdown spring context after class (all tests are run using the same context)
public class CallRestServiceTest extends Assert {
	private static final Logger log = LoggerFactory.getLogger(CallRestServiceTest.class);

	//or can use FluentProducerTemplate
	@Produce(uri = "direct:callRestServiceUndertow")
	FluentProducerTemplate producerTemplate;

	@Autowired
	CamelContext context;

	@TestConfiguration
	static class TestSpringConfiguration {

		// Register a CXF servlet in the web container for the test
		@Bean
		public ServletRegistrationBean cxfServlet() {
			return new ServletRegistrationBean(new CXFServlet(), "/cxf-test/*");
		}
		//CXF service
		@Bean
		public RouteBuilder createSoapService() {
			return new RouteBuilder() {
				@Override
				public void configure() throws Exception {
					from("cxf:/GlobalWeather?serviceClass=" + GlobalWeatherSoap.class.getName())
							.routeId("test-GlobalWeatherSoap")
							.setBody(constant("<NewDataSet><Table><Country>TEST</Country><City>AA</City></Table></NewDataSet>"));
				}

			};
		}

		//Create an embedded database with sql script
		@Bean(value = "cityInfoDS")
		@Primary
		public DataSource createCityInfoDS(){
			return new EmbeddedDatabaseBuilder()
					.setType(EmbeddedDatabaseType.DERBY)
					.setName("CityInfoDatabase")
					.addScript("sql/GETZIPS.sql")
					.addScript("sql/CallRestServiceTest.sql")
					.build();
		}
	}

	@Before
	public void before() throws Exception {

	}


	@Test
	public void callRestServiceHttp4() throws Exception {
		Exchange response = producerTemplate.withHeader("country","TEST").to("direct:callRestServiceHttp4").send();
		CitiesResponse citiesResponse = response.getIn().getBody(CitiesResponse.class);
		log.info("citiesResponse: {}",citiesResponse);

		assertEquals("TEST", citiesResponse.getCountry());
		assertEquals("AA", citiesResponse.getCities().get(0).getName());
		assertEquals("ZIP-AA", citiesResponse.getCities().get(0).getZips().get(0));

	}

	@Test
	public void callRestServiceHttp() throws Exception {
		Exchange response = producerTemplate.withHeader("country","TEST").to("direct:callRestServiceHttp").send();
		CitiesResponse citiesResponse = response.getIn().getBody(CitiesResponse.class);
		log.info("citiesResponse: {}",citiesResponse);

		assertEquals("TEST", citiesResponse.getCountry());
		assertEquals("AA", citiesResponse.getCities().get(0).getName());
		assertEquals("ZIP-AA", citiesResponse.getCities().get(0).getZips().get(0));

	}

}
