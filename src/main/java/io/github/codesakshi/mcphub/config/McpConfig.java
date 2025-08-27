package io.github.codesakshi.mcphub.config;

/**
 * 
 * @author anilalps.dev@gmail.com
 *
 */

/**
 * Configuration class for the MCP Hub.
 * <p>
 * This class encapsulates various configuration properties required for the MCP
 * server,
 * including server name, version, servlet path, SSE endpoint, API documentation
 * location,
 * host URL, server index, authorization token, retry policies, and timeout
 * settings.
 * </p>
 * <ul>
 * <li><b>mcpServerName</b>: Unique name of the MCP server.</li>
 * <li><b>mcpServerVersion</b>: Version of the MCP server.</li>
 * <li><b>servletPathSpec</b>: Servlet path specification for the MCP server
 * (default: "/").</li>
 * <li><b>mcpSseEndPoint</b>: SSE endpoint for the MCP server (default:
 * "sse").</li>
 * <li><b>mcpMessageEndPoint</b>: Message endpoint for the MCP server (default:
 * "message").</li>
 * <li><b>apiDocLocation</b>: API documentation location for Swagger or OpenAPI
 * (JSON/YAML).</li>
 * <li><b>apiHostUrl</b>: API host URL for the server.</li>
 * <li><b>apiServerIndex</b>: API server index to be used from Swagger or
 * OpenAPI spec (default: 0).</li>
 * <li><b>optimizeSchema</b>: Specifies the optimization of the schema (default:
 * true).</li>
 * <li><b>apiAuthorization</b>: API authorization token to be used when
 * executing REST API calls.</li>
 * <li><b>apiConnectionTimeoutMs</b>: API connection timeout in milliseconds
 * (default: 30000).</li>
 * <li><b>apiReadTimeoutMs</b>: API read timeout in milliseconds (default:
 * 10000).</li>
 * <li><b>apiMaxRetries</b>: Maximum number of retries for API calls (default:
 * 3).</li>
 * <li><b>apiRetryDelayMs</b>: Delay between API call retries in milliseconds
 * (default: 100).</li>
 * 
 * </ul>
 * <p>
 * Provides getter and setter methods for each configuration property.
 * </p>
 * 
 * @author anilalps.dev@gmail.com
 */
public class McpConfig {

    /**
     * Unique name of the mcp server.
     */
    private String mcpServerName;

    /**
     * Version of the mcp server.
     */
    private String mcpServerVersion;

    /**
     * Servlet Path specification for the mcp server.
     */
    private String servletPathSpec = "/";

    /**
     * SSE endpoint for the mcp server.
     */
    private String mcpSseEndPoint = "sse";

    /**
     * Message endpoint for the mcp server.
     */
    private String mcpMessageEndPoint = "message";

    /**
     * API documentation location for the swagger or open Api ( json/yml)
     */
    private String apiDocLocation;

    /**
     * API host URL for the server.
     */
    private String apiHostUrl = "";

    /**
     * API server index To be used from swagger or open Api spec
     */
    private int apiServerIndex = 0;

    /**
     * Specifies the optimization of the schema.
     */
    private boolean optimizeSchema = true;

    /**
     * API authorization token to be used when the REST api is executed.
     */
    private String apiAuthorization;

    /**
     * API connection timeout in milliseconds.
     */
    private int apiConnectionTimeoutMs = 10000;

    /**
     * API read timeout in milliseconds.
     */
    private int apiReadTimeoutMs = 10000;

    /**
     * Maximum number of retries for API calls.
     */
    private int apiMaxRetries = 3;

    /**
     * Delay between API call retries in milliseconds.
     */
    private int apiRetryDelayMs = 1000;

    /**
     * Get the mcp server name.
     *
     * @return the mcp server name
     */
    public String getMcpServerName() {
        return mcpServerName;
    }

    /**
     * Set the mcp server name.
     *
     * @param mcpServerName the mcp server name
     */
    public void setMcpServerName(String mcpServerName) {
        this.mcpServerName = mcpServerName;
    }

    /**
     * Get the mcp server version.
     *
     * @return the mcp server version
     */
    public String getMcpServerVersion() {
        return mcpServerVersion;
    }

    /**
     * Set the mcp server version.
     *
     * @param mcpServerVersion the mcp server version
     */
    public void setMcpServerVersion(String mcpServerVersion) {
        this.mcpServerVersion = mcpServerVersion;
    }

    /**
     * Get the Servlet path specification.
     *
     * @return the Servlet path specification
     */
    public String getServletPathSpec() {
        return servletPathSpec;
    }

    /**
     * Set the Servlet path specification.
     *
     * @param servletPathSpec the Servlet path specification
     */
    public void setServletPathSpec(String servletPathSpec) {
        this.servletPathSpec = servletPathSpec;
    }

    /**
     * Get the MCP SSE endpoint.
     *
     * @return the MCP SSE endpoint
     */
    public String getMcpSseEndPoint() {
        return mcpSseEndPoint;
    }

    /**
     * Set the MCP SSE endpoint.
     *
     * @param mcpSseEndPoint the MCP SSE endpoint
     */
    public void setMcpSseEndPoint(String mcpSseEndPoint) {
        this.mcpSseEndPoint = mcpSseEndPoint;
    }

    /**
     * Get the message endpoint for the MCP server.
     *
     * @return the message endpoint
     */
    public String getMcpMessageEndPoint() {
        return mcpMessageEndPoint;
    }

    /**
     * Set the message endpoint for the MCP server.
     *
     * @param messageEndPoint the message endpoint
     */
    public void setMcpMessageEndPoint(String messageEndPoint) {
        this.mcpMessageEndPoint = messageEndPoint;
    }    

    /**
     * Get the API documentation location.
     *
     * @return the API documentation location
     */
    public String getApiDocLocation() {
        return apiDocLocation;
    }

    /**
     * Set the API documentation location.
     *
     * @param apiDocLocation the API documentation location
     */
    public void setApiDocLocation(String apiDocLocation) {
        this.apiDocLocation = apiDocLocation;
    }

    /**
     * Get the API host URL.
     *
     * @return the API host URL
     */
    public String getApiHostUrl() {
        return apiHostUrl;
    }

    /**
     * Set the API host URL.
     *
     * @param apiHostUrl the API host URL
     */
    public void setApiHostUrl(String apiHostUrl) {
        this.apiHostUrl = apiHostUrl;
    }

    /**
     * Get the API server index.
     *
     * @return the API server index
     */
    public int getApiServerIndex() {
        return apiServerIndex;
    }

    /**
     * Set the API server index.
     *
     * @param apiServerIndex the API server index
     */
    public void setApiServerIndex(int apiServerIndex) {
        this.apiServerIndex = apiServerIndex;
    }

    /**
     * Get the optimization of the schema.
     *
     * @return the optimization of the schema
     */
    public boolean isOptimizeSchema() {
        return optimizeSchema;
    }

    /**
     * Set the optimization of the schema.
     *
     * @param optimizeSchema the optimization of the schema
     */
    public void setOptimizeSchema(boolean optimizeSchema) {
        this.optimizeSchema = optimizeSchema;
    }

    /**
     * Get the API authorization.
     *
     * @return the API authorization
     */
    public String getApiAuthorization() {
        return apiAuthorization;
    }

    /**
     * Set the API authorization.
     *
     * @param apiAuthorization the API authorization
     */
    public void setApiAuthorization(String apiAuthorization) {
        this.apiAuthorization = apiAuthorization;
    }

    /**
     * Get the API connection timeout in milliseconds.
     *
     * @return the API connection timeout in milliseconds
     */
    public int getApiConnectionTimeoutMs() {
        return apiConnectionTimeoutMs;
    }

    /**
     * Set the API connection timeout in milliseconds.
     *
     * @param apiConnectionTimeoutMs the API connection timeout in milliseconds
     */
    public void setApiConnectionTimeoutMs(int apiConnectionTimeoutMs) {
        this.apiConnectionTimeoutMs = apiConnectionTimeoutMs;
    }

    /**
     * Get the API read timeout in milliseconds.
     *
     * @return the API read timeout in milliseconds
     */
    public int getApiReadTimeoutMs() {
        return apiReadTimeoutMs;
    }

    /**
     * Set the API read timeout in milliseconds.
     *
     * @param apiReadTimeoutMs the API read timeout in milliseconds
     */
    public void setApiReadTimeoutMs(int apiReadTimeoutMs) {
        this.apiReadTimeoutMs = apiReadTimeoutMs;
    }

    /**
     * Get the API maximum retries.
     *
     * @return the API maximum retries
     */
    public int getApiMaxRetries() {
        return apiMaxRetries;
    }

    /**
     * Set the API maximum retries.
     *
     * @param apiMaxRetries the API maximum retries
     */
    public void setApiMaxRetries(int apiMaxRetries) {
        this.apiMaxRetries = apiMaxRetries;
    }

    /**
     * Get the API retry delay in milliseconds.
     *
     * @return the API retry delay in milliseconds
     */
    public int getApiRetryDelayMs() {
        return apiRetryDelayMs;
    }

    /**
     * Set the API retry delay in milliseconds.
     *
     * @param apiRetryDelayMs the API retry delay in milliseconds
     */
    public void setApiRetryDelayMs(int apiRetryDelayMs) {
        this.apiRetryDelayMs = apiRetryDelayMs;
    }



}
