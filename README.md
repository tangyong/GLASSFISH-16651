GLASSFISH-16651
===============

The repository is for contributing GLASSFISH-16651(http://java.net/jira/browse/GLASSFISH-16651).

Since we support of wrapping of WARs with webbundle: URL scheme, we should explore the option of using it to wrap plain vanilla WARs when such WARs are deployed using --type=osgi property. This will allow OSGi Web Container to be available to a wider audience.

Currently, I have made a prototype of [GLASSFISH-16651] and if using the following way ,  plain vanilla WAR can be wraped WAB(osgi rfc#66) and can be accessed from Brawser. 

Using Way:

1 build main\appserver\extras\osgi-container using my modified files.

2 copy built osgi-container.jar  to  glassfish3\glassfish\modules  directory

3 setting the value of "glassfish.osgi.start.level.final" into 3 in glassfish3\glassfish\config\osgi.properties file.

4 executing "asadmin start-domain"

5 On the current prototype implementation, user can using the following
way to deploy a wab and launch the wab successfully:

asadmin deploy --type=osgi --properties uriScheme=webbundle:Web-ContextPath=/test_sample1:Bundle-SymbolicName=test1 e:\test_sample1.war

6 From Browser, you can access "http://localhost:8080/test_sample1/"

"Hello! Servlet3.0 Sample1111111111" 

Note:test_sample1.war is in testsample directory.

ToDo:

1) Admin Gui's Enhancement
