<@namespace ont="http://fusepool.com/ontologies/interlinking#" />
<@namespace cc="http://creativecommons.org/ns#" />
<@namespace dcterms="http://purl.org/dc/terms/" />
<@namespace rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" />
<@namespace rdfs="http://www.w3.org/2000/01/rdf-schema#" />
<html>
<head>
<head>
    <title>Fusepool Data Lifecycle: Task</title>
    <link type="text/css" rel="stylesheet" href="../styles/common.css" />
  </head>
</head>
<body>
   
      <div id="title">
        
    	<h1><img src="../images/fusepool.png" />Fusepool Data Lifecycle Task</h1>

      </div>
	
    Date submitted: <@ldpath path="dcterms:dateSubmitted"/><br/>
    Date started: <@ldpath path="dcterms:dateAccepted"/><br/>
    <#if evalLDPath("ont:endDate")??>
    Date ended: <@ldpath path="ont:endDate"/><br/>
    <#else>
    Task did not yet terminate, press reload for an updated status
    </#if>
    <pre>
<@ldpath path="rdfs:comment"/>
    </pre>

    <div class="dangerZone">
        <h2>Danger zone</h2>
        This cannot be undone, and there is no confirmation dialogue:
        <form action="<@ldpath path="."/>" method="post">
            <input type="hidden" name="action" value="TERMINATE" />
            <input type="submit" value="Request temination" />
        </form>
    </div>
</body>
</html>