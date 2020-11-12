[Overview](TableOfContent.md)

---
# Hard facts
* The (technical) project is organized as a Maven 3 multi-module project; using a parent-POM-approach.
* The minimum Java version is 1.8

# Project structure
```
+ <GIT>
  - pom.xml                                    Root POM with all version definitions of dependencies
  + /code-coverage-report                      Module to define content for code coverage report
  + /doc                                       Directory with documentation content
  + /jpa-tools                                 Directory with additional modules for tools around OData-JPA-Adapter
    + /java-client-api-generator-maven-plugin  Code generator module for client side java model
  + /jpa-tutorial                              Additional UML models for documentation
  + /odata-jpa
    + /odata-jpa-addons                        Directory with modules extending the functionality at runtime
      + /odata-jpa-processor-excelexport       Module implementing a server side excel report generation based on a OData query
    + /odata-jpa-annotation                    Module defining the required compile time annotations to annotate a JPA model for OData
    + /odata-jpa-metadata                      Module implementing the adapter internal mapping representation for JPA -> OData
    + /odata-jpa-test                          Module defining a test data model used for the test suite
    + /odata-jpa-processor                     The core functionality module, giving the name of the GitHub repository 
  + /odata-jpa-examples
    + /java-client-api-generator-example       Demo module to consume the code generator for client side java code
    + /olingo-generic-servlet-example          Demo servlet using the OData-JPA-Adapter and having a few integration tests, the built-in jetty will also provide the ui5-client-example
    + /ui5-client-example                      Demo UI5 app using OData 
```
