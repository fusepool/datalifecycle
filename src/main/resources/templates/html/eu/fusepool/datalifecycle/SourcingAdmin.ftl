<@namespace ont="http://fusepool.com/ontologies/interlinking#" />
<@namespace ehub="http://stanbol.apache.org/ontology/entityhub/entityhub#" />
<@namespace cc="http://creativecommons.org/ns#" />
<@namespace dcterms="http://purl.org/dc/terms/" />
<@namespace rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" />
<@namespace wf="http://example.com/ont/workflow/" />

<html>
  <head>
    <title>Data Lifecycle Admin Page</title>
    <link type="text/css" rel="stylesheet" href="styles/resource-resolver.css" />
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
        <li><input type="radio" name="operation_code" value="12" disabled>RDFize<br>
        <div>
        	<label for="url">URL of XML file to upload:</label>
        	<input type="text" name="xml_url" value="" size="70"><br/>
        </div>
        </li>
        <li><input type="radio" name="operation_code" value="1">Add triples to the pipe's source graph<br>
        <div>
        	<label for="url">URL of RDF data to upload:</label>
        	<input type="text" name="data_url" value="" size="70"><br/>
        </div>
        </li>
        <li><input type="radio" name="operation_code" value="9">Extract Text (from patent docs)<br></li>
        <li><input type="radio" name="operation_code" value="10">Extract Text from articles only for test<br></li>
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

