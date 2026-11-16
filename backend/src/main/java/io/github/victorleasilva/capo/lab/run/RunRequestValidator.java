package io.github.victorleasilva.capo.lab.run;

import io.github.victorleasilva.capo.lab.descriptor.ExperimentDescriptor;
import io.github.victorleasilva.capo.lab.seed.SeedSettings;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Valida o pedido de execução contra o descritor (A2, passos 3 e 4), antes da trava e de qualquer gravação. Um erro
 * por campo, na ordem fixa: propriedades desconhecidas do corpo, {@code variants}, {@code parameters} e cada parâmetro
 * na ordem do pedido. Depois, as regras de negócio: o limite de tamanho de cada versão e os produtos carregados.
 */
@Component
public final class RunRequestValidator {

    private static final String VARIANTS = "variants";
    private static final String PARAMETERS = "parameters";

    private final JsonMapper mapper;
    private final Environment environment;

    public RunRequestValidator(JsonMapper mapper, Environment environment) {
        this.mapper = mapper;
        this.environment = environment;
    }

    /**
     * Devolve as versões e os parâmetros efetivos, ou lança {@link InvalidParameterException},
     * {@link SizeExceedsVariantLimitException} ou {@link InsufficientDataException}.
     */
    public ValidatedRun validate(ExperimentDescriptor descriptor, JsonNode body) {
        if (body == null || !body.isObject()) {
            throw new InvalidParameterException(List.of(new ParameterError("body", "precisa ser um objeto JSON")));
        }
        JsonNode document = mapper.readTree(descriptor.json());
        List<ParameterError> errors = new ArrayList<>();

        for (String name : body.propertyNames()) {
            if (!VARIANTS.equals(name) && !PARAMETERS.equals(name)) {
                errors.add(new ParameterError(name, "propriedade desconhecida"));
            }
        }
        List<String> variants = variants(document, body, errors);
        Map<String, JsonNode> definitions = new LinkedHashMap<>();
        for (JsonNode parameter : document.get(PARAMETERS)) {
            definitions.put(parameter.get("name").asString(), parameter);
        }
        Map<String, Object> requested = parameters(definitions, body, errors);
        if (!errors.isEmpty()) {
            throw new InvalidParameterException(errors);
        }

        Map<String, Object> effective = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> definition : definitions.entrySet()) {
            String name = definition.getKey();
            effective.put(
                    name,
                    requested.containsKey(name)
                            ? requested.get(name)
                            : value(definition.getValue().get("default")));
        }
        businessRules(document, definitions, variants, effective);
        return new ValidatedRun(variants, effective);
    }

    private static List<String> variants(JsonNode document, JsonNode body, List<ParameterError> errors) {
        if (!body.has(VARIANTS)) {
            errors.add(new ParameterError(VARIANTS, "obrigatório"));
            return List.of();
        }
        JsonNode requested = body.get(VARIANTS);
        if (!requested.isArray() || requested.isEmpty()) {
            errors.add(new ParameterError(VARIANTS, "precisa ser uma lista com ao menos uma versão"));
            return List.of();
        }
        List<String> known = new ArrayList<>();
        document.get(VARIANTS).forEach(v -> known.add(v.get("id").asString()));
        List<String> variants = new ArrayList<>();
        for (JsonNode item : requested) {
            if (!item.isString() || !known.contains(item.asString())) {
                errors.add(new ParameterError(
                        VARIANTS, "versão desconhecida; as versões deste experimento são " + String.join(", ", known)));
                return List.of();
            }
            variants.add(item.asString());
        }
        if (new HashSet<>(variants).size() != variants.size()) {
            errors.add(new ParameterError(VARIANTS, "versão repetida"));
            return List.of();
        }
        return variants;
    }

    private static Map<String, Object> parameters(
            Map<String, JsonNode> definitions, JsonNode body, List<ParameterError> errors) {
        Map<String, Object> requested = new LinkedHashMap<>();
        if (!body.has(PARAMETERS)) {
            return requested;
        }
        JsonNode parameters = body.get(PARAMETERS);
        if (!parameters.isObject()) {
            errors.add(new ParameterError(PARAMETERS, "precisa ser um objeto"));
            return requested;
        }
        for (Map.Entry<String, JsonNode> entry : parameters.properties()) {
            String name = entry.getKey();
            JsonNode definition = definitions.get(name);
            String error;
            if (definition == null) {
                error = "parâmetro desconhecido";
            } else if ("integer-set".equals(definition.get("type").asString())) {
                error = integerSetError(definition, entry.getValue());
            } else {
                error = integerError(definition, entry.getValue());
            }
            if (error != null) {
                errors.add(new ParameterError(name, error));
            } else {
                requested.put(name, value(entry.getValue()));
            }
        }
        return requested;
    }

    private static String integerError(JsonNode definition, JsonNode value) {
        if (!isInt(value)) {
            return "precisa ser um inteiro";
        }
        int number = value.intValue();
        if (definition.has("allowed") && !allowed(definition).contains(number)) {
            return "precisa ser um destes valores: " + join(allowed(definition));
        }
        if (outOfBounds(definition, number)) {
            boolean both = definition.has("min") && definition.has("max");
            return both ? "precisa estar " + bounds(definition) : "precisa ser " + bounds(definition);
        }
        return null;
    }

    private static String integerSetError(JsonNode definition, JsonNode value) {
        if (!value.isArray() || value.isEmpty()) {
            return "precisa ser uma lista com ao menos um valor";
        }
        List<Integer> numbers = new ArrayList<>();
        for (JsonNode item : value) {
            if (!isInt(item)) {
                return "precisa ter só inteiros";
            }
            numbers.add(item.intValue());
        }
        if (new HashSet<>(numbers).size() != numbers.size()) {
            return "valor repetido";
        }
        if (definition.has("allowed") && !allowed(definition).containsAll(numbers)) {
            return "precisa ter só valores destes: " + join(allowed(definition));
        }
        if (numbers.stream().anyMatch(number -> outOfBounds(definition, number))) {
            return "precisa ter só valores " + bounds(definition);
        }
        return null;
    }

    private static boolean outOfBounds(JsonNode definition, int number) {
        return (definition.has("min") && number < definition.get("min").asInt())
                || (definition.has("max") && number > definition.get("max").asInt());
    }

    /** O limite em texto: "entre 1 e 10", "no mínimo 2" ou "no máximo 9". */
    private static String bounds(JsonNode definition) {
        if (definition.has("min") && definition.has("max")) {
            return "entre " + definition.get("min").asInt() + " e "
                    + definition.get("max").asInt();
        }
        return definition.has("min")
                ? "no mínimo " + definition.get("min").asInt()
                : "no máximo " + definition.get("max").asInt();
    }

    private static boolean isInt(JsonNode value) {
        return value.isIntegralNumber() && value.canConvertToInt();
    }

    private static List<Integer> allowed(JsonNode definition) {
        List<Integer> allowed = new ArrayList<>();
        definition.get("allowed").forEach(v -> allowed.add(v.intValue()));
        return allowed;
    }

    private static String join(List<Integer> values) {
        return values.stream().map(String::valueOf).collect(Collectors.joining(", "));
    }

    /** O valor já validado como {@code Integer} ou {@code List<Integer>}, na ordem em que veio. */
    private static Object value(JsonNode node) {
        if (node.isArray()) {
            List<Integer> values = new ArrayList<>();
            node.forEach(v -> values.add(v.intValue()));
            return List.copyOf(values);
        }
        return node.intValue();
    }

    /** O passo 4 do A2: o limite de cada versão e, depois, os produtos carregados. */
    private void businessRules(
            JsonNode document,
            Map<String, JsonNode> definitions,
            List<String> variants,
            Map<String, Object> effective) {
        String sizeParameter = null;
        for (Map.Entry<String, JsonNode> definition : definitions.entrySet()) {
            if (definition.getValue().path("isSize").asBoolean(false)) {
                sizeParameter = definition.getKey();
            }
        }
        if (sizeParameter == null) {
            return;
        }
        int size = 0;
        for (Object value : (List<?>) effective.get(sizeParameter)) {
            size = Math.max(size, (Integer) value);
        }
        Map<String, JsonNode> byId = new LinkedHashMap<>();
        document.get(VARIANTS).forEach(v -> byId.put(v.get("id").asString(), v));
        for (String variant : variants) {
            JsonNode definition = byId.get(variant);
            if (definition.has("maxSize") && size > definition.get("maxSize").asInt()) {
                throw new SizeExceedsVariantLimitException(
                        variant, definition.get("maxSize").asInt(), size);
            }
        }
        int seedProducts = SeedSettings.parse(environment.getProperty(SeedSettings.KEY));
        if (size > seedProducts) {
            throw new InsufficientDataException(size, seedProducts);
        }
    }
}
