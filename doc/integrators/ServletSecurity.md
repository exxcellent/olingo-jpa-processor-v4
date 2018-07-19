# 1. Container managed security
You can annotate the servlet with `@ServletSecurity` to define global roles for GET, POST and other request types. This is enough to separate users by read and write access.
# 2. Self managed security
Often the developer has to separate the access to different types of data (resources) or not every user may call every OData action...

Absicherung von:
- Entitäten anlegen, löschen, modifizieren
- Functions/Actions aufrufen
via:
* Authentifizierung in Servlets ermöglichen (gibt es andere serverseitige Aufruf/Betriebsszenarien als Servlets?)
* Authorisierung ermöglichen (gleicher Schritt wie Authentifizierung im Kontext einer Ressource?)
* wie kann die Security von java.rs.ws nachgebildet werden?
* muss der Response eingeschränkt werden (nicht alle Entitäten zurückliefern) können?
* müssen Relationships zwischen Entitäten abgesichert werden oder reicht die Einstiegsentität?
-> es wird angenommen, dass nur der Request überprüft werden muss