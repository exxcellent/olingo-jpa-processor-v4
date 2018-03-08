# Fast Build to package the artifacts
* Unit and integration tests are disabled

_<GIT>/source_: `mvn clean verify -DskipTests -Dmaven.source.skip -Dmaven.javadoc.skip [-s maven_project_settings_exxcellent.xml]`

# Debug (example) code in a running servlet container
* Jetty is used for integration tests and local debugging of developers
* You need a additional client to call something in the backend to trigger breakpoints...
    * You can execute the integration tests
    * You can use a separate REST client calling the functionality to debug
* Maven on command line, especially under Linux will try to determine the JAVA_HOME by it's own. If you have installed multiple JDK's
it will take the wrong one. So we have explicit to set a JAVA_HOME if not already predefined.
Check the Java version used by Maven via: `mvn -version`.

Start Jetty from command line in _<GIT>/source_ directory via:

```
export JAVA_HOME=/usr/lib/jvm/default-java
mvn jetty:run-war -pl :olingo-generic-servlet-example -Ddisable.jetty=false [-s maven_project_settings_exxcellent.xml]
```

# Release (+Build) artifacts to public (exxcellent) Maven repository
* Currently deploying is based on single files (artifacts) to publish
* We have to deploy multiple artifacts with a list of Maven commands
* We don't use _deploy:deploy_, because this goal involves a _install_ that we want to avoid
* You must build/package **all** the artifacts **before** deploying

_<GIT>/source_: `clean verify deploy:deploy -DskipTests [-s maven_project_settings_exxcellent.xml]`

