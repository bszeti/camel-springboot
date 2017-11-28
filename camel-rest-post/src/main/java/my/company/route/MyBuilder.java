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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.core.JsonProcessingException;

import my.company.model.ApiResponse;
import my.company.model.CitiesResponse;
import my.company.model.CountryPojo;
import my.company.model.HeadersPojo;
import my.company.model.UserPojo;

@Component("mybuilder")
public class MyBuilder extends RouteBuilder {
	private final static Logger log = LoggerFactory.getLogger(MyBuilder.class);
	
	private final static UserPojo DUMMY_USER = new UserPojo("JohnDoe", 21);
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
			.responseMessage().code(200).responseModel(UserPojo.class).endResponseMessage() //OK
			.responseMessage().code(500).responseModel(ApiResponse.class).endResponseMessage() //Not-OK
			//route
			.route().routeId("user-get")
				.log("Get user: ${header.id}")
				.setBody().simple("${headers}",HeadersPojo.class)
				.to("bean-validator:validateHeaders") //or .validate().simple("${header.id} < 100")
				.setBody(constant(DUMMY_USER))
				.removeHeaders("*","businessId")
			.endRest()
		.post("/").type(UserPojo.class)
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
				.setProperty("country",header("country")) //Save input values needed later as ExchangeProperty
				.setBody(exchangeProperty("country")) //This is a very simple service where the request object is a String (insted of GetCitiesByCountryRequest
				
				//CXF call
				.removeHeaders("*", HEADER_BUSINESSID)
				.setHeader(CxfConstants.OPERATION_NAME,constant("GetCitiesByCountry"))
				.to("cxf:bean:cxfGlobalWeather")
				.setBody(method(MyBuilder.class,"convertNodeListToList"))
				.validate(body().isNotEqualTo(new ArrayList<String>())) //Verify that the the result is not an empty list
				.to("log:country-get-cities?showAll=true&multiline=true&level=DEBUG")
				
				//Response object
				.setBody(method(MyBuilder.class,"createResponse"))
				.removeHeaders("*", HEADER_BUSINESSID)
				
			.endRest()
		.post("/").type(CountryPojo.class)
			//swagger
			.description("Send country")
			.responseMessage().code(200).endResponseMessage()
			.route().routeId("post-country")
				.log("Country received: ${body}").id("received-country") //This step gets an id, so we can refer it in test
				.setBody(constant(null))//Don't return anything in the body, so no responseModel() or outType() is required
			.endRest()
		;
		
		rest("/secure").description("Basic auth. Try name:'user' passwd:'secret'.")
		.get().outType(ApiResponse.class)
			.route().routeId("secure-get")
			.log("Secure is called")
			.setBody(constant(SUCC))
			.removeHeaders("*",HEADER_BUSINESSID)
		.endRest();
		
	}
	
	//Helper methods used in these routes. It's a good idea to keep them in the RouteBuilder for readability if they are simple.
	//In a real world scenario the response is probably more complicated based on the current exchange
	public static CitiesResponse createResponse(@ExchangeProperty("country") String country, @Body List<String> cities) {
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
		if (ex instanceof BeanValidationException){
			message = Optional.ofNullable(((BeanValidationException)ex).getConstraintViolations()).orElseGet(Collections::emptySet)
					.stream()
					.map((v)->"'"+v.getPropertyPath()+"' "+v.getMessage())
					.collect(Collectors.joining("; "));
		} else {
			message = ex.getMessage();
		}
		return new ApiResponse(5000, message);
	}
	
	//Convert xml path NodeList to List
	public static List<String> convertNodeListToList(@XPath("//NewDataSet/Table/City/text()") NodeList nodeList) {
		return IntStream.range(0, nodeList.getLength()).mapToObj(nodeList::item).map(n->n.getNodeValue()).collect(Collectors.toList());
	}
	
}
