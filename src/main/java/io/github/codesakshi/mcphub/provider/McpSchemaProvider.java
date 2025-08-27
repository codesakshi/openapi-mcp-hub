package io.github.codesakshi.mcphub.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.github.codesakshi.mcphub.config.McpConfig;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;

/**
 * 
 * @author anilalps.dev@gmail.com
 *
 */

/**
 * Provides utilities to generate MCP (Model-Controller-Provider) schema
 * representations
 * from OpenAPI specifications. This class builds input schemas for API
 * operations,
 * supporting request bodies, parameters, queries, and headers, and serializes
 * them
 * into JSON format. The generated schemas are used for describing API inputs in
 * a
 * structured and extensible way, supporting additional metadata and OpenAPI
 * features.
 *
 * <p>
 * Main features:
 * <ul>
 * <li>Builds MCP input schemas from OpenAPI Operation and RequestBody
 * objects.</li>
 * <li>Supports object, array, and primitive types, including nested
 * schemas.</li>
 * <li>Handles OpenAPI references ($ref) and resolves component schemas.</li>
 * <li>Supports marking required fields and adding extra schema properties.</li>
 * <li>Serializes schemas to pretty-printed JSON using Jackson.</li>
 * <li>Provides a wrapper class {@link InputSchemaWrap} for schema
 * metadata.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Usage example:
 * 
 * <pre>
 * McpSchemaProvider provider = new McpSchemaProvider();
 * InputSchemaWrap schemaWrap = provider.buildInputMcpSchema(openApi, operation);
 * String schemaJson = schemaWrap.getSchema();
 * </pre>
 * </p>
 *
 * <p>
 * Internal classes:
 * <ul>
 * <li>{@code McpBaseSchema} - Base class for all MCP schema types.</li>
 * <li>{@code McpObjectSchema} - Represents object schemas with properties.</li>
 * <li>{@code McpArraySchema} - Represents array schemas with item types.</li>
 * <li>{@code McpRequestSchema} - Specialized object schema for request
 * inputs.</li>
 * <li>{@code InputSchemaWrap} - Wrapper for schema JSON and metadata
 * flags.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Dependencies:
 * <ul>
 * <li>Jackson (ObjectMapper, ObjectWriter) for JSON serialization.</li>
 * <li>OpenAPI models (OpenAPI, Operation, RequestBody, Parameter, Schema).</li>
 * <li>SLF4J Logger for logging warnings and errors.</li>
 * </ul>
 * </p>
 */

public class McpSchemaProvider {

	private static final Logger logger = LoggerFactory.getLogger(McpSchemaProvider.class);

	private static ObjectWriter writer;
	static {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		writer = objectMapper.writerWithDefaultPrettyPrinter();
	}

	/**
	 * Base class for MCP schemas.
	 */
	private static class McpBaseSchema {

		/**
		 * The type of the MCP schema. object, array, string etc.
		 */
		private String type;

		public McpBaseSchema(String type) {
			this.type = type;
		}

		public String getType() {
			return type;
		}

		/**
		 * Extra properties for the MCP schema.
		 */
		private Map<String, Object> extra = new HashMap<>();

		/**
		 * Specify required parameters.
		 */
		private List<String> required;

		// JsonAnyGetter will expose all the extra properties as direct properties
		@JsonAnyGetter
		public Map<String, Object> getExtra() {
			return extra;
		}

		public void add(String key, Object value) {
			extra.put(key, value);
		}

		public List<String> getRequired() {
			return required;
		}

		public void addRequired(String required) {

			if (this.required == null) {
				this.required = new ArrayList<>();
			}

			this.required.add(required);
		}

		public void addRequired(List<String> required) {

			if (null == required) {
				return;
			}

			if (required.isEmpty()) {
				return;
			}

			if (this.required == null) {
				this.required = new ArrayList<>();
			}

			this.required.addAll(required);
		}
	}

	/**
	 * Object schema for MCP.
	 */
	private static class McpObjectSchema extends McpBaseSchema {

		private Map<String, McpBaseSchema> properties = new HashMap<>();

		public McpObjectSchema() {
			super("object");
		}

		public Map<String, McpBaseSchema> getProperties() {
			return properties;
		}

		public void addProperty(String name, McpBaseSchema value) {
			properties.put(name, value);
		}

		public void addProperty(String name, McpBaseSchema value, boolean required) {

			properties.put(name, value);

			if (required) {
				addRequired(name);
			}
		}

		@JsonIgnore
		public boolean isEmpty() {
			return properties.isEmpty();
		}

	}

	/**
	 * Array schema for MCP.
	 */
	private static class McpArraySchema extends McpBaseSchema {

		private McpBaseSchema items;

		public McpArraySchema() {
			super("array");
		}

		public McpBaseSchema getItems() {
			return items;
		}

		public void setItems(McpBaseSchema items) {
			this.items = items;
		}

	}

	/**
	 * Request schema for MCP.
	 */
	private static class McpRequestSchema extends McpObjectSchema {

		public McpRequestSchema() {
			super();
		}

		public void setParameters(McpObjectSchema parameters) {
			addProperty("parameters", parameters, true);
		}

		public void setQueries(McpObjectSchema queries) {
			addProperty("queries", queries, true);
		}

		public void setHeaders(McpObjectSchema headers) {
			addProperty("headers", headers, true);
		}

		public void setRequestBody(McpBaseSchema requestBody, boolean isRequired) {
			addProperty("requestBody", requestBody, isRequired);
		}

		@JsonIgnore
		public McpObjectSchema getParemeters() {
			return (McpObjectSchema) getProperties().get("parameters");
		}

		@JsonIgnore
		public McpObjectSchema getQueries() {
			return (McpObjectSchema) getProperties().get("queries");
		}

		@JsonIgnore
		public McpObjectSchema getHeaders() {
			return (McpObjectSchema) getProperties().get("headers");
		}

		@JsonIgnore
		public McpBaseSchema getRequestBody() {
			return getProperties().get("requestBody");
		}
	}

	/**
	 * Wrapper class for input schemas in MCP.
	 */
	public static class InputSchemaWrap {

		/**
		 * The schema definition for the input.
		 */
		private final String schema;

		/**
		 * Indicates if the input has a request body.
		 */
		private final boolean hasRequestBody;

		/**
		 * Indicates if the input has parameters.
		 */
		private final boolean hasParameters;

		/**
		 * Indicates if the input has unique parameters.
		 */
		private final boolean isUniqueParameters;

		public InputSchemaWrap(String schema,
				boolean hasRequestBody,
				boolean hasParameters,
				boolean isUniqueParameters) {

			this.schema = schema;

			this.hasRequestBody = hasRequestBody;
			this.hasParameters = hasParameters;
			this.isUniqueParameters = isUniqueParameters;
		}

		public String getSchema() {
			return schema;
		}

		public boolean isHasRequestBody() {
			return hasRequestBody;
		}

		public boolean isHasParameters() {
			return hasParameters;
		}

		public boolean isUniqueParameters() {
			return isUniqueParameters;
		}
	}

	/**
	 * Builds the input schema for a given OpenAPI operation in the MCP
	 * (Model-Controller-Provider) format.
	 * <p>
	 * This method analyzes the provided OpenAPI operation, including its request
	 * body and parameters,
	 * and constructs a wrapped schema representation suitable for MCP processing.
	 * The resulting schema
	 * includes details about request bodies, parameters (path, query, header), and
	 * their uniqueness.
	 * </p>
	 *
	 * @param openApi   The OpenAPI specification object containing component
	 *                  schemas.
	 * @param operation The OpenAPI operation for which to build the input schema.
	 * @return An {@link InputSchemaWrap} object containing the generated schema as
	 *         a JSON string,
	 *         and flags indicating the presence of request body, parameters, and
	 *         uniqueness of parameters.
	 * @throws RuntimeException if there is an error during schema generation or
	 *                          serialization.
	 */
	public InputSchemaWrap buildInputMcpSchema(McpConfig mcpConfig, OpenAPI openApi,
			Operation operation) {

		logger.info("Building input MCP schema for operation: {}", operation.getOperationId());

		boolean hasRequestBody = false;
		boolean hasParameters = false;

		boolean isUniqueParameters = false;

		McpRequestSchema requestSchema = new McpRequestSchema();

		RequestBody requestBody = operation.getRequestBody();

		// Build request body schema
		if (requestBody != null && requestBody.getContent() != null
				&& requestBody.getContent().get("application/json") != null
				&& requestBody.getContent().get("application/json").getSchema() != null) {

			logger.debug("Processing request body for operation: {}", operation.getOperationId());
			Schema<?> requestBodySchema = requestBody.getContent().get("application/json").getSchema();

			Schema<?> componentSchema = requestBodySchema.get$ref() != null
					? getComponentSchema(openApi, requestBodySchema.get$ref())
					: requestBodySchema;

			McpBaseSchema mcpRequestBodySchema = buildMcpSchema(openApi, componentSchema);

			if (mcpRequestBodySchema != null) {

				boolean isRequired = requestBody.getRequired() != null ? requestBody.getRequired() : false;
				requestSchema.setRequestBody(mcpRequestBodySchema, isRequired);
				hasRequestBody = true;
				logger.debug("Request body schema added. Required: {} for operation: {}", 
                        isRequired, operation.getOperationId());
			}
		}

		// Build parameters schema
		if (operation.getParameters() != null && !operation.getParameters().isEmpty()) {

			logger.debug("Processing parameters for operation: {}", operation.getOperationId());
			hasParameters = true;

			McpObjectSchema parametersSchema = null;
			McpObjectSchema queriesSchema = null;
			McpObjectSchema headersSchema = null;

			Set<String> uniqueNames = new HashSet<>();

			uniqueNames.addAll(operation.getParameters().stream().map(p -> p.getName()).toList());

			isUniqueParameters = uniqueNames.size() == operation.getParameters().size();

			if (isUniqueParameters && mcpConfig.isOptimizeSchema() ) {

				// no duplicates, all 'parameter' will be in single schema

                logger.trace("All parameters are unique for operation: {}", operation.getOperationId());

				if (hasRequestBody) {

					logger.trace("Using single 'parameter' schema for all parameters in operation: {}", operation.getOperationId());

					// Request body present, all parameters will be in separate 'parameters' schema
					// create parameters schema for 'parameters'
					parametersSchema = new McpObjectSchema();
					queriesSchema = parametersSchema;
					headersSchema = parametersSchema;

				} else {

                    logger.trace("Using root request schema for all parameters in operation: {}", operation.getOperationId());
					// No request body, all parameters in root
					parametersSchema = requestSchema;
					queriesSchema = requestSchema;
					headersSchema = requestSchema;
				}

			} else {

                logger.trace("Using separate schemas for parameters, queries and headers in operation: {}", operation.getOperationId());
				// Separate schemas for parameters, queries and headers
				parametersSchema = new McpObjectSchema();
				queriesSchema = new McpObjectSchema();
				headersSchema = new McpObjectSchema();
			}

			for (Parameter parameter : operation.getParameters()) {

				logger.trace("Processing parameter: {}", parameter.getName());
				boolean isRequired = parameter.getRequired() != null ? parameter.getRequired() : false;

				String type = null != parameter.getSchema() ? parameter.getSchema().getType() : "string";

				String description = parameter.getDescription();

				if ("array".equals(type)) {

					type = "string"; // represent array as comma separated string

					description = (description != null)
							? description + " (array. comma separated values)"
							: "array. comma separated values";
				}

				McpBaseSchema paramSchema = new McpBaseSchema(type);

				// Add extra properties
				if (description != null) {
					paramSchema.add("description", description);
				}

				if (parameter.getSchema() != null) {
					extractSchemaData(parameter.getSchema(), paramSchema);
				}

				if ("path".equals(parameter.getIn())) {

					parametersSchema.addProperty(parameter.getName(), paramSchema, isRequired);

				} else if ("query".equals(parameter.getIn())) {

					queriesSchema.addProperty(parameter.getName(), paramSchema, isRequired);

				} else if ("header".equals(parameter.getIn())) {

					headersSchema.addProperty(parameter.getName(), paramSchema, isRequired);

				} else {

					logger.warn("Unsupported parameter in: " + parameter.getIn() + " for parameter: "
							+ parameter.getName());
				}
			}

			if (isUniqueParameters && mcpConfig.isOptimizeSchema() ) {
				// no duplicates, all 'parameter' will be in single schema

				if (hasRequestBody) {
					// Request body present, all parameters will be in separate 'parameters' schema
					// Add 'parameters' schema to request schema
					if (!parametersSchema.isEmpty()) {
						requestSchema.setParameters(parametersSchema);
					}
				}

				// In 'else' condition, no need to add to 'Request schema'
				// Because we are using 'Request schema' for all parameters

			} else {
				// Duplicates or no optimization. we use separate schemas
				if (!parametersSchema.isEmpty()) {
					requestSchema.setParameters(parametersSchema);
				}

				if (!queriesSchema.isEmpty()) {
					requestSchema.setQueries(queriesSchema);
				}

				if (!headersSchema.isEmpty()) {
					requestSchema.setHeaders(headersSchema);
				}
			}
		}

		String schemaStr = null;
		try {
			schemaStr = writer.writeValueAsString(requestSchema);
			logger.trace("Generated schema JSON: {}", schemaStr);
		} catch (Exception e) {
			logger.error("Error generating input schema: " + e.getMessage(), e);
			throw new RuntimeException("Error generating input schema", e);
		}

		InputSchemaWrap inputSchemaWrap = new InputSchemaWrap(schemaStr, hasRequestBody, hasParameters,
				isUniqueParameters);

		logger.info("Successfully built input MCP schema for operation: {}", operation.getOperationId());
		return inputSchemaWrap;
	}

	private void extractSchemaData(Schema<?> schema, McpBaseSchema mcpSchema) {

		if (null == schema || null == mcpSchema) {
			return;
		}

		logger.trace("Extracting schema data for type: {}", schema.getType());

		if (schema.getDescription() != null) {
			mcpSchema.add("description", schema.getDescription());
		}

		if (schema.getFormat() != null) {
			mcpSchema.add("format", schema.getFormat());
		}
		if (schema.getEnum() != null) {
			mcpSchema.add("enum", schema.getEnum());
		}
		if (schema.getDefault() != null) {
			mcpSchema.add("default", schema.getDefault());
		}
	}

	private Schema<?> getComponentSchema(OpenAPI openApi, String ref) {

		logger.debug("Resolving component schema for ref: {}", ref);

		if (ref == null || !ref.startsWith(Components.COMPONENTS_SCHEMAS_REF)) {
			logger.warn("Invalid or non-component schema ref: {}", ref);
			return null;
		}

		String schemaName = ref.substring(Components.COMPONENTS_SCHEMAS_REF.length());
		if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
			logger.warn("No components or schemas found in OpenAPI spec.");
			return null;
		}

		Schema<?> schema = openApi.getComponents().getSchemas().get(schemaName);
		if (schema == null) {
			logger.warn("Component schema not found for name: {}", schemaName);
		}
		return schema;
	}

	private McpBaseSchema buildMcpSchema(OpenAPI openApi, Schema<?> schema) {

		logger.trace("Building MCP schema for schema ref: {}", (schema != null ? schema.get$ref() : "null"));
		McpBaseSchema mcpSchema = null;
		if (null != schema) {

			if (null != schema.get$ref()) {

				logger.trace("Building schema from ref: {}", schema.get$ref());
				mcpSchema = buildMcpSchema(openApi, getComponentSchema(openApi, schema.get$ref()));

			} else if ("object".equals(schema.getType())) {

				logger.trace("Building object schema.");
				McpObjectSchema objSchema = new McpObjectSchema();
				mcpSchema = objSchema;

				// add properties
				if (schema.getProperties() != null) {

					for (String key : schema.getProperties().keySet()) {

						Schema<?> childSchema = schema.getProperties().get(key);
						objSchema.addProperty(key, buildMcpSchema(openApi, childSchema));
					}
				}

				mcpSchema.addRequired(schema.getRequired());

			} else if ("array".equals(schema.getType())) {

				logger.trace("Building array schema.");
				McpArraySchema arrSchema = new McpArraySchema();
				mcpSchema = arrSchema;

				// add items
				if (schema.getItems() != null) {

					arrSchema.setItems(buildMcpSchema(openApi, schema.getItems()));
				}

				mcpSchema.addRequired(schema.getRequired());

			} else {

				logger.trace("Building primitive schema of type: {}", schema.getType());
				mcpSchema = new McpBaseSchema(schema.getType());

			}

			extractSchemaData(schema, mcpSchema);
		}

		return mcpSchema;
	}
}

