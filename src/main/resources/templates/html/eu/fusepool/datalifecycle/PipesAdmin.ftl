<@namespace wf="http://example.com/ont/workflow/" />
<html>
<head>
<title>Fusepool Pipes Admin Page</title>
<link type="text/css" rel="stylesheet" href="styles/resource-resolver.css" />
</head>
<body>

<h1>Pipes Manager</h1>
List of Pipes
<table>
<tr><th>Pipe</th><th>Task 1</th><th>Task 2</th><th>Task 3</th><th>Task 4</th></tr>
<@ldpath path="wf:pipe">
<tr><td><@ldpath path="."/></td><@ldpath path="wf:creates"><td><@ldpath path="."/></td></@ldpath></tr>
</@ldpath>
</table>
		 
<#include "/html/includes/footer.ftl">
</body>
</html>