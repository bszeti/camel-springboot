package my.company.route;

import com.sun.org.apache.xpath.internal.operations.Bool;
import my.company.model.CitiesResponse;
import my.company.model.City;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.message.CxfConstants;
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
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
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
			.unmarshal(df) //Unmarshall to object
			.removeHeaders("*", HEADER_BUSINESSID)
			;

		from("direct:callRestServiceHttp").routeId("callRestServiceHttp")
			.setProperty("country",header("country"))
			.removeHeaders("*", HEADER_BUSINESSID)
			.setHeader(Exchange.HTTP_PATH,simple("/country/${exchangeProperty.country}/cities"))
			.to("{{self.url}}?throwExceptionOnFailure=false") //Make the http request
			.unmarshal(df) //Unmarshall to object
			.to("log:callRestServiceHttp?showAll=true&multiline=true")
			.removeHeaders("*", HEADER_BUSINESSID)
		;

		//Uses CXF client. See cxfRsClient bean in application-context.xml
		from("direct:callRestServiceCxf").routeId("callRestServiceCxf")
			.onException(WebApplicationException.class)
				.handled(true)
				.to("log:onWebApplicationException?showAll=true&multiline=true")
				//Http response can be found in the exception
				.setBody(method(this,"handleWebApplicationException"))
				.end()
			.setProperty("country",header("country"))
			.removeHeaders("*", HEADER_BUSINESSID)
			.setHeader(CxfConstants.CAMEL_CXF_RS_USING_HTTP_API, constant(Boolean.TRUE)) //default
			.setHeader(Exchange.HTTP_METHOD,constant("GET")) //default is POST
			.setHeader(Exchange.HTTP_PATH,simple("/country/${exchangeProperty.country}/cities"))
			.setHeader(CxfConstants.CAMEL_CXF_RS_RESPONSE_CLASS,constant(CitiesResponse.class))

			//Exception is thrown if status is not 2xx and wrapped in a WebApplicationException
			//To avoid any exception being thrown:
			//.setHeader(CxfConstants.CAMEL_CXF_RS_RESPONSE_CLASS,constant(Response.class))
			//.to("cxfrs:bean:cxfRsClient?synchronous=true&throwExceptionOnFailure=false")

			.to("cxfrs:bean:cxfRsClient?synchronous=true")
			.to("log:afterCxfRsClient?showAll=true&multiline=true")
			.removeHeaders("*", HEADER_BUSINESSID);

	}

	public String handleWebApplicationException(@ExchangeException WebApplicationException ex){
		Response response = ex.getResponse();
		String answer =  "Failed to call rest service.";
		try{
			answer = MessageFormat.format("Failed to call rest service. status:{0} body:{1}",response.getStatus(), getContext().getTypeConverter().convertTo(String.class,response.getEntity()));
		} catch (Exception formatEx){
			log.warn("Failed to format error message",formatEx);
		}
		return answer;
	}


}
