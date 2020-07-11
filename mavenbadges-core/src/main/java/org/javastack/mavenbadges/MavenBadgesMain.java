package org.javastack.mavenbadges;

import org.javastack.mavenbadges.MavenBadges.PathInfo;
import org.javastack.mavenbadges.MavenBadges.Response;

/**
 * Simple Test
 */
public class MavenBadgesMain {
	public static void main(String[] args) throws Throwable {
		final MavenBadges mb = new MavenBadges();
		if (true) {
			final PathInfo pi = PathInfo.parse("/org.javastack/mavenbadges-core/badge.svg");
			System.out.println("PathInfo: " + pi);
			final Response r = mb.process(pi);
			System.out.println("StatusCode: " + r.getCode());
			System.out.println("Headers: " + r.getHeaders());
			System.out.println("Body: " + r.getBody());
		}
		if (true) {
			final PathInfo pi = PathInfo.parse("/org.javastack/mavenbadges-core/link");
			System.out.println("PathInfo: " + pi);
			final Response r = mb.process(pi);
			System.out.println("StatusCode: " + r.getCode());
			System.out.println("Headers: " + r.getHeaders());
			System.out.println("Body: " + r.getBody());
		}
	}
}
