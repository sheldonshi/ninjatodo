ninjatodo
=========

Ninja To Do is a minimalist to do list in Java, originally ported from mytinytodo 1.4.2 in php (http://mytinytodo.net). Ninja To Do uses the Play Framework 1.2.4 (http://playframework.org)

h2. Introduction

Agile development teams need a very light-weight to do list that also offers collaboration and a mobile-friendly UI. It would be even better if it is open source. I've searched for a long time and found mytinytodo fit 70% of the bill. But it is in php. I like Java. So I decided to port it to Java, and further streamline its UI as well as enhance its collaboration capabilities. If there is any features you'd like to see based on your agile practice, please let me know.

h2. Requirement

* JDK 1.6 or later
* Play Framework 1.2.4
* MySQL 5.1 or later

h2. Installation

* Install Play Framework according to its guide
* Install and run MySQL
* Configure conf/application.conf to have the correct MySQL url and user/password 
* (Option 1) Under the root folder of the project, run "play run" in command line. By default the server listens on port 9000. To change the port consult Play documentation
* (Option 2) Under the root folder of the project, run "play war" to generate a war file. The war file can be deployed in any Java servlet container.

