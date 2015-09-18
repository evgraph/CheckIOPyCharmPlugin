# CheckIOPyCharmPlugin

**Build and run instructions**

1 Create new Idea project by cloning this repository. 

2 Add OAuth properties file with clientId and clientSecret from CheckIO.

- In order to create new CheckIO project you should authorize. For correct authorization you should add _ourProperties.properties_ file to resources/properties folder. This file contains "clientId" and "clientSecret" fields. Here is sample property file:
__
clientId= yourClientId
clientSecret= yourClientSecret

2 Install Java 8

* On Windows and Linux, install the latest [JRE 8] (http://www.oracle.com/technetwork/java/javase/downloads/index.html) from Oracle.

* On Mac OS X, download and install [JDK 8] (http://www.oracle.com/technetwork/java/javase/downloads/index.html).

3 Install [PyCharm Educational Edition] (https://www.jetbrains.com/pycharm-educational/download/)

4 Create [IntelliJ Platform Plugin SDK] (https://www.jetbrains.com/idea/help/configuring-intellij-platform-plugin-sdk.html) with installed version of PyCharm Educational Edition and JDK 8 as Java SDK. Add *interactive-learning-python.jar* and  *interactive-learning.jar* from plugins directory to classpath of created IntelliJ Platform Plugin SDK.

![My image](https://github.com/evgraph/CheckIOPyCharmPlugin/blob/master/images/add_plugins_to_classpath.png)

5 Set created IntelliJ Platform Plugin SDK as Project SDK. ([How to configure project SDK] (https://www.jetbrains.com/idea/help/configuring-global-project-and-module-sdks.html#d1278485e69))
