/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.javastack.mavenbadges;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.util.Map.Entry;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.javastack.mapexpression.InvalidExpression;
import org.javastack.mavenbadges.MavenBadges.PathInfo;
import org.javastack.mavenbadges.MavenBadges.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.xml.sax.SAXException;

/**
 * Badges for Maven Central
 * 
 * @author Guillermo Grandes / guillermo.grandes[at]gmail.com
 */
public class MavenBadgesServlet extends HttpServlet {
	static final Logger log = LoggerFactory.getLogger(MavenBadgesServlet.class);
	private static final long serialVersionUID = 42L;
	private MavenBadges mb = null;

	@Override
	public void init() throws ServletException {
		try {
			mb = new MavenBadges();
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	@Override
	public void destroy() {
	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		try {
			MDC.put(Constants.MDC_IP, request.getRemoteAddr());
			MDC.put(Constants.MDC_ID, getNewID());
			doGet0(request, response);
		} catch (FileNotFoundException e) {
			if (response.isCommitted()) {
				throw new ServletException(e);
			} else {
				response(response, HttpServletResponse.SC_NOT_FOUND, "Not Found");
				log.error("Not found: " + e);
			}
		} catch (SocketTimeoutException e) {
			if (response.isCommitted()) {
				throw new ServletException(e);
			} else {
				response(response, HttpServletResponse.SC_GATEWAY_TIMEOUT, "Timeout");
				log.error("Timeout: " + e);
			}
		} catch (Exception e) {
			if (response.isCommitted()) {
				throw new ServletException(e);
			} else {
				response(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
				log.error("Internal Server Error: " + e, e);
			}
		} finally {
			MDC.clear();
		}
	}

	private void doGet0(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException, InvalidExpression, ParserConfigurationException,
			SAXException {
		final PathInfo pi = PathInfo.parse(request.getPathInfo());
		final Response r = mb.process(pi);
		response(response, r);
	}

	private static final void response(final HttpServletResponse response, final Response r)
			throws IOException {
		final PrintWriter out = response.getWriter();
		response.setStatus(r.getCode());
		response.setContentType(r.getContentType());
		for (final Entry<String, String> e : r.getHeaders().entrySet()) {
			response.setHeader(e.getKey(), e.getValue());
		}
		out.print(r.getBody());
	}

	private static final void response(final HttpServletResponse response, final int status, final String msg)
			throws IOException {
		final PrintWriter out = response.getWriter();
		response.setStatus(status);
		response.setContentType("text/plain; charset=ISO-8859-1");
		out.print(msg);
	}

	private static final String getNewID() {
		return UUID.randomUUID().toString();
	}
}
