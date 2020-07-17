[Overview](TableOfContent.md)

---
# Hard facts
* The (technical) project is organized as a Maven 3 multi-module project; using a parent-POM-approach.
* The minimum Java version is 1.8

# Project structure
```
+ <root>
  - pom.xml                                   Root POM with all version definitions of dependencies
  + /code-coverage-report						Module to define content for code coverage report
  + /doc											Directory with documentation content
  + /jpa-tools										Directory with additional modules
    + /java-client-api-generator-maven-plugin	Code generator module for client side java model
  + /jpa-tutorial									Additional UML models for documentation
  + /odata-jpa
  + /odata-jpa-examples
    + /java-client-api-generator-example		Demo module to consume the code generator for client side jav code
    + /olingo-generic-servlet-example			Demo servlet using the OData-JPA-Adapter and having a few integration tests
    + /ui5-client-example						Demo UI5 app using OData 
```
