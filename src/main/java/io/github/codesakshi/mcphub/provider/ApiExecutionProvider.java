package io.github.codesakshi.mcphub.provider;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.github.codesakshi.mcphub.config.McpConfig;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;

/**
 * 
 * @author anilalps.dev@gmail.com
 *
 */

/**
 * Provides functionality to execute API calls based on OpenAPI operation
 * metadata and request details.
 * <p>
 * This provider constructs and sends HTTP requests using OkHttp, supporting
 * path, query, and header parameters,
 * as well as request bodies and authorization headers. It serializes request
 * bodies to JSON and handles
 * parameter resolution according to the OpenAPI specification.
 * </p>
 * <p>
 * Usage involves supplying configuration, an HttpClient, OpenAPI operation
 * details, server URL, API path,
 * HTTP method, and a tool request containing arguments. The provider manages
 * parameter extraction, header
 * population, path resolution, and executes the HTTP request, returning the
 * response body as a string.
 * </p>
 * <p>
 * Logging is provided for request execution and error handling.
 * </p>
 * 
 * @author anilalps.dev@gmail.com
 */
public class ApiExecutionProvider {

    private static final Logger logger = LoggerFactory.getLogger(ApiExecutionProvider.class);

    private static final Map<String, Object> EMPTY_MAP = new HashMap<>();

    private static ObjectWriter writer;
    static {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        writer = objectMapper.writerWithDefaultPrettyPrinter();
    }

    /**
     * Executes an API call based on the provided OpenAPI operation and request
     * details.
     * <p>
     * This method constructs and sends an HTTP request using the provided
     * configuration, HTTP client,
     * OpenAPI operation metadata, and tool request arguments. It supports handling
     * of path, query, and header
     * parameters, as well as request bodies, and applies authorization headers if
     * configured.
     * </p>
     *
     * @param mcpConfig          The configuration object containing API
     *                           authorization and other settings.
     * @param jHttpClient        The HttpClient instance used to execute the HTTP
     *                           request.
     * @param operation          The OpenAPI Operation object describing the API
     *                           endpoint.
     * @param serverUrl          The base URL of the API server.
     * @param apiPath            The API path, possibly containing path parameters
     *                           (e.g., "/users/{id}").
     * @param httpMethod         The HTTP method to use (e.g., "GET", "POST").
     * @param toolRequest        The CallToolRequest containing arguments for the
     *                           API call.
     * @param hasRequestBody     Indicates if the request includes a body.
     * @param hasParameters      Indicates if the request includes parameters.
     * @param isUniqueParameters Indicates if all parameters are unique (no
     *                           duplicate keys).
     * @return The response body as a String.
     * @throws Exception If an error occurs during request construction or
     *                   execution.
     */
    public String executeApiCall(
            McpConfig mcpConfig,
            HttpClient jHttpClient,
            Operation operation,
            String serverUrl,
            String apiPath,
            String httpMethod,
            CallToolRequest toolRequest,
            boolean hasRequestBody,
            boolean hasParameters,
            boolean isUniqueParameters) throws Exception {

        logger.info("Entering executeApiCall with serverUrl: {}, apiPath: {}, httpMethod: {}",
                serverUrl, apiPath, httpMethod);

        logger.trace("Tool request arguments: {}", toolRequest.arguments());

        String requestBodyStr = null;

        if (hasRequestBody) {
            Object reqObject = toolRequest.arguments().get("requestBody");
            if (reqObject != null) {
                requestBodyStr = writer.writeValueAsString(reqObject);
                logger.trace("Request body string: {}", requestBodyStr);
            }
        }

        Map<String, Object> parameters = null;
        Map<String, Object> queries = null;
        Map<String, Object> headers = null;

        if (isUniqueParameters && mcpConfig.isOptimizeSchema() ) {
            logger.trace("Parameters are unique.");
            // No duplicate keys, all parameters are unique
            if (hasRequestBody) {
                // All in parameters
                parameters = toolRequest.arguments().get("parameters") != null
                        ? (Map<String, Object>) toolRequest.arguments().get("parameters")
                        : EMPTY_MAP;

                queries = parameters;
                headers = parameters;
            } else {

                // No request body, all parameters are in arguments
                parameters = toolRequest.arguments();
                queries = parameters;
                headers = parameters;
            }

        } else if (hasParameters) {
            logger.trace("Request has non-unique parameters.");
            parameters = toolRequest.arguments().get("parameters") != null
                    ? (Map<String, Object>) toolRequest.arguments().get("parameters")
                    : EMPTY_MAP;

            queries = toolRequest.arguments().get("queries") != null
                    ? (Map<String, Object>) toolRequest.arguments().get("queries")
                    : EMPTY_MAP;

            headers = toolRequest.arguments().get("headers") != null
                    ? (Map<String, Object>) toolRequest.arguments().get("headers")
                    : EMPTY_MAP;
        }

        logger.trace("Initial parameters map: {} from request", parameters);
        logger.trace("Initial queries map: {} from request", queries);
        logger.trace("Initial headers map: {} from request", headers);

        Map<String, String> apiParamMap = new HashMap<>();
        Map<String, String> apiQueryMap = new HashMap<>();
        Map<String, String> apiHeaderMap = new HashMap<>();

        // add parameters
        if (null != operation.getParameters()) {
            logger.debug("Processing operation parameters.");
            for (Parameter param : operation.getParameters()) {

                String paramName = param.getName();
                Object paramValue = null;

                if ("path".equals(param.getIn())) {

                    if (parameters != null) {
                        paramValue = parameters.get(paramName);
                    }
                    if (paramValue != null) {
                        apiParamMap.put(paramName, paramValue.toString());
                    }

                } else if ("query".equals(param.getIn())) {

                    if (queries != null) {
                        paramValue = queries.get(paramName);
                    }
                    if (paramValue != null) {
                        apiQueryMap.put(paramName, paramValue.toString());
                    }

                } else if ("header".equals(param.getIn())) {

                    if (headers != null) {
                        paramValue = headers.get(paramName);
                    }
                    if (paramValue != null) {
                        apiHeaderMap.put(paramName, paramValue.toString());
                    }

                } else {
                    logger.warn("Parameter : " + param.getName() + " in : " + param.getIn() + " is not supported");
                }
            }
        }

        logger.debug("Resolved API path parameters: {}", apiParamMap);
        logger.debug("Resolved API query parameters: {}", apiQueryMap);
        logger.debug("Resolved API header parameters: {}", apiHeaderMap);

        // Update authorization header if configured
        if (mcpConfig.getApiAuthorization() != null && !mcpConfig.getApiAuthorization().isEmpty()) {
            logger.debug("API authorization header is configured.");
            if (!apiHeaderMap.containsKey("Authorization")) {
                apiHeaderMap.put("Authorization", mcpConfig.getApiAuthorization());
                logger.debug("Added Authorization header.");
            }
        }

        // Resolve the path parameters
        String resolvedPath = apiPath;
        for (String paramKey : apiParamMap.keySet()) {
            resolvedPath = resolvedPath.replace("{" + paramKey + "}", apiParamMap.get(paramKey));
        }
        logger.debug("Resolved path: {}", resolvedPath);

        String pathUrl = serverUrl + resolvedPath;
        logger.info("Final request URL: {}", pathUrl);

        // We have all the data. Execute the API call

        String finalUrl =  pathUrl;
        
        if (!apiQueryMap.isEmpty()) {

            StringJoiner queryParamJoiner = new StringJoiner("&");
            for (Map.Entry<String, String> entry : apiQueryMap.entrySet()) {
                queryParamJoiner.add(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }

            finalUrl += "?" + queryParamJoiner.toString();
        }

        apiHeaderMap.put("Accept", "application/json; charset=utf-8");
        apiHeaderMap.put("Content-Type", "application/json; charset=utf-8");

        try {

            String responseBody = executeApiWithRetries(
                    mcpConfig,
                    jHttpClient,
                    URI.create(finalUrl),
                    httpMethod,
                    apiHeaderMap,
                    requestBodyStr);

            logger.info("API call successful. For url: {}", finalUrl);
            logger.debug("API call successful. for url:{} Response: {}", finalUrl, responseBody);
            return responseBody;
        } catch (Exception e) {
            logger.error("Error executing API call: " + e.getMessage(), e);
            throw e;
        }
    }
    
    private String executeApiWithRetries(McpConfig mcpConfig,
            HttpClient jHttpClient,
            URI uri,
            String method,
            Map<String, String> headers,
            String requestBody) throws IOException {

        int maxRetries = mcpConfig.getApiMaxRetries();

        Duration timeoutDuration = Duration.of(mcpConfig.getApiReadTimeoutMs(), ChronoUnit.MILLIS);

        logger.trace("Fetching data from URL: {}", uri.toString());
        int retryCount = 1;

        while (retryCount <= maxRetries) {

            logger.debug("Attempt {} of {}", retryCount, maxRetries);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(timeoutDuration)
                    .method(method, requestBody != null
                            ? HttpRequest.BodyPublishers.ofString(requestBody)
                            : HttpRequest.BodyPublishers.noBody());

            // Add headers from the HashMap
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.header(entry.getKey(), entry.getValue());
            }

            HttpRequest request = requestBuilder.build();

            try {
                
                HttpResponse<String> response = jHttpClient.send(request, HttpResponse.BodyHandlers.ofString());

                int statusCode = response.statusCode();
                if (statusCode == 200) {
                    logger.trace("Successfully fetched data from {}", uri.toString());
                    return response.body(); // Success
                } else if (statusCode >= 500 && statusCode < 600) {
                    logger.error("Server error ({})", statusCode);
                } else {
                    logger.error("Non-retriable HTTP error code: {}", statusCode);
                    break; // Don't retry on client errors
                }

            } catch (HttpTimeoutException e) {
                logger.error("Timeout occurred. Attempt {} of {}: ", retryCount, maxRetries, e);
            } catch (Exception e) {
                logger.error("Connection failed: Attempt {} of {}: ", retryCount, maxRetries, e);
            } finally {
                retryCount++;
            }

            if (retryCount < maxRetries) {
                try {
                    Thread.sleep(mcpConfig.getApiRetryDelayMs());
                } catch (InterruptedException e) {
                    logger.error("Retry delay interrupted: ", e);
                }
            }
        }

        logger.error("Failed to fetch data from {} after {} attempts.", uri.toString(), maxRetries);
        throw new IOException("Failed to fetch data from " + uri.toString() + " after " + maxRetries + " attempts.");
    }

}


