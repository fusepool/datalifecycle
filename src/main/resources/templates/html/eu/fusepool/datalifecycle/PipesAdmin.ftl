<@namespace wf="http://example.com/ont/workflow/" />
<html>
<head>
<title>Fusepool Pipes Graphs Admin Page</title>
<link rel="stylesheet" href="http://localhost:8080/static/home/style/stanbol.css" />
<link rel="icon" type="image/png" href="http://localhost:8080/static/home/images/favicon.png" />
<link rel="stylesheet" href="http://localhost:8080/static/home/style/stanbol.css" />
<!-- link type="text/css" rel="stylesheet" href="styles/resource-resolver.css" /-->
</head>
<body>

<h1>Pipes Graphs Admin Page</h1>
List of Pipes <br>

<@ldpath path="wf:pipe">
		Pipe: <@ldpath path="."/><br>
		
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
		
</@ldpath>

		 
		 
<a href="/sourcing">Data Lifecycle Manager Page</a>		 
<#include "/html/includes/footer.ftl">

</body>
</html>