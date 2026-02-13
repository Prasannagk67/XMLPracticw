package com.pb.xmlpracticecbs.route;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class GetBalance extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        onException(Exception.class)
                .setBody(simple("error occured"));

        // ✅ REST Configuration using Jetty
        restConfiguration()
                .component("jetty")
                .host("0.0.0.0")
                .port(Integer.parseInt(System.getenv().getOrDefault("PORT", "9091")));

        // ✅ REST Endpoint
        rest("/camel/api/cbs/getBalance")
                .post()
                .consumes("application/xml")
                .produces("application/xml")
                .to("direct:getBalanceRoute");

        // ✅ Process and Validate Mobile Number
        from("direct:getBalanceRoute")
                .log("CBS received request: ${body}")
                .convertBodyTo(String.class)

                // Extract values from XML
                .setProperty("messageId", simple("${body.replaceAll('(?s).*<messageId>(.*?)</messageId>.*', '$1')}"))
                .setProperty("name", simple("${body.replaceAll('(?s).*<name>(.*?)</name>.*', '$1')}"))
                .setProperty("mobileNumber", simple("${body.replaceAll('(?s).*<mobileNumber>(.*?)</mobileNumber>.*', '$1')}"))

                .log("Extracted - MessageId: ${exchangeProperty.messageId}, Name: ${exchangeProperty.name}, Mobile: ${exchangeProperty.mobileNumber}")

                // Validate mobile number length
                .choice()
                .when(simple("${exchangeProperty.mobileNumber.length} == 10"))
                // Valid mobile number
                .setBody(simple(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<response>\n" +
                                "    <messageId>${exchangeProperty.messageId}</messageId>\n" +
                                "    <name>${exchangeProperty.name}</name>\n" +
                                "    <mobileNumber>${exchangeProperty.mobileNumber}</mobileNumber>\n" +
                                "    <status>SUCCESS</status>\n" +
                                "    <message>Valid mobile number</message>\n" +
                                "</response>"
                ))
                .otherwise()
                // Invalid mobile number
                .setBody(simple(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<response>\n" +
                                "    <messageId>${exchangeProperty.messageId}</messageId>\n" +
                                "    <name>${exchangeProperty.name}</name>\n" +
                                "    <mobileNumber>${exchangeProperty.mobileNumber}</mobileNumber>\n" +
                                "    <status>FAILED</status>\n" +
                                "    <message>Invalid mobile number - must be 10 digits</message>\n" +
                                "</response>"
                ))
                .end()

                .convertBodyTo(String.class)

                .setBody(simple("${body}"))

                .log("CBS sending response: ${body}")
                .setHeader("Content-Type", constant("application/xml"));
    }
}