package org.javastack.mavenbadges;

import java.io.File;

import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;

public class TomcatServerRunner {
	public static final int PORT = 8080;
	private static final String ROOT_CONTEXT_PATH = "";

	public static void main(String[] args) throws Exception {
		// https://devcenter.heroku.com/articles/create-a-java-web-application-using-embedded-tomcat
		// https://www.codejava.net/servers/tomcat/how-to-embed-tomcat-server-into-java-web-applications
		// Create a basic tomcat server object that will listen on port 8080.
		Tomcat tomcat = new Tomcat();
		tomcat.setPort(PORT);
		StandardContext ctx = (StandardContext) tomcat.addContext(ROOT_CONTEXT_PATH,
				new File(".").getAbsolutePath());
		tomcat.addServlet(ROOT_CONTEXT_PATH, "MavenBadges", new MavenBadgesServlet());
		ctx.addServletMappingDecoded("/*", "MavenBadges");
		tomcat.start();
		tomcat.getServer().await();
	}
}