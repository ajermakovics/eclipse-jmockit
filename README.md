Eclipse plugin for JMockit
--
[Eclipse](http://www.eclipse.org) plugin that adds IDE support to [JMockit](https://code.google.com/p/jmockit/). Provides mock method suggestions and performs static analysis to report API misuse.


Features / Usage
--
1. Suggests methods to be mocked. ([example](https://github.com/ajermakovics/eclipse-jmockit-assist/raw/gh-pages/images/jmockit_eclipse_autocomplete.png))
	* Press Ctrl+Space inside a class extending `MockUp` or annotated with `@MockClass`. A list of mockable methods will appear.


2. Reports warnings as-you-type if mocking API is not used correctly ([example](https://github.com/ajermakovics/eclipse-jmockit-assist/raw/gh-pages/images/jmockit_errors.png))
	* No corresponding real method for mocked method
	* Mock method calling itself but is not marked as 'reentrant'
	* `MockUp` used with interface but missing `getMockInstance()` call
	* Mock method missing `@Mock` annotation 
	* and others

3. Lets you jump to real method from mock method. Hold Ctrl (Cmd) over mock method name.

4. Automatically adds JMockit jar as `-javaagent` argument to JUnit launches. [info](http://jmockit.googlecode.com/svn/trunk/www/gettingStarted.html)


Installation
--
In Eclipse instlall using the [Marketplace Client](http://marketplace.eclipse.org/content/jmockit-eclipse) from the Help menu

Alternatively you can use the Update site:
 - http://dl.bintray.com/ajermakovics/jmockit/


License
--
Eclipse Public License [1.0](http://www.eclipse.org/legal/epl-v10.html)

