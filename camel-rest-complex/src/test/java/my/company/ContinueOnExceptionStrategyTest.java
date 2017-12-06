package my.company;

import my.company.utils.ContinueOnExceptionStrategy;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.util.toolbox.FlexibleAggregationStrategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * Test example based on CamelTestSupport, it doesn't start the SpringBoot application only a CamelContext with the RouteBuilder defined here.
 * Having CamelTestSupport tests and CamelSpringBootRunner tests in the same project can cause problems because CamelSpringBootRunner disables CamelContext startup during init. 
 * So if the CamelTestSupport test is run before the CamelSpringBootRunner, the context won't start. 
 * Use maven with maven-surefire-plugin v2.19.1+ or reuseForks=false to avoid this problem. It still can happen in Eclipse running tests for example.
 */
@RunWith(BlockJUnit4ClassRunner.class) //BlockJUnit4ClassRunner is actually the default Junit4 runner
public class ContinueOnExceptionStrategyTest extends CamelTestSupport {
	private static final Logger log = LoggerFactory.getLogger(ContinueOnExceptionStrategyTest.class);

	@Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            public void configure() {
            	AggregationStrategy aggregationStrategy = 
            			//Join message bodies with ":" delimiter
            			new FlexibleAggregationStrategy<>()
            			.pick(bodyAs(String.class)) //If cast to given fails, null is returned which is simply not collected
            			.accumulateInCollection(ArrayList.class)
            			.storeInBody()
            			.completionAware((exchange) -> {
            				exchange.getIn().setBody(String.join(":", exchange.getIn().getBody(List.class)));
            				}
    					);
            	
            	
            	AggregationStrategy continueOnExceptionStrategy = new ContinueOnExceptionStrategy(aggregationStrategy);
            	
            	//Exception handling for both routes
            	//The goal is to stop execution in the splitter in case of error, but continue with the whole route 
            	onException(Exception.class)
	            	.handled(true)
	            	.setBody(simple("Error for ${body}"));
            
            	//The route stops after the splitter because the exchange returned by the FlexibleAggregationStrategy is a handled error exchange
            	//By default stopOnAggregateException is not enabled
                from("direct:stopAfterError")
                .split(body().tokenize(",")).parallelProcessing().aggregationStrategy(aggregationStrategy)
                	.to("mock:splitterStartStopAfterError")
                	.log("stopAfterError: ${body}")
                	.throwException(new Exception("Error in splitter"))
                	.to("mock:splitterEndStopAfterError")
            	.end()
            	.log("resultStopAfterError: ${body}")
                .to("mock:resultStopAfterError");
                
                //The route continues after the splitter because the ContinueOnExceptionStrategy "cleans" the exchange from the remains of the exception
                from("direct:continueAfterError")
                .split(body().tokenize(",")).parallelProcessing().aggregationStrategy(continueOnExceptionStrategy)
                	.to("mock:splitterStartContinueAfterError")
                	.log("continueAfterError: ${body}")
                	.throwException(new Exception("Error in splitter"))
                	.to("mock:splitterEndContinueAfterError")
            	.end()
            	.log("resultContinueAfterError: ${body}")
                .to("mock:resultContinueAfterError");
            }
        };
        
    }

	@Test
	public void stopAfterError() {
		String response = (String) template.requestBody("direct:stopAfterError", "a,b,c");
		log.info("Response stopAfterError: {}",response);
		assertEquals("Error for a:Error for b:Error for c", response);
		assertEquals(3,getMockEndpoint("mock:splitterStartStopAfterError").getReceivedExchanges().size());
		assertEquals(0,getMockEndpoint("mock:splitterEndStopAfterError").getReceivedExchanges().size());
		assertEquals(0,getMockEndpoint("mock:resultStopAfterError").getReceivedExchanges().size()); //The execution in the main route stops
	}

	@Test
	public void continueAfterError() {
		String response = (String) template.requestBody("direct:continueAfterError", "a,b,c");
		log.info("Response continueAfterError: {}",response);
		assertEquals("Error for a:Error for b:Error for c", response);
		assertEquals(3,getMockEndpoint("mock:splitterStartContinueAfterError").getReceivedExchanges().size());
		assertEquals(0,getMockEndpoint("mock:splitterEndContinueAfterError").getReceivedExchanges().size());
		assertEquals(1,getMockEndpoint("mock:resultContinueAfterError").getReceivedExchanges().size()); //The execution in the main route continues
	}
}
