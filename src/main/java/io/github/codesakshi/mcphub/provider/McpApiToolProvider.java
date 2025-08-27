package io.github.codesakshi.mcphub.provider;

import java.net.http.HttpClient;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.codesakshi.mcphub.config.McpConfig;
import io.github.codesakshi.mcphub.provider.McpSchemaProvider.InputSchemaWrap;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.converter.SwaggerConverter;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

/**
 * 
 * @author anilalps.dev@gmail.com
 *
 */

 /** Provider class for MCP API tool.
 * Provider class for generating SyncToolSpecifications from an OpenAPI/Swagger specification.
 * <p>
 * This class is responsible for:
 * <ul>
 *   <li>Loading the OpenAPI or Swagger specification from a configured location.</li>
 *   <li>Extracting API operations and generating corresponding {@link SyncToolSpecification} instances.</li>
 *   <li>Building tool definitions and call handlers for each API operation, enabling dynamic execution of API calls.</li>
 *   <li>Handling schema extraction and input validation for API operations.</li>
 *   <li>Supporting configuration of server URLs and host overrides via {@link McpConfig}.</li>
 * </ul>
 * </p>
 * <p>
 * Usage:
 * <pre>
 *     McpApiToolProvider provider = new McpApiToolProvider();
 *     List&lt;SyncToolSpecification&gt; specs = provider.createSyncToolSpecifications(mcpConfig, jHttpClient);
 * </pre>
 * </p>
 * <p>
 * Exceptions are logged and propagated where appropriate. Operations without an operationId are skipped with a warning.
 * </p>
 *
 * @author anilalps.dev@gmail.com
 */
public class McpApiToolProvider {

    private static final Logger logger = LoggerFactory.getLogger(McpApiToolProvider.class);

    /**
     * Provider class for MCP API schema.
     */
    private McpSchemaProvider mcpSchemaProvider = new McpSchemaProvider();

    /**
     * Provider class for API execution.
     */
    private ApiExecutionProvider apiExecutionProvider = new ApiExecutionProvider();

    /**
     * Creates a list of SyncToolSpecifications based on the provided MCP configuration and HTTP client.
     * <p>
     * This method reads the OpenAPI/Swagger specification from the location specified in the MCP configuration,
     * extracts the API operations, and creates corresponding SyncToolSpecifications for each operation. Each tool
     * specification includes a tool definition and a call handler that executes the API call using the provided
     * HTTP client.
     * </p>
     *
     * @param mcpConfig  the MCP configuration containing API documentation location and other settings
     * @param jHttpClient the jHttpClient instance to be used for executing API calls
     * @return a list of SyncToolSpecifications representing the API operations defined in the OpenAPI/Swagger spec
     * @throws Exception if there is an error loading the OpenAPI/Swagger specification or creating tool specifications
     */
    public List<SyncToolSpecification> createSyncToolSpecifications(
            McpConfig mcpConfig,
            HttpClient jHttpClient) throws Exception {

        logger.info("Creating tool specifications for server: {}", mcpConfig.getMcpServerName());
        logger.trace("Entering createSyncToolSpecifications for server: {}", mcpConfig.getMcpServerName());

        List<SyncToolSpecification> toolSpecifications = new LinkedList<SyncToolSpecification>();

        OpenAPI openApi = loadOpenApiSpec(mcpConfig);

        if (null == openApi) {
            logger.error("Failed to load OpenAPI/Swagger specification from location: {}",
                    mcpConfig.getApiDocLocation());
            throw new IllegalArgumentException(
                    "Failed to load OpenAPI/Swagger specification from location : " + mcpConfig.getApiDocLocation());
        }

        logger.debug("Successfully loaded OpenAPI specification from: {}", mcpConfig.getApiDocLocation());

        String serverUrl = getServerUrl(mcpConfig, openApi);
        logger.info("Resolved server URL: {} for server: {}", serverUrl, mcpConfig.getMcpServerName());

        openApi.getPaths().forEach((apiPath, pathItem) -> {

            logger.trace("Processing API path: {}", apiPath);

            pathItem.readOperationsMap().forEach((httpMethod, operation) -> {

                logger.trace("Processing operation for method: {}, path: {}", httpMethod, apiPath);

                try {

                    String operationId = operation.getOperationId();

                    if (null != operationId && !operationId.isBlank()) {
                        logger.info("Creating tool for operation: {}", operationId);

                        InputSchemaWrap lnputSchemaWrap = mcpSchemaProvider.buildInputMcpSchema(mcpConfig, openApi, operation);
                        logger.trace("Built input MCP schema for operationId: {}", operationId);

                        String summary = operation.getSummary();
                        if (null == summary || summary.isBlank()) {
                            summary = "API call to " + httpMethod.name() + " " + apiPath;
                            logger.trace("Generated summary for operationId {}: {}", operationId, summary);
                        }

                        String description = operation.getDescription();
                        if (null == description || description.isBlank()) {

                            description = summary;

                            if (null == description || description.isBlank()) {
                                description = operationId + " using API call to " + httpMethod.name() + " " + apiPath;
                            }

                            logger.trace("Generated description for operationId {}: {}", operationId, description);
                        }

                        Tool tool = Tool.builder()
                                .name(operationId)
                                .title(summary)
                                .description(description)
                                .inputSchema(lnputSchemaWrap.getSchema())
                                .build();

                        logger.debug("Created tool: {}", tool.name());

                        SyncToolSpecification toolSpec = SyncToolSpecification.builder()
                                .tool(tool)
                                .callHandler((exchange, toolRequest) -> {

                                    logger.trace("Entering call handler for tool: {}", operationId);

                                    try {

                                        String result = apiExecutionProvider.executeApiCall(
                                                mcpConfig,
                                                jHttpClient,
                                                operation,
                                                serverUrl,
                                                apiPath,
                                                httpMethod.name(),
                                                toolRequest,
                                                lnputSchemaWrap.isHasRequestBody(),
                                                lnputSchemaWrap.isHasParameters(),
                                                lnputSchemaWrap.isUniqueParameters());
                                        logger.debug("API call successful for tool: {}", operationId);
                                        logger.trace("API call result for tool {}: {}", operationId, result);
                                         return new CallToolResult(result, false);
                                    } catch (Exception e) {
                                        logger.error("Error executing API call for tool: "
                                                + operationId + ", error: " + e.getMessage(), e);
                                        return new CallToolResult("Error executing API call for tool: " + operationId + ", error: " + e.getMessage(), true);
                                    }

                                })
                                .build();                        toolSpecifications.add(toolSpec);
                        logger.debug("Added tool specification for: {}", operationId);

                    } else {
                        logger.warn("Skipping operation with missing operationId for API Path: " + apiPath
                                + ", method: " + httpMethod.name());
                    }

                } catch (Exception e) {
                    logger.error("Error creating tool specification for API Path: " + apiPath + ", method: "
                            + httpMethod.name() + ", error: " + e.getMessage(), e);
                }

            });

        });

        logger.info("Found {} tools for server: {}", toolSpecifications.size(), mcpConfig.getMcpServerName());
        logger.trace("Exiting createSyncToolSpecifications for server: {}. Found {} tools.",
            mcpConfig.getMcpServerName(), toolSpecifications.size());

        return toolSpecifications;
    }

    /**
     * Loads the OpenAPI specification from the configured location.
     *
     * @param mcpConfig the MCP configuration containing API documentation location
     * @return the loaded OpenAPI specification
     * @throws Exception if there is an error loading the specification
     */
    private OpenAPI loadOpenApiSpec(McpConfig mcpConfig) throws Exception {

        logger.info("Loading OpenAPI specification from: {}", mcpConfig.getApiDocLocation());
        logger.trace("Entering loadOpenApiSpec from location: {}", mcpConfig.getApiDocLocation());

        // Try loading as OpenAPI first, if fails try loading as Swagger
        SwaggerParseResult parseResult = new OpenAPIV3Parser()
                .readLocation(mcpConfig.getApiDocLocation(), null, null);

        OpenAPI openApi = parseResult.getOpenAPI();

        if (null == openApi) {

            logger.debug("Failed to parse as OpenAPI 3. Trying as Swagger 2.");
            parseResult = new SwaggerConverter()
                    .readLocation(mcpConfig.getApiDocLocation(), null, null);

            openApi = parseResult.getOpenAPI();
        }

        if (openApi != null) {
            logger.info("Successfully parsed API specification.");
        } else {
            logger.error("Failed to parse API specification from location: {}", mcpConfig.getApiDocLocation());
        }

        logger.trace("Exiting loadOpenApiSpec.");

        return openApi;
    }

    /**
     * Extracts the server URL from the OpenAPI specification.
     *
     * @param mcpConfig the MCP configuration containing API server index
     * @param openApi   the OpenAPI specification
     * @return the server URL as a String
     */
    private String getServerUrl(McpConfig mcpConfig, OpenAPI openApi) {

        logger.trace("Entering getServerUrl.");
        List<String> servers = openApi.getServers().stream()
                .map(server -> server.getUrl())
                .toList();

        logger.debug("Found servers in API specification: {}", servers);

        int index = Math.min(mcpConfig.getApiServerIndex(), servers.size() - 1);
        logger.debug("Using server index: {}", index);

        // get server url using given index
        String serverUrl = servers.get(index);
        logger.debug("Selected server URL from spec: {}", serverUrl);

        String apiHostUrl = mcpConfig.getApiHostUrl();
        logger.debug("Configured API host URL: {}", apiHostUrl);

        if (null != apiHostUrl && !apiHostUrl.isBlank()) {

            if (apiHostUrl.endsWith("/")) {
                apiHostUrl = apiHostUrl.substring(0, apiHostUrl.length() - 1);
            }

            if (!serverUrl.startsWith("/")) {
                // If server url is relative and apiHostUrl is absolute, use apiHostUrl as base
                serverUrl = apiHostUrl + "/" + serverUrl;
            } else {
                // Both are absolute, override server url with apiHostUrl
                serverUrl = apiHostUrl + serverUrl;
            }

            logger.debug("Overridden server URL with host URL. New server URL: {}", serverUrl);
        }

        logger.trace("Exiting getServerUrl. Final server URL: {}", serverUrl);
        return serverUrl;
    }

}
