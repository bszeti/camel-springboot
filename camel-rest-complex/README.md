# Rest services using Camel on Spring Boot
This example shows how to use Camel with Spring Boot focusing on the following features:
- Create REST endpoints with [Rest DSL](http://camel.apache.org/rest-dsl.html)
- Call SOAP service with [CXF](http://camel.apache.org/cxf.html)
- Call a [SQL stored procedure](http://camel.apache.org/sql-stored-procedure.html)
- [EhCache](http://camel.apache.org/ehcache.html)
- [MDC logging](http://camel.apache.org/mdc-logging.html)

Unit tests:
- Using [CamelSpringBootRunner](http://camel.apache.org/spring-testing.html)
- Add extra routes to implement SOAP endpoint with CXF
- Modifies the routes with [@UseAdviceWith](http://camel.apache.org/advicewith.html)
- [@MockEndpoints](http://camel.apache.org/maven/current/camel-core/apidocs/org/apache/camel/component/mock/MockEndpoint.html) to assert and skip external calls
- Embedded Apache Derby database with stored procedure

The solution uses Fabric8 BOM file for dependency versions. See:
- [Fabric8.io](https://fabric8.io/)
- [Fuse Integration Services](https://access.redhat.com/documentation/en-us/red_hat_jboss_middleware_for_openshift/3/html-single/red_hat_jboss_fuse_integration_services_2.0_for_openshift/)

### Building
The example can be built with Maven (see repositories in settings.xml attached).

    mvn clean install

### Running

	mvn spring-boot:run

or

    java -jar target/camel-rest-complex-1.0-SNAPSHOT.jar


### Rest API
```java
  GET http://localhost:8080/api/user/1
```
  Users are defined in resources/spring/user-list.xml. Also timestamps are returned to demonstrate marshalling format.
  A hardcoded 'JohnDoe' is returned if user is not found.

```java
POST http://localhost:8080/api/user
{
	"name": "Bob",
	"age": 2
}

POST http://localhost:8080/api/country
{
	"iso": "EN",
	"country": "England"
}
```
  Two post endpoints with different object types.
  The received objects are not stored or processed, simply a status json object with code/message shows if the objects were received succesfully.

```java
GET http://localhost:8080/api/secure/role/member
```
  URLs matching *security.basic.path* requires http basic authentication. Try "user"/"secret".

  Returns the user list for that role as defined in resources/spring/user-list.xml


```java
GET http://localhost:8080/api/country/France/cities
```
  Endpoint implementing a complex (real world like) scenario:
  - Call a third party SOAP service to get cities of a country at http://www.webservicex.net (thanks for the public WS example)
  - Call SQL stored procedure to get zip codes for cities. This requires a database running, so it only works in the unit tests using an embedded Apache Derby. Running on localhost will result an error message for each city indicating the missing database.  

Header "businessId" correlation id is accepted, forwarded and returned by all endpoints and also added to all logs for this request. A UUID is generated if it's not set.

### Swagger

Swagger document is available at
- http://localhost:8080/api/swagger
- http://localhost:8080/api/swagger/swagger.json
- http://localhost:8080/api/swagger/swagger.yaml

Swagger UI is at
- http://localhost:8080/webjars/swagger-ui/index.html?url=/api/swagger&validatorUrl=
- http://localhost:8080/swagger-ui (redirect)

### HTTPS

Start the app with HTTPS on port 8443. See files:
- run-ssl.sh: run with "ssl" spring profile
- application-sll.properties: ssl properties
- selfsigned.jks: Selfsigned private key

### Running the example in OpenShift

It is assumed that:
- OpenShift platform is already running, if not you can find details how to [Install OpenShift at your site](https://docs.openshift.com/container-platform/3.3/install_config/index.html).
- Localhost is configured for Fabric8 Maven Workflow. If not, you can follow the [Get Started Guide](https://access.redhat.com/documentation/en/red-hat-jboss-middleware-for-openshift/3/single/red-hat-jboss-fuse-integration-services-20-for-openshift/)

The example can be built and deployed on OpenShift using a profile:

    mvn clean install -P fabric8

To list all the running pods:

    oc get pods

Open OpenShift web console and Click *Open Java Console* on the pod to access hawtio. 

## Implementation details and lessons learned
### SpringBoot application
The @SpringBootApplication class is also @Configuration, so it can have @Bean methods to create beans. In some cases it may cause "infinite loop" problems in unit tests, so it's probably safer to have a separate @Configuration class (see AppConfig.java).
When using Spring XMLs and @ImportResource be careful with using "classpath:" or classpath*:" and fixed name or ant-style pattern. See [PathMatchingResourcePatternResolver](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/core/io/support/PathMatchingResourcePatternResolver.html) for details:
- classpath:my.file returns the first file from classpath, via the standard java resource lookup.
- classpath\*:my.file returns multiple files from **all** classpath roots (/classes AND /test-classes AND jars).
- classpath:dir/\*\*/my\*.file returns multiple files under the **first** classpath root having the /dir directory (/classes OR /test-classes).
- classpath\*:dir/\*\*/my\*.file returns matching files from all classpath roots.

### Camel context
A CamelContext is created automatically (by camel-spring-boot-starter) customized by camel.springboot.\* properties.
- RouteBuilder beans are picked up automatically, don't forget to add @Component to your RouteBuilder class.
- XML DSL routes can be defined in resource files /camel/\*.xml (camel.springboot.xmlRoutes).
- Rest DSL endpoints can be defined in resource files /camel-rest/\*.xml (camel.springboot.xmlRests).
- The automatically created context can be customized by adding [CamelContextConfiguration](http://static.javadoc.io/org.apache.camel/camel-spring-boot/2.19.1/org/apache/camel/spring/boot/CamelContextConfiguration.html) beans.

(!) If a CamelContext bean is created another way (e.g. in a spring XML) the autoconfiguration (and the camel.springboot.\* properties) are ignored.

### Rest DSL
First a RestConfigurationDefinition is needed in a RouteBuilder (see RestConfiguration.java).
- It sets how the rest routes bind to a webserver. In this case Spring Boot runs the webserver (we don't want to start a new one from Camel) so "servlet" component is used that requires a *CamelServlet* registered.   
The CamelServlet is autoconfigured by *camel-servlet-starter* after v2.19 (for older versions you need to register it, see Application.java)
- Customize the json (Jackson) serializers. Two instance is created, one to unmarshall incoming messages (json.in.\*) and to serialize the response object (json.out.\*).
- Enable and configure swagger, apiContextPath() is required. Make sure to set contextPath() to the CamelServlet's base url (camel.component.servlet.mapping.contextPath) and to set the empty host to make relative calls work in [the added SwaggerUI](https://medium.com/@bszeti/swagger-with-spring-boot-and-camel-ac59cca9556e).

Rest endpoints can be defined in a RestBuilder like routes starting with rest().
- The route implementation can be under a verb directly *get().route()...endRest()* or in a direct route *get().to("direct:my")*.
- Set .skipBindingOnErrorCode(false) if a json response body is required in case of errors.
- Remove received http (or all) headers before the end of the route as they can interfere with the response (e.g. Content-Length).
- At least one responseMessage()...endResponseMessage() is required to avoid problems with SwaggerUI.

The number of http requests processed parallel depends on the server's worker thread pool, see properties:
- server.undertow.worker-threads
- server.tomcat.max-threads
- server.jetty.acceptors
It's also suggested to think about the threadpool running multicast/split sub-routes. See *<camel:threadPool/>* in application.context. These should have lazy-init to show up in JMX under CamelContext.

The Rest DSL currently doesn't support automatic body or header validations, so it should be done in the route.
- [Camel bean-validator](http://camel.apache.org/bean-validation.html) can be used to verify annotated POJOs (e.g. received request body).
- For header validation a workaround is to create a POJO with fields matching the expected headers (see HeaderValidationsPojo.java). Register a DozerTypeConverter to convert Map to this POJO class and then *simple("${headers}",HeaderValidationsPojo.class)* expression works to create the object, that can be validated with bean-validator.
- The object for validation must be set as the current Exchange body for bean-validator.  
See Application.java contextConfiguration() and route "user-get".

### Datasources
One datasource can be configured in SpringBoot with the *spring.datasource.\** properties, but it doesn't work if multiple (pooled) datasources are needed (so this example shows a solution that can be used for multiple).  
Datasources could be created by a custom factory class or via code somehow mapping custom properties from the properties file. The easiest way may be to create a Properties instance with values added by @ConfigurationProperties(prefix="mydatasource") and then use this object to create the [Apache DBCP2](https://commons.apache.org/proper/commons-dbcp/) BasicDataSource instance. This way all the pool properties can be set and also the /configprops actuator endpoint shows the properties correctly (masking the password).

The *destroyMethod=""* should be set for the datasource beans to avoid warning caused by multiple close() calls by Camel and Spring context. In case of multiple datsources one must be annotated as @Primary.

### SQL stored procedure
The *sql-stored* component requires the signature of the called stored procedure (see GetCityZips.java). This template can be put directly in the endpoint url (that looks ugly), can come from a resource file or from the current message body. The resource file is probably the best solution, but the template language currently doesn't support property placeholders (for example to insert database schema), so it can't be customized from properties. The workaround here is to use *useMessageBodyForTemplate=true* and take the template from message body, that can have any custom string.

The template language only supports *${header.myheader}* expressions for IN parameters, no other "simple-like" expressions are allowed.

### CXF client
First generate java classes from the wsdl file by using the 'wsdl2java' tool coming with [Apache CXF](http://cxf.apache.org/). These classes should be added to the project (needed on the classpath) as they represent the service and its data objects.  
The easiest is to create the CXF endpoint bean in xml using the *http://camel.apache.org/schema/cxf* namespace using the @SOAPBinding interface as serviceClass. This bean should be used in the Camel CXF producer *to("cxf:bean:myCxfEndpointBean")* to make the soap call.
- By default the client makes asyncronous calls executing the request in another thread. If this is not needed because the caller thread has to wait for the response (InOut), enable *synchronous=true*, so the same thread does the CXF call.  
For async the CXF client uses a ThreadPoolExecutor with 5-25 *default-workqueue* threads and 256 max queue size with Abort policy (see [AutomaticWorkQueueImpl](https://cxf.apache.org/javadoc/latest/org/apache/cxf/workqueue/AutomaticWorkQueueImpl.html)).
- By default Java HttpURLConnection is used to make connections. It uses keep-alive for HTTP connections automatically.
- Apache HttpAsynClient can be used with CXF by adding [*cxf-rt-transports-http-hc*](http://cxf.apache.org/docs/asynchronous-client-http-transport.html), but it's used only for async reuests by default and adds an extra layer of threads to the call stack which doesn't sound ok. See application-context.xml how to enable it also for synchronous requests if it's still needed for whatever reason.

### EhCache
EhCache 3 config xml can be added and referenced in Camel Ehcache endpoint
- If key and value type other than *Object* is set in the ehcache config, it must be added to all Camel endpoints too.   

### Logging with MDC
MDC logging can be enabled in a CamelContextConfiguration and then %X{camel...} expressions can be used in SLF4J log pattern.

To customize what is put in the MDC context, implement a custom UnitOfWork. Also a custom UnitOfWorkFactory bean must be instantiated which is automatically used by the Camel context. See CustomMDCBreadCrumbIdUnitOfWork.java where camel.breadcrumbId is overwritten with a received correlation header that defaults to a UUID. Factory bean is in Application.java.

SLF4J also supports automatic regexp replace (for example to hide sensitive fields). See pattern is logback.xml, it also works of course in SpringBoot property *logging.pattern*.
