DataLifeCycle
============

Implements the processing chain from the RDF imports to providing the interlinked and smushed data into the content graph


To compile the bundle run

    mvn install

To deploy the bundle to a stanbol instance running on localhost port 8080 run

    mvn org.apache.sling:maven-sling-plugin:install


After installing a new menu item pointing you to /datalifecycle will appear.

The example service allows to look up resources using the site-manager. The 
service can be accessed via browser as HTML or as RDF for machine clients.



