package com.thinkgem.jeesite.modules.sys.listener;

import javax.servlet.ServletContext;

import org.springframework.web.context.WebApplicationContext;

import com.thinkgem.jeesite.modules.sys.service.SystemService;

public class WebContextListener extends org.springframework.web.context.ContextLoaderListener {
	
	@Override
	public WebApplicationContext initWebApplicationContext(ServletContext servletContext) {
		if (!SystemService.printKeyLoadMessage()){
			return null;	//【扣】虽然这里现在不可能执行到，但，若initWebApplicationContext返回null会导致什么？程序终止在此？
		}
		return super.initWebApplicationContext(servletContext);
/*
		ContextLoader.initWebApplicationContext doc：

		public WebApplicationContext initWebApplicationContext(ServletContext servletContext)

		Initialize Spring's web application context for the given servlet context,
		using the application context provided at construction time, or
		creating a new one according to the "contextClass" and "contextConfigLocation" context-params.

		Parameters:
		servletContext - current servlet context

		Returns:
		the new WebApplicationContext

		------------------------------------------ContextLoaderListener-------------------------------------------------

		public class ContextLoaderListener extends ContextLoader
		                                   implements ServletContextListener
		Bootstrap listener to start up and shut down Spring's root WebApplicationContext.
		Simply delegates to ContextLoader as well as to ContextCleanupListener.
		This listener should be registered after Log4jConfigListener in web.xml, if the latter is used.

		As of Spring 3.1, ContextLoaderListener supports injecting the root web application context via the ContextLoaderListener(WebApplicationContext) constructor,
		allowing for programmatic configuration in Servlet 3.0+ environments. See WebApplicationInitializer for usage examples.

		----------------------------------------------ContextLoader-----------------------------------------------------

		public class ContextLoader extends Object

		Performs the actual initialization work for the root application context. Called by ContextLoaderListener.
		Looks for a "contextClass" parameter at the web.xml context-param level to specify the context class type, falling back to XmlWebApplicationContext if not found.
		With the default ContextLoader implementation, any context class specified needs to implement the ConfigurableWebApplicationContext interface.

		Processes a "contextConfigLocation" context-param and passes its value to the context instance,
		parsing it into potentially multiple file paths which can be separated by any number of commas and spaces, e.g. "WEB-INF/applicationContext1.xml, WEB-INF/applicationContext2.xml".
		Ant-style path patterns are supported as well, e.g. ......
		If not explicitly specified, the context implementation is supposed to use a default location (with XmlWebApplicationContext: "/WEB-INF/applicationContext.xml").

		Note:
		In case of multiple config locations, later bean definitions will override ones defined in previously loaded files,
		                                      at least when using one of Spring's default ApplicationContext implementations.
		This can be leveraged to deliberately override certain bean definitions via an extra XML file.

		Above and beyond loading the root application context,
		this class can optionally load or obtain and hook up a shared parent context to the root application context.
		See the loadParentContext(ServletContext) method for more information.

		As of Spring 3.1, ContextLoader supports injecting the root web application context via the ContextLoader(WebApplicationContext) constructor,
				          allowing for programmatic configuration in Servlet 3.0+ environments.
		See WebApplicationInitializer for usage examples.

*/
	}
}
