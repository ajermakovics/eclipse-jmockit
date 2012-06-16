Eclipse plugin for JMockit
--
[Eclipse](http://www.eclipse.org) plugin that adds IDE support to [JMockit](https://code.google.com/p/jmockit/). Provides mock method suggestions and performs static analysis to report API misuse.

Downloads
--
- v 0.1.0 [eclipse-jmockit-assist_0.1.0.jar](https://github.com/downloads/ajermakovics/eclipse-jmockit-assist/eclipse-jmockit-assist_0.1.0.jar)

Features / Usage
--
* Suggests methods to be mocked
	* Press Ctrl+Space inside a class extending `MockUp` or annotated with `@MockClass`. A list of mockable methods will appear.


- Reports warnings as-you-type if mocking API is not used correctly
	* No corresponding real method for mocked method
	* Mock method is private
	* Mock method missing `@Mock` annotation 
	* `MockUp` used with interface but missing `getMockInstance()` call
	* ...

Installation
--
Copy the [plugin jar](https://github.com/downloads/ajermakovics/eclipse-jmockit-assist/eclipse-jmockit-assist_0.1.0.jar) to `eclipse/dropins` folder. Restart Eclipse.

There is no update site yet.

License
--
Eclipse Public License [1.0](http://www.eclipse.org/legal/epl-v10.html)

