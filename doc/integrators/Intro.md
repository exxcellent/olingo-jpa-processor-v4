# Concept
There are some assumptions to enable this JPA adapter library for your project:
* You have an already existing JPA entity model + _persistence.xml_ so the library can access an EntityManager for the related persistence unit.
* You have to annotate your entity model with additional annotations to configure the OData/REST API maintained by this library.
* This library will run as a separate servlet or can at least control incoming HTTP (servlet) requests and the response.

# Dependency handling
## 1. Configure repository to resolve artifacts
* You need to add a repository configuration to download the dependencies as part of your Maven build.
* You should have a project specific _settings.xml_.

```
<settings>
  <mirrors>
    <mirror>
      <id>eXXcellent</id>
      <name>eXXcellent internal repository</name>
      <url>http://nexus.exxcellent.de/repository/maven-public/</url>
      <mirrorOf>external:*</mirrorOf>
    </mirror>
  </mirrors>
</settings>
```

We have to differentiate two phases of application lifecycle having different requirements on dependencies:
## 2. Compile time (meaning editing source and compiling that)
* In that phase only _org.apache.olingo.jpa:odata-jpa-annotation_ is required to annotate your Java classes (JPA entities) for later use

```
<dependencies>
    <dependency>
        <groupId>org.apache.olingo.jpa</groupId>
        <artifactId>odata-jpa-annotation</artifactId>
        <version>...</version>
    </dependency>
</dependencies>
```
    
## 3. Runtime
* The target type containing all the dependencies for runtime is controlled by the integrator. The following documentation will assume a WAR as target to explain a example integration.
* You need to have the OLingo base libraries for OData protocol handling as dependecies for your WAR module.
* And you need to have the OLingo JPA libraries to make your JPA entities OData capable

```
<dependencies>
	<dependency>
		<groupId>org.apache.olingo</groupId>
		<artifactId>odata-server-api</artifactId>
		<version>...</version>
	</dependency>
	<dependency>
	    <groupId>org.apache.olingo</groupId>
	    <artifactId>odata-client-core</artifactId>
		<version>...</version>
	</dependency>

	<dependency>
		<groupId>org.apache.olingo.jpa</groupId>
		<artifactId>odata-jpa-processor</artifactId>
		<version>...</version>
	</dependency>
</dependencies>
```
---

The next steps are explained in [servlet configuration](AsWar.md)
