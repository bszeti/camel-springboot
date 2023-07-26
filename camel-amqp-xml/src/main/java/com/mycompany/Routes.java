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
             // Validate XML
             .to("validator:xml/in.xsd")

             // Query by id
             .setHeader("uuid",xpath("/order/@uuid",String.class))
             .log("Order UUID: ${header.uuid}")
             .to("sql:select * from orders where uuid=:#${header.uuid}?outputHeader=orderInfo&outputType=SelectOne")
             // .to("log:debugexchange?showAll=true&multiline=true")
             .log(LoggingLevel.INFO, "Query result : ${header.orderInfo[customer]} - ${header.orderInfo[address]}")

             // XSLT
             .to("xslt:xml/in.xslt")

             // Send message
             .log(LoggingLevel.INFO, "Sending - ${body}")
             .to("jms:queue:{{send.queue}}")
         ;

    }
}
