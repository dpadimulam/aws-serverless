package com.task02;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;

@LambdaHandler(
    lambdaName = "hello_world",
	roleName = "hello_world-role",
	isPublishVersion = true,
	aliasName = "${lambdas_alias_name}",
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@LambdaUrlConfig(
        authType = AuthType.NONE,
        invokeMode = InvokeMode.BUFFERED
   )
public class HelloWorld implements RequestHandler<Map<String, Object>, Map<String, Object>> {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public Map<String, Object> handleRequest(Map<String, Object> request, Context context) {
		System.out.println("Received request: " + request);
		try {
			if (!request.containsKey("requestContext") || !(request.get("requestContext") instanceof Map)) {
				return genLambdaResponse(400, "Missing or invalid 'requestContext' in request.");
			}
			Map<String, Object> requestContext = ((Map<String, Object>) request.get("requestContext"));
			if (!requestContext.containsKey("http") || !(requestContext.get("http") instanceof Map)) {
				return genLambdaResponse(400, "Missing or invalid 'http' object in requestContext.");
			}
			Map<String, Object> http = ((Map<String, Object>) requestContext.get("http"));
			String path = String.valueOf(http.getOrDefault("path", ""));
			String method = String.valueOf(http.getOrDefault("method", ""));
			
			if (path.isEmpty() || method.isEmpty()) {
				return genLambdaResponse(400, "Missing 'path' or 'method' in request.");
			}
			if ("/hello".equals(path) && "GET".equalsIgnoreCase(method)) {
				return genLambdaResponse(200, "Hello from Lambda");
			} else {
				return genLambdaResponse(400,
						"Bad request syntax or unsupported method. Request path: " + path + ". HTTP method: " + method);
			}
		} catch (Exception e) {
			return genLambdaResponse(500, "Internal Server Error: " + e.getMessage());
		}
	}

	private Map<String, Object> genLambdaResponse(int status, String message) {
		Map<String, Object> response = new HashMap<>();
		Map<String, Object> body = new HashMap<>();
		body.put("statusCode", status);
		body.put("message", message);
		try {
			String bodyString = objectMapper.writeValueAsString(body);
			response.put("statusCode", status);
			response.put("body", bodyString);
			response.put("headers", Map.of("content-type", "application/json"));
			response.put("isBase64Encoded", false);
			return response;
		} catch (Exception e) {
			return Map.of("statusCode", 500, "body", "{\"statusCode\":500, \"message\":\"Internal Server Error\"}",
					"headers", Map.of("content-type", "application/json"), "isBase64Encoded", false);
		}
	}

}
