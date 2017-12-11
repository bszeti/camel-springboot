package my.company.route;

import com.fasterxml.jackson.core.JsonProcessingException;
import my.company.model.*;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.bean.validator.BeanValidationException;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.camel.component.ehcache.EhcacheConstants;
import org.apache.camel.language.XPath;
import org.apache.camel.model.rest.RestParamType;
import org.apache.cxf.binding.soap.SoapFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.NodeList;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.camel.builder.PredicateBuilder.not;

@Component("restbuilder")
public class RestEndpoints extends RouteBuilder {
	private final static Logger log = LoggerFactory.getLogger(RestEndpoints.class);
	
	private final static UserApiPojo DUMMY_USER = new UserApiPojo("JohnDoe", 100);

	public final static String HEADER_BUSINESSID = "businessId"; //Optional custom correlation id received/sent from/to external systems

	@Autowired
	@Qualifier("succResponse")
	private ApiResponse SUCC;
		
	@Override
	public void configure() throws Exception {

		/************************
		 * common exception handlers for all routes defined in this RouteBuilder
		 ************************/
		onException(JsonProcessingException.class)
			.handled(true)
			.to("log:"+RestEndpoints.class.getName()+"?showAll=true&multiline=true&level=ERROR")
			.removeHeaders("*",HEADER_BUSINESSID) //don't let message headers get inserted in the http response
			.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
			.bean("restbuilder","errorResponse(4000,'Invalid json content')");
			
		onException(Exception.class)
			.handled(true)
			.to("log:"+RestEndpoints.class.getName()+"?showAll=true&multiline=true&level=ERROR")
			.removeHeaders("*",HEADER_BUSINESSID) //don't let message headers get inserted in the http response
			.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
			.bean("restbuilder","errorResponse(*)");
		
		/************************
		 * Rest endpoints. Multiple can be defined (in multiple RouteBuilder), but should map different URL path
		 ************************/

		/************************
		 * Simple api with post and get: /user
		 * GET returns users defined in user-list.xml. E.g.: /user/2
		 * POST receives a user pojo (not stored)
		 ************************/
		rest("/user").description("User API")
			.produces(MediaType.APPLICATION_JSON).consumes(MediaType.APPLICATION_JSON)
			.skipBindingOnErrorCode(false) //Enable json marshalling for body in case of errors
		.get("/{id}")
			//swagger
			.description("Query user")
			.param().name("id").type(RestParamType.path).description("Id of the user. Must be number and less than 100.").required(true).dataType("string").endParam()
			.param().name(HEADER_BUSINESSID).type(RestParamType.header).description("Business transactionid. Defaults to a random uuid").required(false).dataType("string").endParam()
			.responseMessage().code(200).responseModel(UserApiPojo.class).endResponseMessage() //OK
			.responseMessage().code(500).responseModel(ApiResponse.class).endResponseMessage() //Not-OK
			//route
			.route().routeId("user-get")
				.bean("routeHelper","logHeadersByPattern")

				//Validate headers
				.setBody().simple("${headers}",HeaderValidationsPojo.class)
				.to("bean-validator:validateHeaders") //or .validate().simple("${header.id} < 100")

				//Lookup user from beans with name "user[id]"
				.setBody(simple("${ref:user${headers.id}}"))
				.choice()
				.when(body().isNull()) // Return dummy user if missing
					.setBody(constant(DUMMY_USER))
				.end()
				.removeHeaders("*","businessId")
			.endRest()
		.post("/").type(UserApiPojo.class) //The post endpoint doesn't store the user, it only sends back a successful response
			//swagger
			.description("Send user")
			.param().name(HEADER_BUSINESSID).type(RestParamType.header).description("Business transaction id. Defaults to a random uuid").required(false).dataType("string").endParam()
			.responseMessage().code(200).responseModel(ApiResponse.class).endResponseMessage() //OK
			.responseMessage().code(400).responseModel(ApiResponse.class).message("Unexpected body").endResponseMessage() //Wrong input
			.responseMessage().code(500).responseModel(ApiResponse.class).endResponseMessage() //Not-OK
			//route 
			.route().routeId("post-user")
				.bean("routeHelper","logHeadersByPattern")
				.log("User received: ${body}").id("received-user") //This step gets an id, so we can refer it in test
				.setBody(constant(SUCC))
				.removeHeaders("*",HEADER_BUSINESSID)
			.endRest();

		/************************
		 * Another rest endpoint: /country
		 * GET - A complex endpoint calling a SOAP service and stored procedure to build response
		 * POST is here only to demonstrates that a different unmarshalling type can be set than in the previous post endpoint
		 ************************/
		rest("/country").description("Country API")
			.skipBindingOnErrorCode(false)
		.get("/{country}/cities")
			.description("Get cities of country by calling s SOAP service")
			.responseMessage().code(200).responseModel(CitiesResponse.class).endResponseMessage() //OK
			.responseMessage().code(500).responseModel(ApiResponse.class).endResponseMessage() //Not-OK
			.to("direct:getCitiesWithZip") //Implementation is in a direct route
		.post("/").type(CountryApiPojo.class)
			//swagger
			.description("Send country")
			.responseMessage().code(200).responseModel(ApiResponse.class).endResponseMessage()
			.route().routeId("post-country")
				.log("Country received: ${body}").id("received-country") //This step gets an id, so we can refer it in test
				.setBody(constant(SUCC))
				.removeHeaders("*",HEADER_BUSINESSID)
			.endRest()
		;

		//Get city list for country (from a webservice) and zip codes for each city (calling a stored procedure)
		from("direct:getCitiesWithZip").routeId("get-cities-with-zip")
				//An extra onException handler only for this route
				.onException(ValidationException.class)
					.handled(true)
					.removeHeaders("*",HEADER_BUSINESSID)
					.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
					.bean("restbuilder","errorResponse(4000,'Not found')")
					.end()

				.log("Getting cities for ${header.country}")

				//Save input headers/values needed later as ExchangeProperty
				.setProperty("country",header("country"))

				//Get cities for a country

				//Try to get country from cache first
				.setHeader(EhcacheConstants.ACTION, constant(EhcacheConstants.ACTION_GET))
				.setHeader(EhcacheConstants.KEY, exchangeProperty("country"))
				.to("ehcache://cityNamesCache?configUri=classpath:ehcache.xml&keyType=java.lang.String&valueType=java.util.List")
				.choice()
				.when(not(header(EhcacheConstants.ACTION_HAS_RESULT)))
					//Call SOAP service with CXF to get list of city names
					.log(LoggingLevel.DEBUG,"Calling SOAP service")
					.removeHeaders("*", HEADER_BUSINESSID)
					.setBody(exchangeProperty("country")) //This is a very simple service where the request object is a String (instead of something like GetCitiesByCountryRequest)
					.setHeader(CxfConstants.OPERATION_NAME,constant("GetCitiesByCountry"))
					.to("cxf:bean:cxfGlobalWeather?synchronous=true") //Use synchronous to use the same thread to make the http call
					.setBody(method(this,"getCityNamesFromXML"))
					.to("log:country-get-cities?showAll=true&multiline=true&level=DEBUG")
					//Add to cache
					.setHeader(EhcacheConstants.ACTION, constant(EhcacheConstants.ACTION_PUT))
					.setHeader(EhcacheConstants.KEY, exchangeProperty("country"))
					.to("ehcache://cityNamesCache?configUri=classpath:ehcache.xml&keyType=java.lang.String&valueType=java.util.List")
				.end()
				.validate(body().isNotEqualTo(new ArrayList<String>())) //Verify that the the result is not an empty list //TODO: Validate doesn't work in choice??
				.setProperty("cityNames",body())

				//Get zip codes from the database for each city,
				//Also give back partial response in case of errors, and show the error message next to the city
				//First create an empty list, it will be populated from inside the splitter
				.setProperty("cities",method(this,"emptyCityList"))
				//Splitter gives back the original Exchange if no aggregationStrategy is set (multicast gives back the last Exchange),
				//an exception inside the splitter is only propagated if it's not handled(true)
				//stopOnException is false by default, so we don't have to use the ContinueOnExceptionStrategy
				.split(exchangeProperty("cityNames")).parallelProcessing().executorServiceRef("myThreadPool")
					.setProperty("city",method(this, "newCity"))
					.to("direct:getCityZips")
				.end()
				//At this point the City objects in the list should be populated with the zip codes or errors

				//Response object
				.setBody(method(this,"createResponse"))
				.removeHeaders("*", HEADER_BUSINESSID);

		/************************
		/* Secured route with basic authentication.
		 * GET User list per role (coming from spring xml)
		 ************************/
		rest("/secure").description("Basic auth. Try name:'user' passwd:'secret'.")
		.get("/role/{roleName}").description("User list by role")
				.param().name("roleName").type(RestParamType.path).allowableValues("member","admin").dataType("string").endParam()
				.responseMessage().code(200).endResponseMessage()
			.route().routeId("secure-get")
				.log("Secure is called")
				.setBody(method("roles","get(${header.roleName})"))
				.removeHeaders("*",HEADER_BUSINESSID)
		.endRest();
		
	}
	
	//Helper methods used in these routes. It's a good idea to keep them in the RouteBuilder for readability if they are simple.

	//Build succesful response pojo
	public CitiesResponse createResponse(@ExchangeProperty("country") String country, @ExchangeProperty("cities") List<City> cities) {
		CitiesResponse citiesResponse = new CitiesResponse();
		citiesResponse.setCode(SUCC.getCode());
		citiesResponse.setMessage(SUCC.getMessage());
		citiesResponse.setCountry(country);
		citiesResponse.setCities(cities);
		return citiesResponse;
	}

	//Build error response pojo
	public static ApiResponse errorResponse(int code, String message){
		return new ApiResponse(code, message);
	}
	
	public static ApiResponse errorResponse(@ExchangeException Exception ex){
		String message;
		int code = 5000;
		if (ex instanceof BeanValidationException){
			code=5001;
			message = Optional.ofNullable(((BeanValidationException)ex).getConstraintViolations()).orElseGet(Collections::emptySet)
					.stream()
					.map((v)->"'"+v.getPropertyPath()+"' "+v.getMessage())
					.collect(Collectors.joining("; "));
		} else if (ex instanceof SoapFault) {
			//SoapFault in only thrown is http 200 was returned with soap:Fault, otherwise cxf throws the http exception
			code=5002;
			message = "Error calling soap service: "+((SoapFault)ex).getMessage();
			
		}
		else {
			message = ex.getMessage();
		}
		return new ApiResponse(code, message);
	}

	//Create an empty synchronized List<City>
	public static List<City> emptyCityList(){
		return Collections.synchronizedList(new ArrayList<City>());
	}

	//Convert xml path NodeList to List
	public static List<String> getCityNamesFromXML(@XPath("//NewDataSet/Table/City/text()") NodeList nodeList) {
		return IntStream.range(0, nodeList.getLength()).mapToObj(nodeList::item).map(n->n.getNodeValue()).collect(Collectors.toList());
	}

	//Create a new City object and add it to response list
	public static City newCity(@Body String cityName, @ExchangeProperty("cities") List<City> cities) {
		City city = new City(cityName);
		cities.add(city);
		return city;
	}
	
}
