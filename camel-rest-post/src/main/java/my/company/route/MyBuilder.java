package my.company.route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.ws.rs.core.MediaType;

import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangeException;
import org.apache.camel.ExchangeProperty;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.bean.validator.BeanValidationException;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.camel.language.XPath;
import org.apache.camel.model.rest.RestParamType;
import org.apache.cxf.binding.soap.SoapFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.core.JsonProcessingException;

import my.company.model.ApiResponse;
import my.company.model.CitiesResponse;
import my.company.model.City;
import my.company.model.CountryApiPojo;
import my.company.model.HeaderValidationsPojo;
import my.company.model.UserApiPojo;
import my.company.utils.ContinueOnExceptionStrategy;

@Component("mybuilder")
public class MyBuilder extends RouteBuilder {
	private final static Logger log = LoggerFactory.getLogger(MyBuilder.class);
	
	private final static UserApiPojo DUMMY_USER = new UserApiPojo("JohnDoe", 21);
	private final static ApiResponse SUCC = new ApiResponse(0,"OK");
	
	public final static String HEADER_BUSINESSID = "businessId";
		
	@Override
	public void configure() throws Exception {
		/************************
		 * common exception handlers for all routes defined in this RouteBuilder
		 ************************/
		onException(JsonProcessingException.class)
			.handled(true)
			.to("log:"+MyBuilder.class.getName()+"?showAll=true&multiline=true&level=ERROR")
			.removeHeaders("*",HEADER_BUSINESSID) //don't let message headers get inserted in the http response
			.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
			.bean("mybuilder","errorResponse(4000,'Invalid json content')");
			
		onException(Exception.class)
			.handled(true)
			.to("log:"+MyBuilder.class.getName()+"?showAll=true&multiline=true&level=ERROR")
			.removeHeaders("*",HEADER_BUSINESSID) //don't let message headers get inserted in the http response
			.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
			.bean("mybuilder","errorResponse(*)");
		
		/************************
		 * Rest endpoints. Multiple can be defined (in multiple RouteBuilder), but should map different URL path
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
				.log("Get user: ${header.id}")
				.setBody().simple("${headers}",HeaderValidationsPojo.class)
				.to("bean-validator:validateHeaders") //or .validate().simple("${header.id} < 100")
				.setBody(constant(DUMMY_USER))
				.removeHeaders("*","businessId")
			.endRest()
		.post("/").type(UserApiPojo.class)
			//swagger
			.description("Send user")
			.param().name(HEADER_BUSINESSID).type(RestParamType.header).description("Business transactionid. Defaults to a random uuid").dataType("string").endParam()
			.responseMessage().code(200).responseModel(ApiResponse.class).endResponseMessage() //OK
			.responseMessage().code(400).responseModel(ApiResponse.class).message("Unexpected body").endResponseMessage() //Wrong input
			.responseMessage().code(500).responseModel(ApiResponse.class).endResponseMessage() //Not-OK
			//route 
			.route().routeId("post-user")
				.log("User received: ${body}").id("received-user") //This step gets an id, so we can refer it in test
				.setBody(constant(SUCC))
				.removeHeaders("*",HEADER_BUSINESSID)
			.endRest();
		
		//Another rest dsl
		rest("/country").description("Country API")
			.skipBindingOnErrorCode(false)
		.get("/{country}/cities")
			.description("Get cities of country by calling s SOAP service")
			.responseMessage().code(200).responseModel(CitiesResponse.class).endResponseMessage() //OK
			.responseMessage().code(500).responseModel(ApiResponse.class).endResponseMessage() //Not-OK
			.route().routeId("country-get-cities")
				.log("Getting cities for ${header.country}")
				
				//Save input headers/values needed later as ExchangeProperty
				.setProperty("country",header("country"))
				
				//Get cities for a country
				//Call SOAP servce with CXF
				.setBody(exchangeProperty("country")) //This is a very simple service where the request object is a String (insted of GetCitiesByCountryRequest
				.removeHeaders("*", HEADER_BUSINESSID)
				.setHeader(CxfConstants.OPERATION_NAME,constant("GetCitiesByCountry"))
				.to("cxf:bean:cxfGlobalWeather?synchronous=true") //Use synchronous to use the same thread to make the http call
				.setBody(method(MyBuilder.class,"convertNodeListToList"))
				.validate(body().isNotEqualTo(new ArrayList<String>())) //Verify that the the result is not an empty list
				.to("log:country-get-cities?showAll=true&multiline=true&level=DEBUG")
				.setProperty("cityNames",body())
				
				//Lookup some information (zip codes) from a database for each city
				.setProperty("cities",method(MyBuilder.class,"emptyCityList"))
				
				//Splitter gives back the original Exchange if no aggregationStrategy is set (multicast gives back the last Exchange!),
				//an exception inside the splitter is only propagated if not handled(true) 
				//stopOnException is false by default, so we don't have to use the ContinueOnExceptionStrategy
				.split(exchangeProperty("cityNames")).parallelProcessing().executorServiceRef("myThreadPool")
					.setProperty("city",method(MyBuilder.class, "newCity"))
					.to("direct:getCityInfo")
				.end()
				//At this point the City objects in the list should be populated with the zip codes or errors
				
				//Response object
				.setBody(method(MyBuilder.class,"createResponse"))
				.removeHeaders("*", HEADER_BUSINESSID)
			.endRest()
		.post("/").type(CountryApiPojo.class)
			//swagger
			.description("Send country")
			.responseMessage().code(200).endResponseMessage()
			.route().routeId("post-country")
				.log("Country received: ${body}").id("received-country") //This step gets an id, so we can refer it in test
				.setBody(constant(null))//Don't return anything in the body, so no responseModel() or outType() is required
			.endRest()
		;
		
		//Secured with basic authentication
		rest("/secure").description("Basic auth. Try name:'user' passwd:'secret'.")
		.get().outType(ApiResponse.class)
			.route().routeId("secure-get")
			.log("Secure is called")
			.setBody(constant(SUCC))
			.removeHeaders("*",HEADER_BUSINESSID)
		.endRest();
		
	}
	
	//Helper methods used in these routes. It's a good idea to keep them in the RouteBuilder for readability if they are simple.
	public static CitiesResponse createResponse(@ExchangeProperty("country") String country, @ExchangeProperty("cities") List<City> cities) {
		CitiesResponse citiesResponse = new CitiesResponse();
		citiesResponse.setCode(SUCC.getCode());
		citiesResponse.setMessage(SUCC.getMessage());
		citiesResponse.setCountry(country);
		citiesResponse.setCities(cities);
		return citiesResponse;
	}
	
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
	
	//Convert xml path NodeList to List
	public static List<String> convertNodeListToList(@XPath("//NewDataSet/Table/City/text()") NodeList nodeList) {
		return IntStream.range(0, nodeList.getLength()).mapToObj(nodeList::item).map(n->n.getNodeValue()).collect(Collectors.toList());
	}
	
	//Create an empty synchronized List<City>
	public static List<City> emptyCityList(){
		return Collections.synchronizedList(new ArrayList<City>());
	}
	
	//Create a new City object and add it to response list
	public static City newCity(@Body String cityName, @ExchangeProperty("cities") List<City> cities) {
		City city = new City(cityName);
		cities.add(city);
		return city;
	}
	
}
