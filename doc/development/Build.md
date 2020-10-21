[Overview](TableOfContent.md)

---
# Fast Build to package the artifacts
* Unit and integration tests are disabled
   _<GIT>/_ : `mvn clean verify -DskipTests -Dmaven.source.skip -Dmaven.javadoc.skip`

# Release (+Build) artifacts to public Maven repository
* Currently deploying is based on single files (artifacts) to publish
* We have to deploy multiple artifacts with a list of Maven commands
* You must build/package **all** the artifacts **before** deploying

_<GIT>/_ : `clean verify deploy:deploy -DskipTests -Djetty.skip`

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

# <a id="ui5Demo"></a>Play with the UI5 demo app
The content of the UI5 demo app (contained in module  _ui5-client-example_ ) is also included in the Jetty defined in module _olingo-generic-servlet-example_ . So you can simply start that Jetty to have an running OData endpoint and a HTTP server hosting the UI5 demo app. Follow the steps:
1. Do the same as for 'fast build' to bundle all the required artifacts 
    _<GIT>/_ : `mvn clean install -DskipTests -Dmaven.source.skip -Dmaven.javadoc.skip`
    
   Within Eclipse you can replace `install` with `package` if workspace resolving is enabled.
1. Start Jetty
    _<GIT>/_ : `mvn jetty:run -pl :olingo-generic-servlet-example -Ddisable.jetty=false`
1. Open a browser to start the UI5 app with the url
    _http://localhost:8080/#/persons_
    