To play around with the example, do the follwing steps:
* Open a shell in the root of github project (where the root pom is located, not in this directory)
* start jetty to run backend functionality: `mvn jetty:run -pl :olingo-generic-servlet-example -Ddisable.jetty=false -Dmaven.source.skip -Dmaven.javadoc.skip`
* Now you can execute tests in the IDE or via command line like `mvn test -pl :olingo-generic-servlet-example -Dtest=ReadEntitiesIT#testMetadataAndCacheControl`
