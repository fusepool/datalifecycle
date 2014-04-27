<@namespace ont="http://fusepool.com/ontologies/interlinking#" />
<@namespace ehub="http://stanbol.apache.org/ontology/entityhub/entityhub#" />
<@namespace cc="http://creativecommons.org/ns#" />
<@namespace dcterms="http://purl.org/dc/terms/" />
<@namespace rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" />
<@namespace rdfs="http://www.w3.org/2000/01/rdf-schema#" />
<@namespace wf="http://example.com/ont/workflow/" />
<html>
<head>
<script>
function confirmDeleteDataset(form){
var status = form.parentNode.getElementsByTagName("SPAN")[0].innerHTML;
if(status=="Published") return confirm("It will not be possible to unpublish the dataset after it is deleted");
else return true;
}
function confirmClearGraph(form){
var status = form.parentNode.getElementsByTagName("SPAN")[0].innerHTML;
if(status=="Published") return confirm("Unpublish the dataset before clearing one of its graphs.");
else return true;
}
</script>
<head>
    <title>Fusepool Data Lifecycle</title>
    <link type="text/css" rel="stylesheet" href="../sourcing/styles/common.css" />
  </head>
</head>
<body>
<div id="wrapper">
	   <div id="top">    
      <div id="title">
        
    	<h1><img src="../sourcing/images/fusepool.png" />Fusepool Data Lifecycle</h1>

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
	
	<@ldpath path="wf:pipe">
	        <div id="dataset">
			<h2>Dataset: <@ldpath path="rdfs:label"/></h2>
			<h2> Status: <span id="status"><@ldpath path="wf:status/rdfs:label"/></span> </h2>
			<table>
			<tr><th>Name</th><th>Size</th><th></th><th></th></tr>
			<@ldpath path="wf:creates/wf:deliverable">					
				<tr>
				<td><@ldpath path="."/></td>
				<td><@ldpath path="wf:size"/></td>
				<td>
					<form action="get_graph" method="get">
						<input type="submit" value="Get">	
						<input type="hidden" name="graph" value="<@ldpath path="."/>">				
					</form>
				</td>
				<td>
					<form action="clear_graph" method="post" onsubmit="return confirmClearGraph(this);">
						<input type="submit" value="Clear">
						<input type="hidden" name="graph" value="<@ldpath path="."/>">					
					</form>
				</td>
				</tr>
					
			</@ldpath>
			</table>
			
			<form action="unpublish_dataset" method="post">
				<input type="submit" value="Unpublish dataset">
				<input type="hidden" name="pipe" value="<@ldpath path="."/>">					
			</form>
			
			<form action="delete_pipe" method="post" onsubmit="return confirmDeleteDataset(this);">
				<input type="submit" value="Delete dataset">
				<input type="hidden" name="pipe" value="<@ldpath path="."/>">					
			</form>
			</div>
	</@ldpath>

	</div><!-- mainColumn end -->
    
    <div id="footer">
    
    	<#include "/html/includes/footer.ftl">
    </div><!-- footer end -->
    
    </div><!-- wrapper end -->
    
</body>
</html>