GLASSFISH-16651
===============

The repository is for contributing GLASSFISH-16651(http://java.net/jira/browse/GLASSFISH-16651).

Since we support of wrapping of WARs with webbundle: URL scheme, we should explore the option of using it to wrap plain vanilla WARs when such WARs are deployed using --type=osgi property. This will allow OSGi Web Container to be available to a wider audience.

Currently, I have made a prototype of [GLASSFISH-16651] and if using the following way ,  plain vanilla WAR can be wraped WAB(osgi rfc#66) and can be accessed from Brawser. 

Using Way:

1 build fighterfish\module\osgi-javaee-base and osgi-web-container 
Note: because currently, fighterfish project is not integrated in GFv4's main, you must pay attention to some settings in pom files.

2 build main\appserver\extras\osgi-container using my modified files.

3 copy built osgi-javaee-base.jar and osgi-web-container.jar to glassfish3\glassfish\modules\autostart directory

4 copy built osgi-container.jar  to  glassfish3\glassfish\modules  directory

5 setting the value of "glassfish.osgi.start.level.final" into 3 in glassfish3\glassfish\config\osgi.properties file.

6 executing "asadmin start-domain"

7 On the current prototype implementation, user can using the following
way to deploy a wab and launch the wab successfully:

1) asadmin deploy --type=osgi --properties uriScheme=wab:Web-ContextPath=/test_sample1 e:\test_sample1.war

2) asadmin deploy --type=osgi --properties uriScheme=wab:Web-ContextPath=/test_sample1:Bundle-SymbolicName=test1 e:\test_sample1.war

8 From Browser, you can access "http://localhost:8080/test_sample1/"

"Hello! Servlet3.0 Sample1111111111" 

Note:test_sample1.war is in testsample directory.

ToDo:

1) Wish Admin Team to support the way of [queryParams="Web-ContextPath=/test_sample1"]
2) Admin Gui's Enhancement
