package my.company.route;

import my.company.model.CitiesResponse;
import my.company.model.City;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.cloud.Http4ServiceExpression;
import org.apache.camel.http.common.HttpCommonComponent;
import org.apache.camel.language.Simple;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.spi.DataFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static my.company.route.RestEndpoints.HEADER_BUSINESSID;

@Component
public class CallRestService extends RouteBuilder {
	private static final Logger log = LoggerFactory.getLogger(CallRestService.class);

	@Autowired
	ServerProperties serverProperties;

	@Override
	public void configure() throws Exception {
		onException(Exception.class)
			.handled(true)
			.log(LoggingLevel.ERROR, "Error calling service: ${exception.message}")
			.to("log:CallRestService.error?showAll=true&multiline=true&level=ERROR")
			;

		//Create and customize JsonDataFormat
		JsonDataFormat df = new JsonDataFormat(JsonLibrary.Jackson);
		df.setUnmarshalType(CitiesResponse.class);


		from("direct:callRestServiceHttp4").routeId("callRestServiceHttp4")
			.setProperty("country",header("country"))
			.removeHeaders("*", HEADER_BUSINESSID)
			.setHeader(Exchange.HTTP_URI, simple("properties:self.url"))
			.setHeader(Exchange.HTTP_PATH,simple("/country/${exchangeProperty.country}/cities"))
			.to("http4:host?throwExceptionOnFailure=false&sslContextParameters=#sslContextParameters") //Make the http request
			.setBody(bodyAs(String.class))
			.to("log:afterCallingService?showAll=true&multiline=true")
			.unmarshal(df) //Unmarshall to object
			.to("log:callRestServiceHttp4?showAll=true&multiline=true")
			.removeHeaders("*", HEADER_BUSINESSID)
			;

		from("direct:callRestServiceHttp").routeId("callRestServiceHttp")
				.setHeader(Exchange.HTTP_PATH,simple("/country/${header.country}/cities"))
				.to("{{self.url}}?throwExceptionOnFailure=false") //Make the http request
				.unmarshal(df) //Unmarshall to object
				.to("log:callRestServiceHttp?showAll=true&multiline=true")
		;
	}
	

}
