<@namespace wf="http://example.com/ont/workflow/" />
<html>
<head>
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

         </ul>
      </div>
      
    </div><!-- top ends here -->
    <div id="mainColumn">

	
	<@ldpath path="wf:pipe">
			<h2>Dataset <@ldpath path="."/></h2>
			
			<table>
			<@ldpath path="wf:creates/wf:deliverable">
					
				<tr>
				<td><@ldpath path="."/></td>
				<td>
					<form action="get_graph" method="get">
						<input type="submit" value="Get">	
						<input type="hidden" name="graph" value="<@ldpath path="."/>">				
					</form>
				</td>
				<td>
					<form action="empty_graph" method="post">
						<input type="submit" value="Empty">
						<input type="hidden" name="graph" value="<@ldpath path="."/>">					
					</form>
				</td>
				</tr>
					
			</@ldpath>
			</table>
			<form action="delete_pipe" method="post">
				<input type="submit" value="Delete Dataset">
				<input type="hidden" name="pipe" value="<@ldpath path="."/>">					
			</form>
			
	</@ldpath>

	</div><!-- mainColumn end -->
    
    <div id="footer">
    
    	<#include "/html/includes/footer.ftl">
    </div><!-- footer end -->
    
    </div><!-- wrapper end -->
    
</body>
</html>