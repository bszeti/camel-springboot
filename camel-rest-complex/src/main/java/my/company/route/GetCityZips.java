package my.company.route;

import my.company.model.City;
import org.apache.camel.Body;
import org.apache.camel.ExchangeException;
import org.apache.camel.ExchangeProperty;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The
 */
@Component
public class GetCityZips extends RouteBuilder {
	private static final Logger log = LoggerFactory.getLogger(GetCityZips.class);

	//Stored procedure sql call template using header
	//The sql-stored endpoint has useMessageBodyForTemplate=true so we can add the schema to the template if it's configured
	//If no schema is required then using a template file as sql-stored:classpath:sql/myprocedure.sql also works
	//The template only supports ${header.foo} not all simple language expressions
	public static String SP_GETZIPS = "GETZIPS(\n" + 
			"  CHAR ${header.cityName},\n" + 
			"  OUT INTEGER STATUS,\n" + 
			"  OUT CHAR MESSAGE\n" + 
			")";
	
	@Value("${cityInfo.datasource.schema:#{null}}") //default value is null. It's required is SpEL otherwise the missing property causes an exception
	String schema;

	@Override
	public void configure() throws Exception {
		//OnExcpetion handlers apply only on the routes defined in this RouteBuilder
		onException(Exception.class)
			.handled(true)
			.log(LoggingLevel.ERROR, "Error getting city info. ${exception.message}")
			.bean(GetCityZips.class, "addErrorToCity");
		
		//A City object is expected as exchangeProperty.city having the name set already.
		//This object will be enriched by the route. This is easier than writing an AggregatorStrategy
		from("direct:getCityZips").routeId("getCityZips")

			//prepare and call stored procedure
			.removeHeaders("*", RestEndpoints.HEADER_BUSINESSID)
			.setHeader("cityName", simple("${exchangeProperty.city?.name}"))
			.setBody((constant(schema == null ? SP_GETZIPS : schema+"."+SP_GETZIPS)))
			.to("sql-stored:GetZips?useMessageBodyForTemplate=true&dataSource=#cityInfoDS")
			
			//Process stored proc response
			.choice()
				.when(simple("${body[STATUS]} == '0'"))
					.setBody(simple("${body[#result-set-1]}")) //A stored procedure may also return multiple resultsets
					.bean(GetCityZips.class, "processResultset")
				.otherwise()
					.log(LoggingLevel.WARN, "Failed to get zips for ${header.cityName}")
					.bean(GetCityZips.class, "throwStatusError")
			.end()
			;
		
	}
	
	//Route helper methods
	
	//Get values from ZIP column and store in City object
	public static void processResultset(
			@Body List<Map<String,Object>> resultset, 
			@ExchangeProperty("city") City city) {
		List<String> zips = resultset.stream()
				.map(m->(String)m.get("ZIP"))
				.collect(Collectors.toList());
		city.setZips(zips);
	}

	//Throw an exception id status is not 0
	public static void throwStatusError(
			@Simple("${body[STATUS]}") String status,
			@Simple("${body[MESSAGE]}") String message
			) throws Exception {
		throw new Exception(MessageFormat.format("Status {0} - {1}", status, message));
	}
	
	public static void addErrorToCity(@ExchangeProperty("city") City city, @ExchangeException Exception ex) {
		if (city != null)
			city.setError(ex.getMessage());
	}
}
