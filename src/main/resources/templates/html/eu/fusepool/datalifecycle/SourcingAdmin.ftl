<@namespace ont="http://fusepool.com/ontologies/interlinking#" />
<@namespace ehub="http://stanbol.apache.org/ontology/entityhub/entityhub#" />
<@namespace cc="http://creativecommons.org/ns#" />
<@namespace dcterms="http://purl.org/dc/terms/" />

<html>
  <head>
    <title>Data Lifecycle Admin Page</title>
    <link type="text/css" rel="stylesheet" href="styles/resource-resolver.css" />
  </head>

  <body>
    <h1>RDF Data Lifecycle Manager</h1>
    
    <h2>Create a new RDF graph</h2>
    <div>
    <form action="create_graph" method="post">
        Graph URI: <input type="text" name="graph" value="" size="60"> Label: <input type="text" name="graph_label" value="" size="60"><br/>
        <input type="submit" value="Create" />        
    </form>
    </div>
    
    
    
    <h2>Graphs Management</h2>
    <div>
    <form action="operate" method="post">
        
        <div>
        <p>Select a graph to which you want upload the RDF data</p>
        <select name="graph" size="10">
           
           <@ldpath path="dcterms:hasPart">
               <option><@ldpath path="."/></option>
           </@ldpath>
           
        </select>
        </div>
        
        <div>
        <input type="radio" name="operation_code" value="1">Add triples<br>
        <input type="radio" name="operation_code" value="2">Remove all triples<br>
        <input type="radio" name="operation_code" value="3">Delete<br>
        <input type="radio" name="operation_code" value="4">Reconcile<br>
        <input type="radio" name="operation_code" value="5">Smush<br>
        </div>
        
        <div>
        	<label for="url">Deferenceable URL for RDF data to upload:</label>
        	<input type="text" name="data_url" value="" size="70"><br/>
        </div>
        <input type="submit" value="Apply" />
        
    </form>
    </div>
    
    
    
    
    
    <#include "/html/includes/footer.ftl">
  </body>
</html>

