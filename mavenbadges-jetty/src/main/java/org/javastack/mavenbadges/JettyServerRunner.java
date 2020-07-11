package org.javastack.mavenbadges;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

public class JettyServerRunner {
	public static final int PORT = 8080;

	public static void main(String[] args) throws Exception {
		// https://www.eclipse.org/jetty/documentation/current/embedding-jetty.html
		// Create a basic jetty server object that will listen on port 8080.

		// Note that if you set this to port 0 then a randomly available port
		// will be assigned that you can either look in the logs for the port,
		// or programmatically obtain it for use in test cases.
		final Server server = new Server(PORT);

		// The ServletHandler is a dead simple way to create a context handler
		// that is backed by an instance of a Servlet.
		// This handler then needs to be registered with the Server object.
		final ServletHandler handler = new ServletHandler();
		server.setHandler(handler);

		// Passing in the class for the Servlet allows jetty to instantiate an
		// instance of that Servlet and mount it on a given context path.

		// IMPORTANT:
		// This is a raw Servlet, not a Servlet that has been configured
		// through a web.xml @WebServlet annotation, or anything similar.
		handler.addServletWithMapping(MavenBadgesServlet.class, "/*");
		// Start things up!
		server.start();

		// The use of server.join() the will make the current thread join and
		// wait until the server thread is done executing.
		server.join();
	}
}
