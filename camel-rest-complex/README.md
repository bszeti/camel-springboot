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

The solution is using Fabric8 BOM file. See:
- [Fabric8.io](https://fabric8.io/)
- [Fuse Integration Services](https://access.redhat.com/documentation/en-us/red_hat_jboss_middleware_for_openshift/3/html-single/red_hat_jboss_fuse_integration_services_2.0_for_openshift/)

### Building
The example can be built with Maven (see repositories in setting.xml attached).

    mvn clean install

### Running

	mvn spring-boot:run

or

  java -jar target/camel-rest-complex-1.0-SNAPSHOT.jar

### Rest API:
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
  With http basic authentication: user/secret

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
- Localhost is configured for Fabric8 Maven Workflow, if not you can find a [Get Started Guide](https://access.redhat.com/documentation/en/red-hat-jboss-middleware-for-openshift/3/single/red-hat-jboss-fuse-integration-services-20-for-openshift/)

The example can be built and deployed on OpenShift using a profile:

    mvn clean install -P fabric8

To list all the running pods:

    oc get pods
