package org.javastack.mavenbadges;

class Constants {
	static final String MDC_IP = "IP";
	static final String MDC_ID = "ID";
	static final String MAVEN_METADATA = "https://repo1.maven.org/maven2/${groupId}/${artifactId}/maven-metadata.xml";
	static final String MAVEN_SEARCH = "https://search.maven.org/artifact/${groupId}/${artifactId}/${version}/jar";
	static final int CONNECTION_TIMEOUT = 10000;
	static final int READ_TIMEOUT = 10000;
}
