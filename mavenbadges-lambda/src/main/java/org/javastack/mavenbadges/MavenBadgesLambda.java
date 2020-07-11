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
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.Collections;

import org.javastack.mavenbadges.MavenBadges.PathInfo;
import org.javastack.mavenbadges.MavenBadges.Response;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Badges for Maven Central
 * 
 * @author Guillermo Grandes / guillermo.grandes[at]gmail.com
 */
public class MavenBadgesLambda implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
	// https://docs.aws.amazon.com/lambda/latest/dg/java-package.html
	// https://docs.aws.amazon.com/lambda/latest/dg/lambda-java.html
	// https://github.com/awsdocs/aws-lambda-developer-guide/blob/master/sample-apps/java-events/events/apigateway-v2.json
	// https://github.com/awslabs/aws-serverless-java-container/wiki/Quick-start---Spring-Boot

	private Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private MavenBadges mb = null;

	public MavenBadgesLambda() {
		try {
			System.out.println("Constructor Begin");
			mb = new MavenBadges();
			// example: /org.javastack/mavenbadges/badge.svg
			final String initPathInfo = System.getenv("initPathInfo");
			if (initPathInfo != null) {
				// Hot Cold Start
				mb.process(PathInfo.parse(initPathInfo));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			System.out.println("Constructor End");
		}
	}

	@Override
	public APIGatewayV2HTTPResponse handleRequest(final APIGatewayV2HTTPEvent request, Context context) {
		final APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();
		final boolean isDebug = !"false".equals(request.getStageVariables().get("debug")); // default true
		final LambdaLogger log = context.getLogger();
		if (isDebug) {
			log.log("PROPERTIES: " + gson.toJson(System.getProperties()));
			log.log("REQUEST: " + gson.toJson(request));
			log.log("CONTEXT: " + gson.toJson(context));
			log.log("RAWPATH: " + gson.toJson(request.getRawPath()));
		}
		try {
			final PathInfo pi = PathInfo.parse(request.getRawPath());
			log.log("PathInfo: " + gson.toJson(pi));
			final Response r = mb.process(pi);
			response(response, r);
		} catch (FileNotFoundException e) {
			response(response, HttpURLConnection.HTTP_NOT_FOUND, "Not Found");
			log.log("Not found: " + e);
		} catch (SocketTimeoutException e) {
			response(response, HttpURLConnection.HTTP_GATEWAY_TIMEOUT, "Timeout");
			log.log("Timeout: " + e);
		} catch (Exception e) {
			response(response, HttpURLConnection.HTTP_INTERNAL_ERROR, "Internal Server Error");
			log.log("Internal Server Error: " + e);
			e.printStackTrace();
		}
		if (isDebug) {
			log.log("RESPONSE: " + gson.toJson(response));
		}
		return response;
	}

	private static final void response(final APIGatewayV2HTTPResponse response, final Response r)
			throws IOException {
		r.setHeader("Content-Type", r.getContentType());
		response.setStatusCode(r.getCode());
		response.setHeaders(r.getHeaders());
		response.setBody(r.getBody());
	}

	private static final void response(final APIGatewayV2HTTPResponse response, final int status,
			final String msg) {
		response.setStatusCode(status);
		response.setHeaders(Collections.singletonMap("Content-Type", "text/plain; charset=ISO-8859-1"));
		response.setBody(msg);
	}
}
