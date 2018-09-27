## Simple HelloWorld rest api using Fuse7 BOM
The projects build executebale Spring Boot fat jar and also can be run in OpenShift.

### Build and run
The Red Hat maven repos are configured in configuration/settings.xml
Run locally: 
  mvn -s configuration/settings.xml clean install
  java -jar target/fuse7-hello-1.0-SNAPSHOT.jar
Or with spring-boot-maven-plugin:
  mvn -s configuration/settings.xml spring-boot:run
Try:  
  http://localhost:8080/api/hello
  http://localhost:8080/api/hello/MyName
Deploy to minishift: mvn -s configuration/settings.xml clean install -P fabric8
  http://fuse7-hello-myproject.192.168.99.100.nip.io/api/hello

### Minishift 
Download: https://developers.redhat.com/products/cdk/overview/
  minishift config set vm-driver virtualbox
  minishift setup-cdk 
    -> This creates a ~/.minishift directory. You can always delete that to start from scratch.
  minishift start --memory 4GB
    --> The memory settings are actually fixed after the first start
  admin/admin or developer/developer are two existing users

### Documentation:
  Fuse on Openshift: https://access.redhat.com/documentation/en-us/red_hat_fuse/7.1/html-single/fuse_on_openshift_guide/
  All Fuse docs: https://access.redhat.com/documentation/en-us/red_hat_fuse/7.1/
  SpringBoot: https://docs.spring.io/spring-boot/docs/1.5.13.RELEASE/reference/htmlsingle/
  Camel Rest DSL: https://github.com/apache/camel/blob/master/camel-core/src/main/docs/rest-dsl.adoc

