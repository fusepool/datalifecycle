<@namespace ont="http://fusepool.com/ontologies/interlinking#" />
<@namespace ehub="http://stanbol.apache.org/ontology/entityhub/entityhub#" />
<@namespace cc="http://creativecommons.org/ns#" />
<@namespace dcterms="http://purl.org/dc/terms/" />
<@namespace rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" />
<@namespace wf="http://example.com/ont/workflow/" />

<html>
  <head>
    <title>Data Lifecycle Admin Page</title>
    <link rel="stylesheet" href="http://localhost:8080/static/home/style/stanbol.css" />
    <link rel="icon" type="image/png" href="http://localhost:8080/static/home/images/favicon.png" />
    <link rel="stylesheet" href="http://localhost:8080/static/home/style/stanbol.css" />
    <!-- link type="text/css" rel="stylesheet" href="styles/resource-resolver.css" /-->
  </head>

  <body>
    <h1>Data Lifecycle Manager</h1>
    
    <h2>Create a new pipe to process a dataset</h2>
    <div>
    <form action="create_pipe" method="post">
        Label: <input type="text" name="pipe_label" value="" size="60"><br/>
        <input type="submit" value="Create Pipe" />        
    </form>
    </div>
    
    
    
    <h2>Pipes Management</h2>
    <div>
    <form action="operate" method="post">
        
        <div>
        <p>Select a pipe to perform any of the operations below</p>
        <select name="pipe" size="10">
           
           <@ldpath path="wf:pipe">
               <option><@ldpath path="."/></option>
           </@ldpath>
           
        </select>
        </div>
        
        <ul>
        <li><input type="radio" name="operation_code" value="1" disabled>RDFize MAREC patent
        <div>
        	<label for="url">URL of MAREC XML file to upload:</label>
        	<input type="text" name="xml_url" value="" size="70"><br>
        </div>
        </li>
        <li><input type="radio" name="operation_code" value="2" disabled>RDFize PubMed article
        <div>
        	<label for="url">URL of PubMed XML file to upload:</label>
        	<input type="text" name="xml_url" value="" size="70"><br>
        </div>
        </li>
        <li><input type="radio" name="operation_code" value="3">Add triples to the pipe's source graph
        <div>
        	<label for="url">URL of RDF data to upload:</label>
        	<input type="text" name="data_url" value="" size="70"><br>
        </div>
        </li>
        <li><input type="radio" name="operation_code" value="4">Extract text from MAREC patents</li>
        <li><input type="radio" name="operation_code" value="5">Extract text from PubMed articles</li>
        <li><input type="radio" name="operation_code" value="6">Reconcile
        <div>
        	<label for="target_recon_graph">Target graph URI:</label>
        	<input type="text" name="data_url" value="" size="70"><br>
        </div>
        </li>
        </li>
        <li><input type="radio" name="operation_code" value="7">Smush<br></li>
        
        </ul>
        
        <input type="submit" value="Apply" />
        
    </form>
    </div>
    
    <a href="/pipesadmin">Pipes graphs admin page</a>
    <#include "/html/includes/footer.ftl">
  </body>
</html>

