# Fast Build to package the artifacts
* Unit and integration tests are disabled

_<GIT>/_: `mvn clean verify -DskipTests -Dmaven.source.skip -Dmaven.javadoc.skip`

# Debug (example) code in a running servlet container
* Jetty is used for integration tests and local debugging of developers
* You need a additional client to call something in the backend to trigger breakpoints...
    * You can execute the integration tests
    * You can use a separate REST client calling the functionality to debug
* Maven on command line, especially under Linux will try to determine the JAVA_HOME by it's own. If you have installed multiple JDK's
it will take the wrong one. So we have explicit to set a JAVA_HOME if not already predefined.
Check the Java version used by Maven via: `mvn -version`.

Start Jetty from command line in _<GIT>/_ directory via:

```
export JAVA_HOME=/usr/lib/jvm/default-java
mvn jetty:run-war -pl :olingo-generic-servlet-example -Ddisable.jetty=false
```

# Release (+Build) artifacts to public Maven repository
* Currently deploying is based on single files (artifacts) to publish
* We have to deploy multiple artifacts with a list of Maven commands
* You must build/package **all** the artifacts **before** deploying

_<GIT>/_: `clean verify deploy:deploy -DskipTests -Djetty.skip`

