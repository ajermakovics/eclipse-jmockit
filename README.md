Eclipse plugin for JMockit
--
[Eclipse](http://www.eclipse.org) plugin that adds IDE support to [JMockit](https://code.google.com/p/jmockit/). Provides mock method suggestions and performs static analysis to report API misuse.


Features / Usage
--
* Suggests methods to be mocked
	* Press Ctrl+Space inside a class extending `MockUp` or annotated with `@MockClass`. A list of mockable methods will appear.


* Reports warnings as-you-type if mocking API is not used correctly
	* No corresponding real method for mocked method
	* Mock method calling itself but is not marked as 'reentrant'
	* `MockUp` used with interface but missing `getMockInstance()` call
	* Mock method missing `@Mock` annotation 
	* and others

* Automatically adds JMockit jar as `-javaagent` argument to JUnit launches. [info](http://jmockit.googlecode.com/svn/trunk/www/gettingStarted.html)


Installation
--
In Eclipse 3.7 (Indigo):

Using Update site:
 - https://raw.github.com/ajermakovics/jmockit-eclipse-updatesite/master

Or:
 - Copy the [plugin jar](https://github.com/downloads/ajermakovics/eclipse-jmockit-assist) to `eclipse/dropins` folder. Restart Eclipse.


License
--
Eclipse Public License [1.0](http://www.eclipse.org/legal/epl-v10.html)

