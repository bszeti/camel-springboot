package com.mycompany;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class Routes extends RouteBuilder {

    @Override
    public void configure() throws Exception {

         from("jms:queue:{{receive.queue}}?concurrentConsumers={{receive.concurrentConsumers}}&transacted=true&cacheLevelName=CACHE_CONSUMER")
                 .routeId("processXML")
             .log(LoggingLevel.INFO,"Received - ${body}")
             .to("validator:xml/in.xsd")

             .setHeader("OrderUUID",xpath("/order/@uuid"))
                 .log("OrderUUID: ${header.OrderUUID}")

             .to("xslt:xml/in.xslt")

             .log(LoggingLevel.INFO, "Sending - ${body}")
             .to("jms:queue:{{send.queue}}")
         ;

    }
}
