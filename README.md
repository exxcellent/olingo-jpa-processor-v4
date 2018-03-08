[![Build Status](https://travis-ci.org/exxcellent/olingo-jpa-processor-v4.svg?branch=eXXcellent_adaptions)](https://travis-ci.org/exxcellent/olingo-jpa-processor-v4)

# First words...
This is a major refactoring of the content now provided via [SAP/olingo-jpa-processor-v4](https://github.com/SAP/olingo-jpa-processor-v4). The content in this fork was modified before the GitHub project provided by SAP/Olingo was created and based on a initial archive attached to an [Olingo Issue](https://issues.apache.org/jira/browse/OLINGO-1010). One of the goals of this GitHub fork is to give back most of the code contributions to the origin project.

# Generic OLingo (OData) JPA Adapter
This library implements functionality to enable CRUD operations for an JPA based data model in a OData (REST) environment.
Developers using this library have to write only a few lines of code to get a servlet running handling all typical operations to read, write/update and delete JPA entities as OData/REST resources. Additional supported out-of-the-box functionality defined by the [OData](http://www.odata.org/) standard is:
* Call Java methods like remote procedure call (RPC) as OData bound actions
* Sorting + filtering for results

# Documentation
## For integrators
More informations how to integrate the library into your Java project read [doc/integrators](doc/integrators/Intro.md).

## For developers
More informations can be found under [doc/development](doc/development/Project-Structure.md).
