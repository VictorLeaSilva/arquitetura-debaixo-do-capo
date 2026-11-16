package io.github.victorleasilva.capo.lab.web;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * O contrato OpenAPI da API do laboratório ({@code openapi/lab-api.yaml}) usado como verificador nos testes (Q-038):
 * os schemas de um OpenAPI 3.1 são JSON Schema 2020-12, então a mesma biblioteca dos descritores valida os corpos.
 */
public final class OpenApiContract {

    static final String DOCUMENT = "classpath:openapi/lab-api.yaml";
    private static final String DESCRIPTOR_SCHEMA_ID = "urn:capo:experiment-descriptor:1";

    private final SchemaRegistry registry;
    private final JsonNode document;

    public OpenApiContract() {
        // O $ref do 200 do detalhe aponta para o schema dos descritores pelo $id dele, sem cópia.
        String descriptorSchema = read("/lab/experiments/experiment.schema.json");
        this.registry = SchemaRegistry.withDefaultDialect(
                SpecificationVersion.DRAFT_2020_12,
                builder -> builder.schemas(Map.of(DESCRIPTOR_SCHEMA_ID, descriptorSchema)));
        this.document = YAMLMapper.builder().build().readTree(read("/openapi/lab-api.yaml"));
    }

    JsonNode document() {
        return document;
    }

    /** Os erros de validação de {@code body} contra o schema da resposta {@code status} de {@code method path}. */
    public List<String> validate(String path, String method, int status, String mediaType, JsonNode body) {
        String pointer = "/paths/" + escape(path) + "/" + method + "/responses/" + status + "/content/"
                + escape(mediaType) + "/schema";
        Schema schema = registry.getSchema(SchemaLocation.of(DOCUMENT + "#" + pointer));
        List<String> errors = new ArrayList<>();
        for (Error error : schema.validate(body)) {
            errors.add(error.toString());
        }
        return errors;
    }

    /** Escapa um trecho de JSON Pointer (RFC 6901): "~" vira "~0" e "/" vira "~1". */
    private static String escape(String segment) {
        return segment.replace("~", "~0").replace("/", "~1");
    }

    private static String read(String resource) {
        try (InputStream in = OpenApiContract.class.getResourceAsStream(resource)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
