[Overview](TableOfContent.md)

---
# What data conversions exists while working with OData-JPA-Adapter?
<style>table.conversions-table, tr, td, th {border: 1px solid grey; border-collapse: collapse; border-spacing: 0px; padding: 5px; }</style>
<table class="conversions-table">
	<tr valign="top" align="center"><th>Operation</th><th>Aspect</th><th>Conversion</th></tr>
	<tr valign="top"><td>Call bound action (Additional to unbound aspects)</td><td>Loading of scope (entity to work on)</td><td>[Database-Query] &#8605; <b>&lt;Tuple&gt;</b> &#8631; <i>(Processor)</i> &#8631; <b>&lt;OData-Entity&gt;</b> &#8631; <i>(lookup)</i> &#8631; <b>&lt;JPA-Entity&gt;</b> &#8605; [Java-Code]</td></tr>
	<tr valign="top"><td rowspan="2">Call unbound action</td><td>Parameters via Request body</td><td>[Request] &#8605; <b>&lt;JSON/XML&gt;</b> &#8631; <i>(Deserializer)</i> &#8631; <b>&lt;OData-Entity&gt;</b> &#8631; <i>(Processor)</i> &#8631; <b>&lt;JPA-Entity&gt;</b> &#8605; [Java-Code]</td></tr>
	<tr valign="top"><td>Result via response body</td><td>[Java-Code] &#8605; <b>&lt;JPA-Entity&gt;</b> &#8631; <i>(Processor)</i> &#8631; <b>&lt;OData-Entity&gt;</b> &#8631; <i>(Serializer)</i> &#8631; <b>&lt;JSON/XML&gt;</b> &#8605; [Response]</td></tr>
	<tr valign="top"><td>GET (Load) entity/entities</td><td>Load from database</td><td>[Database-Query] &#8605; <b>&lt;Tuple&gt;</b> &#8631; <i>(Processor)</i> &#8631; <b>&lt;OData-Entity&gt;</b> &#8631; <i>(Serializer)</i> &#8631; <b>&lt;JSON/XML&gt;</b> &#8605; [Response]</td></tr>	
	<tr valign="top"><td>POST (Create) entity</td><td>Store new entity to database</td><td>[Request] &#8605; <b>&lt;JSON/XML&gt;</b> &#8631; <i>(Deserializer)</i> &#8631; <b>&lt;OData-Entity&gt;</b> &#8631; <i>(Processor)</i> &#8631; <b>&lt;JPA-Entity&gt;</b> &#8631; <i>(Processor)</i> &#8631; <b>&lt;OData-Entity&gt;</b> &#8631; <i>(Serializer)</i> &#8631; <b>&lt;JSON/XML&gt;</b> &#8605; [Response]</td></tr>
</table>
