# OpenAPI MCP Hub

Library acts as a bridge between the OpenAPI Specification (OAS) and the Model Context Protocol (MCP). Its purpose is to let tools that understand MCP "talk to" and use APIs defined by OpenAPI.


## Features

1. Reads OpenAPI Specifications: Support standard OpenAPI file (both .yaml or .json document) as input. Version 3 ( Open API) and version 2 ( Swagger) are supported.

2. Dynamically Creates MCP Tools: For every API endpoint described in the OpenAPI file (e.g., /users/{id} or /products), the library actively generates a new, corresponding MCP Tool. This generated tool is the representation of that specific endpoint in the MCP world.

3. Translates Schemas: The library handles the crucial task of converting data formats. The request and response schemas defined in OpenAPI (which use JSON Schema formats) are translated into a format that the MCP can understand and use. This ensures that the data sent to and received from the API is structured correctly for MCP client tools.

4. Handles Invocation: This is the execution part. When an MCP client tool "invokes" one of the generated MCP Tools, the library's role is to handle this request. It takes the parameters from the MCP client, formats them according to the original OpenAPI specification, sends the request to the real API, receives the response, and then translates that response back into an MCP-compatible format before sending it back to the client.

The library makes OpenAPI-defined APIs appear as native MCP Tools, allowing any MCP-compliant application to easily interact with and use them without needing to understand the underlying OpenAPI standard itself. It acts as an adapter or translator, making different systems interoperable.

## Setup

Include the Maven artifact:

```xml
	
    <!-- https://mvnrepository.com/artifact/io.github.codesakshi/openapi-mcp-hub -->
    <dependency>
        <groupId>io.github.codesakshi</groupId>
        <artifactId>openapi-mcp-hub</artifactId>
        <version>1.0.1</version>
    </dependency>

```

Or include the [JAR](https://mvnrepository.com/artifact/io.github.codesakshi/openapi-mcp-hub/latest) in your project

## Usage Examples

This library provides tools to expose OpenAPI specifications as MCP tools. Here's a general overview of how to use it:

1.  **Setup your configuration:** Create Object of `McpConfig` and set your configuration values.
2.  **Create MCP Tools:** Use `McpApiToolProvider` to generate `McpSseServerInstance` giving `McpConfig` as the input.
3.  **Serve MCP Tools:** Use any servlet implementation to expose your MCP tools.

```java
// Example
import io.github.codesakshi.mcphub.config.McpConfig;
import io.github.codesakshi.mcphub.provider.McpServerProvider;
import io.github.codesakshi.mcphub.provider.McpServerProvider.McpSseServerInstance;

public class Main {
    public static void main(String[] args) {

        // 1. Create McpConfig Specification
        McpConfig mcpConfig = new McpConfig();
        mcpConfig.setMcpServerName("petStore");  // Unique name for the mcp server
        mcpConfig.setApiDocLocation("https://petstore.swagger.io/v2/swagger.json"); // Open API documentation path

        // 2. Using McpServerProvider, create instance of McpSseServerInstance
        McpServerProvider mcpServerProvider = new McpServerProvider();
        McpSseServerInstance mcpSseServer = mcpServerProvider.createMcpSseServerInstance(appContextPath, mcpConfig);

        // 3. McpSseServerInstance will contain a servlet servlet pathspec.
        // Add Servlet to container.

        // Jetty servlet example
        context.addServlet(new ServletHolder(mcpSseServer.getServlet()), mcpSseServer.getPathSpec());

    }
}
```

See [https://github.com/codesakshi/openapi-mcpserver-jetty](https://github.com/codesakshi/openapi-mcpserver-jetty) for sample implementation

## McpConfig configuration specification


- **mcpServerName**: Unique name of the MCP server.
- **mcpServerVersion**: Version of the MCP server (default: "1.0.0").
- **servletPathSpec**: Servlet path specification for the MCP server (default: "/").
- **mcpSseEndPoint**: SSE endpoint for the MCP server (default: "sse").
- **mcpMessageEndPoint**: Message endpoint for the MCP server (default: "message").
- **apiDocLocation**: API documentation location for Swagger or OpenAPI (JSON/YAML). URL and File location are supported.
- **apiHostUrl**: API host URL for the server. Some times 'apiHostUrl' in Api spec will be empty or partially formed. Use this parameter to adjust API host url. (default: "")..
- **apiServerIndex**: API server index to be used from Swagger or OpenAPI spec (default: 0).
- **optimizeSchema**: Specifies the optimization of the schema. Library will try to combine request 'path', 'query' and 'header' parameters to single schema if possible. (default: true).
- **apiAuthorization**: API authorization token to be used when executing REST API calls (default: null).
- **apiConnectionTimeoutMs**: API connection timeout in milliseconds (default: 30000).
- **apiReadTimeoutMs**: API read timeout in milliseconds (default: 10000).
- **apiMaxRetries**: Maximum number of retries for API calls (default: 3).
- **apiRetryDelayMs**: Delay between API call retries in milliseconds (default: 100).


**mcpServerName** and **apiDocLocation** are mandatory configuration parameters.


## Contributing Guidelines

Contributions are welcome! Please follow these steps:

1.  Fork the repository.
2.  Create a new branch for your feature or bug fix.
3.  Make your changes and commit them with clear messages.
4.  Push your changes to your fork.
5.  Create a pull request to the main repository.

## License Information

This project is licensed under the MIT License. See the `LICENSE` file for more details.

## Contact Information/Support

For issues, questions, or support, please open an issue on the [GitHub repository](https://github.com/codesakshi/openapi-mcp-hub/issues).

**Developer:**
*   **Name:** Anilal P S
*   **Email:** anilalps.dev@gmail.com
*   **Organization:** Code Sakshi
*   **GitHub:** [https://github.com/codesakshi](https://github.com/codesakshi)
