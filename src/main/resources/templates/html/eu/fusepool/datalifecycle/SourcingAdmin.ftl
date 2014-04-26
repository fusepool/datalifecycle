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
        
        <h2>Load data</h2>
        <ul>
        <li><input type="radio" name="operation_code" value="2">Load RDF data<br></li>
        <li>
        	<input type="radio" name="operation_code" value="1">Load XML data. Transform to RDF using 
        	<select name="rdfizer">
        		<@ldpath path="wf:rdfizeService">
                  <option value="<@ldpath path="rdfs:label"/>"><@ldpath path="rdfs:label"/></option>
                </@ldpath>
        	</select>
        	
        </li>
        URL: <input type="text" name="data_url" value="" size="60">
        </ul>
        
        <h2>Select a task</h2>
        <ol>
         <li>
        	<input type="radio" name="operation_code" value="3">Enhance. Select digester
        	<select name="rdfdigester">
        		<@ldpath path="wf:enhanceService">
                  <option value="<@ldpath path="rdfs:label"/>"><@ldpath path="rdfs:label"/></option>
                </@ldpath>
        	</select>
        	<br>
        </li>
        
        <li><input type="radio" name="operation_code" value="4">Interlink. Select interlinker
        	<select name="interlinker">
        		<@ldpath path="wf:interlinkService">
                  <option value="<@ldpath path="rdfs:label"/>"><@ldpath path="rdfs:label"/></option>
                </@ldpath>
        	</select>
        	<br>
        </li>
        <li><input type="radio" name="operation_code" value="5">Smush<br></li>
        <li><input type="radio" name="operation_code" value="6">Publish<br></li>
        </ol>
        
        <input type="submit" value="Run task" />
        
       </form>
        
        <form action="runsequence" method="post">
        <h2>Select a task sequence</h2>
        <ul>
         <li>
        	Load RDF or XML data, enhance and interlink.<br/>
             Select dataset 
        	<select name="pipe">
            <@ldpath path="wf:pipe">
               <option value="<@ldpath path="."/>"><@ldpath path="rdfs:label"/></option>
            </@ldpath>           
            </select><br/>
                Select RdfIzer
                <select name="rdfizer">
                <option value="none">None (data is in RDF)</option>
        		<@ldpath path="wf:rdfizeService">
                  <option value="<@ldpath path="rdfs:label"/>"><@ldpath path="rdfs:label"/></option>
                </@ldpath>
        	</select><br/>
        	Select digester
        	<select name="digester">
        		<@ldpath path="wf:enhanceService">
                  <option value="<@ldpath path="rdfs:label"/>"><@ldpath path="rdfs:label"/></option>
                </@ldpath>
        	</select><br/>
        	Select interlinker
        	<select name="interlinker">
        		<@ldpath path="wf:interlinkService">
                  <option value="<@ldpath path="rdfs:label"/>"><@ldpath path="rdfs:label"/></option>
                </@ldpath>
        	</select>
        	<br>
        </li>
        URL: <input type="text" name="data_url" value="" size="60">
        </ul>
        
        <input type="submit" value="Run sequence" />
        
    </form>

    <form action="processBatch/" method="post">
            <h2>Batch process</h2>
            Load RDF data, enhance and interlink. Select dataset 
        	<select name="dataSet">     
              <@ldpath path="wf:pipe">
                <option value="<@ldpath path="."/>"><@ldpath path="rdfs:label"/></option>
              </@ldpath>           
            </select>
            Select RdfIzer
            <select name="rdfizer">
              <option value="none">None</option>
        	  <@ldpath path="wf:rdfizeService">
                  <option value="<@ldpath path="rdfs:label"/>"><@ldpath path="rdfs:label"/></option>
              </@ldpath>
            </select>
        	Select digester
        	<select name="digester">
        	    <option value="none">None</option>
        		<@ldpath path="wf:enhanceService">
                  <option value="<@ldpath path="rdfs:label"/>"><@ldpath path="rdfs:label"/></option>
                </@ldpath>
        	</select>
        	Select interlinker
        	<select name="interlinker">
        	  <option value="none">None</option>
        	  <@ldpath path="wf:interlinkService">
                <option value="<@ldpath path="rdfs:label"/>"><@ldpath path="rdfs:label"/></option>
              </@ldpath>
        	</select> <br/> 
        	Skip previously added <input type="checkbox" name="skipPreviouslyAdded" checked="checked" value="on" /><br/>
            Recursively process "subdirectories" <input type="checkbox" name="recurse" checked="checked" value="on" /><br/>
            Stop after <input type="text" name="maxFiles" value="10" size="6"><br/>
            Index URL: <input type="text" name="url" value="" size="60">
            <input type="submit" value="Start processing" />
    </form>
    
    <#if evalLDPath("ont:activeTask")??>
    <h2>Active tasks</h2>
        <@ldpath path="ont:activeTask">
             <a href="<@ldpath path="."/>"><@ldpath path="."/></a><br/>
        </@ldpath>
    </div>
    </#if>
    
    </div><!-- mainColumn end -->
    
    <div id="footer">
    
    <#include "/html/includes/footer.ftl">
    </div><!-- footer end -->
    
    </div><!-- wrapper end -->
    
  </body>
</html>

