package my.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import my.company.model.CitiesResponse;
import my.company.model.City;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.Produce;
import org.apache.camel.component.ehcache.EhcacheConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.MockEndpointsAndSkip;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static my.company.utils.StoredProcEmbedded.STATUS_ERROR;

@RunWith(CamelSpringBootRunner.class)
@ActiveProfiles("test") //The properties are merged from application.properties and application-test.properties
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@MockEndpointsAndSkip("cxf:bean:.*") //pattern supports wildcard "direct:get*" or regexp "direct:.*|cxf:.*"
public class StoredProcTest extends Assert {
	private static final Logger log = LoggerFactory.getLogger(StoredProcTest.class);

	//Create (new) FluentProducerTemplate instances
	@Produce(uri = "undertow:http://localhost:{{local.server.port}}/api/country/TEST/cities")
	FluentProducerTemplate fluentProducerTemplate;

    @Produce(uri = "ehcache://cityNamesCache?configUri=classpath:ehcache.xml&keyType=java.lang.String&valueType=java.util.List")
    FluentProducerTemplate cacheProducer;

	@EndpointInject(uri="mock:cxf:bean:cxfGlobalWeather") //Mock cxf endpoint, no SOAP call is done
	MockEndpoint mockGlobalWeather;

	String mockGlobalWeatherResponseBody; //This is only used in @Before, so no static is needed

	ObjectMapper objectMapper = new ObjectMapper();

	@TestConfiguration
	static class TestSpringConfiguration {

		//Create an embedded database with sql script
		@Bean(value = "cityInfoDS")
		@Primary
		public DataSource createCityInfoDS(){
			return new EmbeddedDatabaseBuilder()
					.setType(EmbeddedDatabaseType.DERBY)
					.setName("CityInfoDatabase")
					.addScript("sql/GETZIPS.sql")
					.addScript("sql/StoredProcTest.sql")
					.build();
		}
	}

	@Before
	public void before(){
		//Prepare common fluentProducerTemplate config
		fluentProducerTemplate
				.withHeader(Exchange.HTTP_METHOD, HttpMethod.GET)
				.withHeader(Exchange.CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.withHeader(Exchange.ACCEPT_CONTENT_TYPE, MediaType.APPLICATION_JSON);

		//Reset mock endpoint and set response by current test
		mockGlobalWeather.reset();
		mockGlobalWeather.whenAnyExchangeReceived(
				(e) -> {
					e.getIn().setBody(mockGlobalWeatherResponseBody);
				}
		);

		//Clear cache as the cache is common for all tests
        cacheProducer.withHeader(EhcacheConstants.ACTION,EhcacheConstants.ACTION_CLEAR).send();
	}

	@Test
	public void singleCity() throws Exception {
		mockGlobalWeatherResponseBody = "<NewDataSet><Table><Country>TEST</Country><City>AA</City></Table></NewDataSet>";

		Exchange response =  fluentProducerTemplate.send();
		String responseBody = response.getIn().getBody(String.class);
		CitiesResponse citiesResponse = objectMapper.readValue(responseBody,CitiesResponse.class);

		assertEquals(200, response.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
		assertEquals("TEST", citiesResponse.getCountry());

		List<City> expectedCities = Arrays.asList(
				new City("AA", Arrays.asList("ZIP-AA"))
		);
		log.info("Expected cities: {}",objectMapper.writeValueAsString(expectedCities));
		log.info("Response cities: {}",objectMapper.writeValueAsString(citiesResponse.getCities()));

		assertEquals(expectedCities, citiesResponse.getCities());
	}

	@Test
	public void multipleCities() throws Exception {
		mockGlobalWeatherResponseBody = "<NewDataSet><Table><Country>TEST</Country><City>AA</City></Table><Table><Country>TEST</Country><City>BB</City></Table></NewDataSet>";

		Exchange response =  fluentProducerTemplate.send();
		String responseBody = response.getIn().getBody(String.class);
		CitiesResponse citiesResponse = objectMapper.readValue(responseBody,CitiesResponse.class);

		assertEquals(200, response.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
		assertEquals("TEST", citiesResponse.getCountry());

		List<City> expectedCities = Arrays.asList(
				new City("AA", Arrays.asList("ZIP-AA")),
				new City("BB", Arrays.asList("ZIP-BB1","ZIP-BB2"))
		);
		//Sort response list by city name
		citiesResponse.getCities().sort(Comparator.comparing(City::getName));
		assertEquals(expectedCities, citiesResponse.getCities());
	}

	@Test
	public void noZipCity() throws Exception {
		mockGlobalWeatherResponseBody = "<NewDataSet><Table><Country>TEST</Country><City>XX</City></Table></NewDataSet>";

        Exchange response =  fluentProducerTemplate.send();
        String responseBody = response.getIn().getBody(String.class);
        CitiesResponse citiesResponse = objectMapper.readValue(responseBody,CitiesResponse.class);

        assertEquals(200, response.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
        assertEquals("TEST", citiesResponse.getCountry());

        List<City> expectedCities = Arrays.asList(
                new City("XX", Arrays.asList())
        );
        assertEquals(expectedCities, citiesResponse.getCities());
	}

	@Test
    public void statusError() throws Exception {
        mockGlobalWeatherResponseBody = "<NewDataSet><Table><Country>TEST</Country><City>"+STATUS_ERROR+"</City></Table></NewDataSet>";

        Exchange response =  fluentProducerTemplate.send();
        String responseBody = response.getIn().getBody(String.class);
        CitiesResponse citiesResponse = objectMapper.readValue(responseBody,CitiesResponse.class);

        assertEquals(200, response.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
        assertEquals("TEST", citiesResponse.getCountry());
        assertEquals("Status 1 - Unexpected city", citiesResponse.getCities().get(0).getError());
    }

}
