[Overview](TableOfContent.md)

---
# Read entity or entity collection
A typical OData/REST call to load data for one or more reources (like _.../Persons('1')_ or _.../Persons_) will load data from the database and convert them directly into OData entities without materialization as JPA entities (Java instances).

<img src="images/sequence-retrieve-data.png" alt="Sequence diagram to show steps to retrieve data from database" height="75%" />

* Internally a tuple-query from the criteria API is used.
* Such a tuple query will convert loaded results from data base using the converters defined by attributes on the entity class (@Entity).
* These converted values are transformed one more time: into the OData representation of the entity class(es). This will trigger also one more value conversion to make the JPA related value types from the tuple query compatible to the data types supported by the Olingo library to built-up the OData entity representations.
