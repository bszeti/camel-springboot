package com.mycompany;

import com.mycompany.jaxb.Order;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JaxbDataFormat;
import org.springframework.stereotype.Component;

@Component
public class Routes extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("jms:queue:{{receive.queue}}?concurrentConsumers={{receive.concurrentConsumers}}&transacted=true&cacheLevelName=CACHE_CONSUMER")
            .routeId("xml.route")

            .log(LoggingLevel.INFO,"Received - ${body}")
            // Validate XML
            .to("validator:xml/in.xsd")

            // Query by id
            .setHeader("uuid",xpath("/order/@uuid",String.class))
            .log("Order UUID: ${header.uuid}")
            .to("sql:select * from orders where uuid=:#${header.uuid}?outputHeader=orderInfo&outputType=SelectOne")
            // .to("log:xml.route?showAll=true&multiline=true")
            .log(LoggingLevel.INFO, "Query result : ${header.orderInfo[customer]} - ${header.orderInfo[address]}")

            // XSLT
            .to("xslt:xml/in.xslt")

            // Send message
            .log(LoggingLevel.INFO, "Sending - ${body}")
            .to("jms:queue:{{send.queue}}")
        ;


        JaxbDataFormat jaxb = new JaxbDataFormat();
        jaxb.setContextPath(Order.class.getPackage().getName());
        jaxb.setPrettyPrint(true);


        from("jms:queue:jaxbqueue")
            .routeId("jaxb.route")


            .to("log:jaxb.route?showBodyType=true") //BodyType: String
            .unmarshal(jaxb)
            .to("log:jaxb.route?showBodyType=true") //BodyType: com.mycompany.jaxb.Order
            .marshal(jaxb)
            .to("log:jaxb.route?showBodyType=true") //BodyType: byte[]
            .convertBodyTo(String.class)
            .to("log:jaxb.route?showBodyType=true") //BodyType: String

            // The dataformat is also automatically used for Object<->String conversions, so we actually don't need "unmarshal/marshal" steps
            .convertBodyTo(Order.class)
            .to("log:jaxb.route?showBodyType=true") //BodyType: com.mycompany.jaxb.Order
            .to("jms:queue:{{send.queue}}?jmsMessageType=Text") //Automatically converted to XML String
        ;

    }
}
