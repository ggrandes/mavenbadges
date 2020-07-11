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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.javastack.fontmetrics.SimpleFontMetrics;
import org.javastack.mapexpression.InvalidExpression;
import org.javastack.mapexpression.MapExpression;
import org.javastack.mapexpression.mapper.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Badges for Maven Central
 * 
 * @author Guillermo Grandes / guillermo.grandes[at]gmail.com
 */
public class MavenBadges {
	static final Logger log = LoggerFactory.getLogger(MavenBadges.class);
	private MapExpression metaMapper, searchMapper, svgMapper, linkMapper;
	private LinkedHashMap<String, String> versionCache;

	public MavenBadges() throws IOException, InvalidExpression {
		metaMapper = new MapExpression().setExpression(Constants.MAVEN_METADATA).parse();
		searchMapper = new MapExpression().setExpression(Constants.MAVEN_SEARCH).parse();
		svgMapper = new MapExpression().setExpression(getResourceTemplate("template.svg")).parse();
		linkMapper = new MapExpression().setExpression(getResourceTemplate("template.html")).parse();
		versionCache = new LinkedHashMap<String, String>() {
			private static final long serialVersionUID = 42L;

			@Override
			protected boolean removeEldestEntry(final Map.Entry<String, String> eldest) {
				return size() > 128;
			}
		};
	}

	private String getResourceTemplate(final String filename) throws IOException {
		BufferedReader br = null;
		InputStream is = null;
		try {
			final StringBuilder sb = new StringBuilder();
			is = getClass().getResourceAsStream("/" + filename);
			br = new BufferedReader(new InputStreamReader(is));
			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line.trim());
			}
			return sb.toString();
		} finally {
			closeSilent(br);
			closeSilent(is);
		}
	}

	public Response process(final PathInfo pi)
			throws IOException, InvalidExpression, ParserConfigurationException, SAXException {
		if ((pi != null) && !pi.groupId.isEmpty() && !pi.artifactId.isEmpty()
				&& ("badge.svg".equals(pi.filename) || "link".equals(pi.filename))) {
			log.info("groupId=" + pi.groupId + " artifactId=" + pi.artifactId + " file=" + pi.filename);
			final String cacheKey = pi.groupId + ":" + pi.artifactId;
			String version = null;
			synchronized (versionCache) {
				version = versionCache.get(cacheKey);
			}
			if (version != null) {
				log.info("Version cache found cacheKey=" + cacheKey + " version=" + version);
			} else {
				final long begin = System.currentTimeMillis();
				version = getVersion(getURL(pi.groupId, pi.artifactId));
				synchronized (versionCache) {
					versionCache.put(cacheKey, version);
				}
				log.info("Version getted (" + (System.currentTimeMillis() - begin) + "ms)" //
						+ " cacheKey=" + cacheKey + " version=" + version);
			}
			// Send response
			if (!"?".equals(version)) {
				if ("badge.svg".equals(pi.filename)) {
					final long begin = System.currentTimeMillis();
					final String svg = generateSVG("maven-central", "v" + version);
					log.info("SVG generated (" + (System.currentTimeMillis() - begin) + "ms)" //
							+ " size=" + svg.length());
					return response(HttpURLConnection.HTTP_OK, svg);
				} else if ("link".equals(pi.filename)) {
					final String link = getLink(pi, version);
					final String html = generateHTML(link);
					return redirect(HttpURLConnection.HTTP_MOVED_TEMP, html, link);
				}
			}
		}
		return response(HttpURLConnection.HTTP_NOT_FOUND, "Not Found");
	}

	private final String getURL(final String groupId, final String artifactId) throws InvalidExpression {
		final StringBuilder sb = new StringBuilder(Constants.MAVEN_METADATA.length() + 32);
		metaMapper.eval(sb, new Mapper() {
			@Override
			public String map(final String input) {
				if ("groupId".equals(input)) {
					return groupId.replace('.', '/');
				} else if ("artifactId".equals(input)) {
					return artifactId;
				}
				return null;
			}
		});
		return sb.toString();
	}

	private final String getVersion(final String url)
			throws IOException, ParserConfigurationException, SAXException {
		final URL u = new URL(url);
		InputStream is = null;
		URLConnection conn = null;
		final DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		try {
			conn = u.openConnection();
			conn.setConnectTimeout(Constants.CONNECTION_TIMEOUT);
			conn.setReadTimeout(Constants.READ_TIMEOUT);
			conn.setDoOutput(false);
			conn.setUseCaches(true);
			conn.connect();
			is = conn.getInputStream();
			final Document doc = db.parse(is);
			doc.getDocumentElement().normalize();
			return doc.getElementsByTagName("release").item(0).getTextContent();
		} finally {
			closeSilent(is);
		}
	}

	private final int textLength(final String in) {
		final SimpleFontMetrics metrics = SimpleFontMetrics.getInstance();
		final int w = (metrics.widthOf(in) / 10);
		return (((w & 1) == 0) ? w + 1 : w); // roundUpToOdd
	}

	private final String generateHTML(final String link) throws InvalidExpression {
		final StringBuilder sb = new StringBuilder(Constants.MAVEN_METADATA.length() + 32);
		linkMapper.eval(sb, new Mapper() {
			@Override
			public String map(final String input) {
				if ("link".equals(input)) {
					return link;
				}
				return null;
			}
		});
		return sb.toString();
	}

	private final String generateSVG(final String leftText, final String rightText)
			throws IOException, InvalidExpression {
		// Calculations carried from:
		// https://github.com/badges/shields/blob/master/badge-maker/lib/badge-renderers.js
		// SVG Reference:
		// https://www.w3schools.com/graphics/svg_reference.asp
		// == Vertical ==
		final int height = 20;
		final int verticalMargin = 0;
		final int shadowMargin = 150 + verticalMargin;
		final int textMargin = 140 + verticalMargin;
		// == Horizontal ==
		final int horizPadding = 5;
		// Left Tag
		final int leftLength = textLength(leftText);
		final int leftMargin = 1;
		final int leftX = (int) (10 * (leftMargin + 0.5f * leftLength + horizPadding));
		final int leftWidth = leftLength + 2 * horizPadding;
		// Right Tag
		final int rightLength = textLength(rightText);
		final int rightMargin = leftWidth - 1;
		final int rightX = (int) (10 * (rightMargin + 0.5f * rightLength + horizPadding));
		final int rightWidth = rightLength + 2 * horizPadding;
		//
		final int width = leftWidth + rightWidth;
		final StringBuilder sb = new StringBuilder(64 //
				+ Constants.MAVEN_METADATA.length() //
				+ leftText.length() //
				+ rightText.length());
		svgMapper.eval(sb, new Mapper() {
			@Override
			public String map(String input) {
				if ("leftText".equals(input)) {
					return escapeXML(leftText);
				} else if ("rightText".equals(input)) {
					return escapeXML(rightText);
				} else if ("height".equals(input)) {
					return String.valueOf(height);
				} else if ("width".equals(input)) {
					return String.valueOf(width);
				} else if ("leftWidth".equals(input)) {
					return String.valueOf(leftWidth);
				} else if ("rightWidth".equals(input)) {
					return String.valueOf(rightWidth);
				} else if ("shadowMargin".equals(input)) {
					return String.valueOf(shadowMargin);
				} else if ("textMargin".equals(input)) {
					return String.valueOf(textMargin);
				} else if ("leftX".equals(input)) {
					return String.valueOf(leftX);
				} else if ("rightX".equals(input)) {
					return String.valueOf(rightX);
				} else if ("leftLength".equals(input)) {
					return String.valueOf(leftLength * 10);
				} else if ("rightLength".equals(input)) {
					return String.valueOf(rightLength * 10);
				}
				return null;
			}
		});
		return sb.toString();
	}

	private final String escapeXML(final String in) {
		if ((in == null) || in.isEmpty()) {
			return "";
		} else {
			return in //
					.replace("&", "&amp;") //
					.replace("<", "&lt;") //
					.replace(">", "&gt;") //
					.replace("\"", "&quot;") //
					.replace("\'", "&apos;");
		}
	}

	private final String getLink(final PathInfo pi, final String version)
			throws IOException, InvalidExpression {
		final StringBuilder sb = new StringBuilder(Constants.MAVEN_SEARCH.length() + 32);
		searchMapper.eval(sb, new Mapper() {
			@Override
			public String map(final String input) {
				if ("groupId".equals(input)) {
					return pi.groupId;
				} else if ("artifactId".equals(input)) {
					return pi.artifactId;
				} else if ("version".equals(input)) {
					return version;
				}
				return null;
			}
		});
		return sb.toString();
	}

	private static final Response redirect(final int code, final String body, final String link)
			throws IOException {
		// Send Redirect
		final Response response = new Response();
		response.setCode(code);
		response.setContentType("text/html");
		response.setHeader("Location", link);
		response.setHeader("Cache-Control", "public, max-age=3600");
		response.setBody(body);
		return response;
	}

	private static final Response response(final int code, final String body) throws IOException {
		// Send Response
		final Response response = new Response();
		response.setCode(code);
		if (code == HttpURLConnection.HTTP_OK) {
			response.setContentType("image/svg+xml");
			response.setHeader("Cache-Control", "public, max-age=3600");
			response.setBody(body);
		} else {
			response.setContentType("text/plain; charset=ISO-8859-1");
			response.setBody(body);
		}
		return response;
	}

	private static final void closeSilent(final Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (Throwable ign) {
			}
		}
	}

	public static class Response {
		private int code;
		private String contentType;
		private int contentLength;
		private Map<String, String> headers;
		private String body;

		public Response() {
		}

		private Map<String, String> headers() {
			if (headers == null) {
				headers = new HashMap<String, String>();
			}
			return headers;
		}

		public void setHeader(final String key, final String value) {
			headers().put(key, value);
		}

		public int getCode() {
			return code;
		}

		public void setCode(final int code) {
			this.code = code;
		}

		public String getContentType() {
			return contentType;
		}

		public void setContentType(final String contentType) {
			this.contentType = contentType;
		}

		public int getContentLength() {
			return contentLength;
		}

		public Map<String, String> getHeaders() {
			return ((headers != null) //
					? Collections.unmodifiableMap(headers) //
					: Collections.emptyMap());
		}

		public void setHeaders(final Map<String, String> headers) {
			this.headers = headers;
		}

		public String getBody() {
			return body;
		}

		public void setBody(final String body) {
			this.body = body;
		}
	}

	public static class PathInfo {
		public final String groupId;
		public final String artifactId;
		public final String filename;

		private PathInfo(final String groupId, final String artifactId, final String filename) {
			this.groupId = groupId;
			this.artifactId = artifactId;
			this.filename = filename;
		}

		public static final PathInfo parse(String pathInfo) {
			if (pathInfo == null)
				return null;
			if (pathInfo.isEmpty())
				return null;
			final int len = pathInfo.length();
			for (int i = 1; i < len; i++) {
				final char c = pathInfo.charAt(i);
				if ((c >= 'A') && (c <= 'Z'))
					continue;
				if ((c >= 'a') && (c <= 'z'))
					continue;
				if ((c >= '0') && (c <= '9'))
					continue;
				if ((c == '-') || (c == '_') || (c == '.') || (c == '/'))
					continue;
				log.warn("Invalid path: " + pathInfo);
				return null;
			}
			final String[] tokens = pathInfo.split("/");
			final String groupId = (tokens.length > 1 ? tokens[1] : "");
			final String artifactId = (tokens.length > 2 ? tokens[2] : "");
			final String filename = (tokens.length > 3 ? tokens[3] : "");
			return new PathInfo(groupId, artifactId, filename);
		}

		@Override
		public String toString() {
			return "groupId=" + groupId + " artifactId=" + artifactId + " filename=" + filename;
		}
	}
}
