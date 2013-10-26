<@namespace ont="http://fusepool.com/ontologies/interlinking#" />
<@namespace ehub="http://stanbol.apache.org/ontology/entityhub/entityhub#" />
<@namespace cc="http://creativecommons.org/ns#" />
<@namespace dct="http://purl.org/dc/terms/" />

<html>
  <head>
    <title>Import Data</title>
    <link type="text/css" rel="stylesheet" href="styles/resource-resolver.css" />
  </head>

  <body>
    <h1>RDF Data Lifecycle Manager</h1>
    <h2>Add RDF data to a graph</h2>
    <div>
    <form action="upload">
        <div>
        	<label for="url">Deferenceable URL for RDF data to upload:</label>
        	<input type="text" name="url" value="" size="80"><br/>
        </div>
        <div>
        <p>Select a graph to which you want upload the RDF data</p>
        <select name="graph" size="10">
           
           <@ldpath path="ont:graph">
               <option><@ldpath path="."/></option>
           </@ldpath>
           
        </select>
        <input type="submit" value="Upload" />
        </div> 
        
    </form>
    </div>
    
    <h2>Create a new RDF graph</h2>
    <div>
    <form action="addgraph">
        <input type="text" name="graph" value="" size="80"><br/>
        <input type="submit" value="Create" />
        
    </form>
    </div>
    
    <#include "/html/includes/footer.ftl">
  </body>
</html>

