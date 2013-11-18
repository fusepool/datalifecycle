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
    
    <h2>Create a new RDF graph (to be published within the Patent Data Set)</h2>
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
        <p>Select a graph on which to perform any of the operations below</p>
        <select name="graph" size="10">
           
           <@ldpath path="dcterms:hasPart">
               <option><@ldpath path="."/></option>
           </@ldpath>
           
        </select>
        </div>
        
        <ul>
        <li><input type="radio" name="operation_code" value="1">Add triples<br>
        <div>
        	<label for="url">URL of RDF data to upload:</label>
        	<input type="text" name="data_url" value="" size="70"><br/>
        </div>
        </li>
        <li><input type="radio" name="operation_code" value="2">Remove all triples<br></li>
        <li><input type="radio" name="operation_code" value="3">Delete<br></li>
        <li><input type="radio" name="operation_code" value="4">Reconcile (against itself)<br></li>
        <li><input type="radio" name="operation_code" value="5">Smush (uses the equivalence sets created with the reconciliation)<br></li>
        <li><input type="radio" name="operation_code" value="6">Reconcile + smush (performs the two operations one after the other)<br>
        <li><input type="radio" name="operation_code" value="7">Reconcile (against the dataset graph)<br>
        <li><input type="radio" name="operation_code" value="8">Move to dataset graph<br>
        <div>
        	<label for="url">URL of RDF data to upload:</label>
        	<input type="text" name="data_url" value="" size="70"><br/>
        </div>
        </li>
        </div>
        
        <input type="submit" value="Apply" />
        
    </form>
    </div>
    
    
    
    
    
    <#include "/html/includes/footer.ftl">
  </body>
</html>

