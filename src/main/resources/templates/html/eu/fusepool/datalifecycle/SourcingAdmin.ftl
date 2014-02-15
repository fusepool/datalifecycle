<@namespace ont="http://fusepool.com/ontologies/interlinking#" />
<@namespace ehub="http://stanbol.apache.org/ontology/entityhub/entityhub#" />
<@namespace cc="http://creativecommons.org/ns#" />
<@namespace dcterms="http://purl.org/dc/terms/" />
<@namespace rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" />
<@namespace rdfs="http://www.w3.org/2000/01/rdf-schema#" />
<@namespace wf="http://example.com/ont/workflow/" />

<html>
  <head>
    <title>Fusepool Data Lifecycle</title>
    <link type="text/css" rel="stylesheet" href="styles/common.css" />
  </head>

  <body>
    <div id="wrapper">
	   <div id="top">    
      <div id="title">
        
    	<h1><img src="images/fusepool.png" />Fusepool Data Lifecycle</h1>

      </div>
	
      <div class="menu">
        <ul>
          <li><a href="/sourcing">Datasets</a></li>

          <li><a href="/pipesadmin">Graphs</a></li>
          
           <li><a href="../sourcing/html/howto.html">?</a></li>

         </ul>
      </div>
      
    </div><!-- top ends here -->
    
    <div id="mainColumn">
    
    <p><@ldpath path="rdfs:comment"/></p>
    
    <div>
    <h2>Create a new dataset</h2>
    <form action="create_pipe" method="post">
        Label: <input type="text" name="pipe_label" value="" size="60"><br/>
        <input type="submit" value="Create Dataset" />        
    </form>
    </div>
    
    <div>
    
    <form action="operate" method="post">
        <h2>Select a dataset</h2>    
        <select name="pipe">
           <@ldpath path="wf:pipe">
               <option value="<@ldpath path="."/>"><@ldpath path="rdfs:label"/></option>
           </@ldpath>           
        </select>
        
        <h2>Select a task</h2>
        <ol>
        <li>
        	<input type="radio" name="operation_code" value="1">RDFize
        	<select name="rdfizer">
        		<option value="patent">MAREC patent</option>
        		<option value="pubmed">PubMed article</option>
        	</select>
        	<br>
        	URL of XML file: <input type="text" name="xml_url" value="" size="70">
        	
        </li>
        <li><input type="radio" name="operation_code" value="2">Add triples from URL<br>
        	<input type="text" name="data_url" value="" size="70">
        </li>
        
         <li>
        	<input type="radio" name="operation_code" value="3">Extract text. Select digester
        	<select name="rdfdigester">
        		<@ldpath path="wf:service">
                  <option value="<@ldpath path="rdfs:label"/>"><@ldpath path="rdfs:label"/></option>
                </@ldpath>
        	</select>
        	<br>
        </li>
        
        <li><input type="radio" name="operation_code" value="4">Reconcile<br>
        	<label for="target_recon_graph">Target graph URI:</label>
        	<input type="text" name="data_url" value="" size="70">
        </li>
        </li>
        <li><input type="radio" name="operation_code" value="5">Smush<br></li>
        <li><input type="radio" name="operation_code" value="6">Publish<br></li>
        </ol>
        
        <input type="submit" value="Apply" />
        
    </form>
    </div>
    
   
    </div><!-- mainColumn end -->
    
    <div id="footer">
    
    <#include "/html/includes/footer.ftl">
    </div><!-- footer end -->
    
    </div><!-- wrapper end -->
    
  </body>
</html>

