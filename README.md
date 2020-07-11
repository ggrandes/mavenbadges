# MavenBadges

Badges! You see these little tagged pictures and some numbers on many GitHub readmes. We all love them, yeah?

This is my implementation in Java, only for fun

[![Maven Central](https://maven-badges.javastack.org/mavenbadges-core/badge.svg)](https://maven-badges.javastack.org/mavenbadges-core/link)

---

### Packages

- mavenbadges-lambda: Run in Serverless mode on AWS-Lambda
- mavenbadges-servlet
    - mavenbadges-jetty: Run standalone with embedded Jetty `java -jar mavenbadges-jetty.jar`
    - mavenbadges-tomcat: Run standalone with embedded Tomcat `java -jar mavenbadges-tomcat.jar`
    - mavenbadges-war: Run in any standard servlet container
- mavenbadges-core: The source of magic

###### Issues with Java 11+ with Javadoc. With Eclipse: `Run configurations >> Environment >> variable: JAVA_HOME value: ${system_property:java.home}`

### Usage

- Image: `http://localhost:8080/<groupId>/<artifactId>/badge.svg`
- Link: `http://localhost:8080/<groupId>/<artifactId>/link`

###### Example HTML

    <a href="http://localhost:8080/org.javastack/mavenbadges-core/link"><img src="http://localhost:8080/org.javastack/mavenbadges-core/badge.svg"></a>

###### Example Markdown

    [![Maven Central](http://localhost:8080/org.javastack/mavenbadges-core/badge.svg)](http://localhost:8080/org.javastack/mavenbadges-core/link)


---
Inspired in [shields.io](https://github.com/badges/shields) and [maven-badges](https://github.com/jirutka/maven-badges/), this code is Java-minimalistic version.
