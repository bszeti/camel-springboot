# Camel Rest DSL example with Spring-Boot

This example demonstrates how to configure Camel Rest APIs in Spring Boot via the Rest DSL.

The unit test modifies the routes inserting a mock endpoints with asserts using [@UseAdviceWith](http://camel.apache.org/advicewith.html).

### Building

The example can be built with

    mvn clean install

### Running

	mvn spring-boot:run

or

    java -jar target/camel-rest-post-1.0-SNAPSHOT.jar

### Call API
```javascript
GET http://localhost:8080/api/user/1

POST http://localhost:8080/api/user
{
	"name": "Balazs",
	"age": 1
}

POST http://localhost:8080/api/country
{
	"iso": "EN",
	"country": "England"
}

GET http://localhost:8080/api/secure
Basic auth: user/secret

```

### Swagger

Swagger document is available at
- http://localhost:8080/api/swagger
- http://localhost:8080/api/swagger/swagger.json
- http://localhost:8080/api/swagger/swagger.yaml

Swagger UI is at
- http://localhost:8080/webjars/swagger-ui/index.html?url=/api/swagger&validatorUrl=
- http://localhost:8080/swagger-ui (redirect)

### Running the example in OpenShift

It is assumed that:
- OpenShift platform is already running, if not you can find details how to [Install OpenShift at your site](https://docs.openshift.com/container-platform/3.3/install_config/index.html).
- Your system is configured for Fabric8 Maven Workflow, if not you can find a [Get Started Guide](https://access.redhat.com/documentation/en/red-hat-jboss-middleware-for-openshift/3/single/red-hat-jboss-fuse-integration-services-20-for-openshift/)

The example can be built and deployed on OpenShift using a profile:

    mvn clean install -P fabric8

To list all the running pods:

    oc get pods
