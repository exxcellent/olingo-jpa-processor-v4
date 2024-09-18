![Build Status (javax)](https://github.com/exxcellent/olingo-jpa-processor-v4/actions/workflows/build-jakarta.yml/badge.svg?branch=main-jakarta&event=push)
[![codecov](https://codecov.io/gh/exxcellent/olingo-jpa-processor-v4/branch/eXXcellent_adaptions/graph/badge.svg)](https://codecov.io/gh/exxcellent/olingo-jpa-processor-v4)

# First words...
This is a major refactoring of the content now provided via [SAP/olingo-jpa-processor-v4](https://github.com/SAP/olingo-jpa-processor-v4). The content in this fork was modified before the GitHub project provided by SAP was created and based on a initial archive attached to an [Olingo Issue](https://issues.apache.org/jira/browse/OLINGO-1010). Over the time the implementations of both GitHub projects became different in many parts. Most parts of the API maybe still compatible, but the supported functionality and behaviour at runtime will not.
Currently we see no chance to give back most of the code contributions to the origin project. So using one of the jpa processor forks may be a decision forever.

# Generic OData-JPA-Adapter
This library implements functionality to enable CRUD operations for an JPA based data model in a OData (REST) environment.
Developers using this library have to write only a few lines of code to get a servlet running handling all typical operations to read, write/update and delete JPA entities as [OData](http://www.odata.org/)/REST resources. Additional supported out-of-the-box functionality is:
* Call Java methods as OData actions
* Define custom OData entities outside JPA via (D)ata(T)ransfer(O)bject classes to get benefits from OData, but avoiding the persistence layer from JPA 
* Limit access to resources/actions with authorisation checks
* A few builtin automatic datatype conversions (including time types) to mediate between OData, Java and the JPA/Database
* Sorting + filtering for results

# Javax (1.x) vs Jakarta (2.x)
Currently the library exists in two kinds of versions. One is for the old `javax` namespace the other for the more modern `jakarta` namespace. The differences can be explained as follows:
* _javax_  means Java 8 (but CI is runing also for Java 11 and 17)
    * This variant is using Olingo 4.x
    * This variant will have the version range **1.x**, the older one 0.x is shared with the  _jakarta_  variant
    * The branch [main-javax](https://github.com/exxcellent/olingo-jpa-processor-v4/tree/main-javax) contains the content.
* _jakarta_  means Java 17+
    * This variant is using Olingo 5.x
    * This variant will have the version range **2.x**, the older one 0.x is shared with the  _javax_  variant
    * The branch [main-jakarta](https://github.com/exxcellent/olingo-jpa-processor-v4/tree/main-jakarta) contains the content.

The documentation is slightly different for the both variants, so read the right one!

# Documentation
## For integrators
More informations how to integrate the library into your Java project read "[Get Started](doc/integrators/GetStarted.md)". Migration requiring tasks resulting from API changes while updating are described in the [migration guide](doc/integrators/MigrationGuide.md).  
All informations are available under [doc/integrators](doc/integrators/TableOfContent.md).

## For developers
More informations can be found under [doc/development](doc/development/TableOfContent.md).
