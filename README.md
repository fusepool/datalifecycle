Data Life Cycle
============

Implements the processing chain from the RDF imports to providing the interlinked and smushed data 
into the content graph


To compile the bundle run

    mvn install

To deploy the bundle to a stanbol instance running on localhost port 8080 run

    mvn org.apache.sling:maven-sling-plugin:install


After installing a new menu item pointing you to /datalifecycle will appear.

Steps of Data LifeCycle
-----

 * A Graph for a category of data gets created
 * Data is uploaded to that graph, on uploading the interlinker adds triple to an owl:sameAs Graph
 * Every night the data graphs are smushed to the content graph
