package io.github.codesakshi.mcphub.provider;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.codesakshi.mcphub.config.McpConfig;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import jakarta.servlet.http.HttpServlet;

/**
 * 
 * @author anilalps.dev@gmail.com
 *
 */

/**
 * Provider class for creating and configuring instances of
 * {@link McpSyncServer}
 * along with their associated HTTP servlet and path specification.
 * <p>
 * This class is responsible for:
 * <ul>
 * <li>Building an {@link jHttpClient} with retry and timeout configurations
 * from {@link McpConfig}.</li>
 * <li>Creating tool specifications using {@link McpApiToolProvider}.</li>
 * <li>Configuring the server's transport provider using SSE (Server-Sent
 * Events).</li>
 * <li>Setting up server capabilities and registering tools.</li>
 * <li>Determining the servlet path specification and SSE endpoint URL.</li>
 * </ul>
 * 
 * <p>
 * Usage:
 * 
 * <pre>
 * McpServerProvider provider = new McpServerProvider();
 * McpServerProvider.McpServerInstance instance = provider.createMcpServerInstance(appContextPath, mcpConfig);
 * </pre>
 * </p>
 * 
 * <p>
 * If no tool specifications are found, server creation is skipped and
 * {@code null} is returned.
 * </p>
 * 
 * @author anilalps.dev@gmail.com
 */
public class McpServerProvider {

    private static final Logger logger = LoggerFactory.getLogger(McpServerProvider.class);

    /*
     * Represents an instance of the MCP server, including its associated
     * HTTP servlet and path specification.
     */
    public static class McpSseServerInstance {
        private final McpSyncServer mcpSyncServer;
        private final HttpServlet servlet;
        private final String pathSpec;

        public McpSseServerInstance(McpSyncServer mcpSyncServer, HttpServlet servlet, String pathSpec) {
            this.mcpSyncServer = mcpSyncServer;
            this.servlet = servlet;
            this.pathSpec = pathSpec;
        }

        public McpSyncServer getMcpSyncServer() {
            return mcpSyncServer;
        }

        public HttpServlet getServlet() {
            return servlet;
        }

        public String getPathSpec() {
            return pathSpec;
        }
    }

    /*
     * Provider class for MCP Server.
     */
    private McpApiToolProvider mcpApiToolProvider = new McpApiToolProvider();

    /**
     * Creates an instance of the MCP server.
     *
     * @param appContextPath the application context path
     * @param mcpConfig      the MCP configuration
     * @return an instance of {@link McpSseServerInstance} or {@code null} if no
     *         tools are found
     * @throws Exception if an error occurs during server creation
     */
    public McpSseServerInstance createMcpSseServerInstance(String appContextPath, McpConfig mcpConfig)
            throws Exception {

        String mcpSseEndPoint = generateEndPoint(mcpConfig.getMcpServerName(),
                appContextPath,
                mcpConfig.getServletPathSpec(),
                mcpConfig.getMcpSseEndPoint());

        String mcpMessageEndPoint = generateEndPoint(mcpConfig.getMcpServerName(),
                appContextPath,
                mcpConfig.getServletPathSpec(),
                mcpConfig.getMcpMessageEndPoint());

        logger.debug("Using SSE endpoint: {}, Message Endpoint {} for MCP server: {}", 
            mcpSseEndPoint, mcpMessageEndPoint, mcpConfig.getMcpServerName());

        HttpServletSseServerTransportProvider transportProvider = HttpServletSseServerTransportProvider.builder()
                .objectMapper(new ObjectMapper())
                .sseEndpoint(mcpSseEndPoint)
                .messageEndpoint(mcpMessageEndPoint)
                .build();

        logger.trace("Created HttpServletSseServerTransportProvider with endpoint: {} for MCP server: {}",
                mcpSseEndPoint, mcpConfig.getMcpServerName());

        return createMcpServerInstance(mcpConfig, transportProvider);
    }

    public McpSseServerInstance createMcpServerInstance(McpConfig mcpConfig,
            McpServerTransportProvider transportProvider)
            throws Exception {

        logger.info("Creating SSE MCP server instance for server: {}", mcpConfig.getMcpServerName());
        logger.debug("Using MCP config: {}", mcpConfig);

        // Single HTTP client per server.
        HttpClient jHttpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(mcpConfig.getApiConnectionTimeoutMs()))
                .build();

        logger.trace("Created HttpClient:");

        List<SyncToolSpecification> toolSpecifications = mcpApiToolProvider.createSyncToolSpecifications(mcpConfig,
                jHttpClient);
        logger.debug("Found {} tool specifications for MCP server: {}",
                toolSpecifications != null ? toolSpecifications.size() : 0, mcpConfig.getMcpServerName());

        if (null == toolSpecifications || toolSpecifications.isEmpty()) {
            logger.warn("No tool specifications found for MCP config: " + mcpConfig.getMcpServerName()
                    + ". Skipping MCP server creation.");
            return null;
        }

        McpSyncServer mcpSyncServer = McpServer.sync(transportProvider)
                .serverInfo(mcpConfig.getMcpServerName(), mcpConfig.getMcpServerVersion())
                .capabilities(ServerCapabilities.builder()
                        .resources(false, false)
                        .tools(true)
                        .prompts(false)
                        // .logging() // Enable logging
                        // .completions() // Enable completions
                        .build())
                .build();
        logger.debug("Built McpSyncServer for {}", mcpConfig.getMcpServerName());

        for (SyncToolSpecification toolSpec : toolSpecifications) {
            mcpSyncServer.addTool(toolSpec);
            logger.trace("Added tool: {}", toolSpec.tool().name());
        }

        String pathSpec = mcpConfig.getServletPathSpec();
        if (null == pathSpec || pathSpec.isBlank()) {
            pathSpec = "/";
        }
        logger.debug("Initial pathSpec: {}", pathSpec);

        if (pathSpec.endsWith("*")) {
            pathSpec = pathSpec.substring(0, pathSpec.length() - 1);
        }

        pathSpec = formatUrlFragment(pathSpec);

        pathSpec = pathSpec + "/*";

        logger.debug("Final pathSpec: {}", pathSpec);

        McpSseServerInstance serverInstance = new McpSseServerInstance(mcpSyncServer, (HttpServlet) transportProvider,
                pathSpec);
        logger.info("Successfully created MCP server instance for server: {}", mcpConfig.getMcpServerName());
        return serverInstance;
    }

    /**
     * Formats a URL fragment by ensuring it starts with a leading slash.
     *
     * @param urlFragment the URL fragment to format
     * @return the formatted URL fragment
     */
    private String formatUrlFragment(String urlFragment) {

        if (null == urlFragment || urlFragment.isBlank()) {
            return "";
        }

        if (!urlFragment.startsWith("/")) {
            urlFragment = "/" + urlFragment;
        }

        if (urlFragment.endsWith("/")) {
            urlFragment = urlFragment.substring(0, urlFragment.length() - 1);
        }
        return urlFragment;
    }

    /*
     * Generates the endpoint URL for the MCP server.
     *
     * @param serverName the name of the MCP server
     * 
     * @param appContextPath the application context path
     * 
     * @param servletPath the servlet path
     * 
     * @param endPoint the endpoint
     * 
     * @return the generated endpoint URL
     */
    private String generateEndPoint(String serverName, String appContextPath, String servletPath, String endPoint) {

        logger.debug("Generating endpoint for server: {} appContextPath: {}, servletPath: {}, endPoint: {}",
                serverName, appContextPath, servletPath, endPoint);

        // Format should be appContextPath/servletPath/sseEndpoint

        if (servletPath.endsWith("*")) {
            servletPath = servletPath.substring(0, servletPath.length() - 1);
        }

        appContextPath = formatUrlFragment(appContextPath);
        servletPath = formatUrlFragment(servletPath);
        endPoint = formatUrlFragment(endPoint);
        logger.trace("Formatted paths - appContextPath: {}, servletPath: {}, sseEndPoint: {}",
                appContextPath, servletPath, endPoint);

        // path/servlet/endpoint

        // endpoint
        // servlet/endpoint
        // path/servlet/endpoint

        String finalEndPointPath = endPoint;

        if (!endPoint.startsWith(appContextPath)) {

            if (!endPoint.startsWith(servletPath)) {
                finalEndPointPath = appContextPath + servletPath + endPoint;
            } else {
                finalEndPointPath = appContextPath + endPoint;
            }
        }
        logger.debug("Final endpoint path: {} for {}", finalEndPointPath, serverName);
        return finalEndPointPath;
    }

}
