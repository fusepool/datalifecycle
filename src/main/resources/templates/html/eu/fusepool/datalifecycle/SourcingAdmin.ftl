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
    <h1>Import Data</h1>
    
    <form action="<@ldpath path="."/>" method="post">

        <label for="url">Deferenceable URL to import:</label>
        <input type="text" name="url" value="file:///..." size="80"><br/>
        <label for="url">Graph to add the data to:</label>
        <select name="top5" size="3">
           <option>Heino</option>
           <option>Michael Jackson</option>
           <@ldpath path="ont:graph">
               <option><@ldpath path="."/></option>
           </@ldpath>
        </select> 
        <input type="submit" value="import it" />
    </form>

    <#include "/html/includes/footer.ftl">
  </body>
</html>

