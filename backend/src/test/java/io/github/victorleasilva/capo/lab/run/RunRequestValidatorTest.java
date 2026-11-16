package io.github.victorleasilva.capo.lab.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.victorleasilva.capo.lab.descriptor.ExperimentDescriptor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * A validação do pedido contra o descritor (A2, passos 3 e 4; T-019): cada regra, cada mensagem, a ordem de
 * {@code errors} e os padrões, com os descritores reais (Q-043) e, para os limites só de mínimo ou só de máximo, um
 * descritor escrito aqui.
 */
@Tag("RF-003")
class RunRequestValidatorTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final RunRequestValidator validator =
            new RunRequestValidator(MAPPER, new MockEnvironment().withProperty("LAB_SEED_PRODUCTS", "10000"));

    private static ExperimentDescriptor real(String id) throws IOException {
        return descriptor(id, Files.readString(Path.of("src/main/resources/lab/experiments", id + ".json")));
    }

    private static ExperimentDescriptor descriptor(String id, String json) {
        return new ExperimentDescriptor(id, "t", "s", "p", json);
    }

    /** Um descritor com um parâmetro de cada forma de limite que os descritores reais não têm. */
    private static ExperimentDescriptor bounds() {
        return descriptor("bounds", """
                {"variants": [{"id": "bad"}, {"id": "good"}],
                 "parameters": [
                   {"name": "atLeast", "type": "integer", "default": 2, "min": 2},
                   {"name": "atMost", "type": "integer", "default": 2, "max": 9},
                   {"name": "between", "type": "integer-set", "default": [3], "min": 3, "max": 7},
                   {"name": "setAtLeast", "type": "integer-set", "default": [2], "min": 2},
                   {"name": "setAtMost", "type": "integer-set", "default": [9], "max": 9}
                 ]}
                """);
    }

    private List<ParameterError> errors(ExperimentDescriptor descriptor, String body) {
        InvalidParameterException e = catchThrowableOfType(
                InvalidParameterException.class, () -> validator.validate(descriptor, MAPPER.readTree(body)));
        assertThat(e).as("400 invalid-parameter para " + body).isNotNull();
        return e.errors();
    }

    private static ParameterError error(String field, String message) {
        return new ParameterError(field, message);
    }

    private List<ParameterError> listIntersection(String body) throws IOException {
        return errors(real("list-intersection"), body);
    }

    // AC-003.2: padrões

    @Test
    void omittedParameterGetsTheDescriptorDefault() throws IOException {
        ValidatedRun run = validator.validate(
                real("list-intersection"),
                MAPPER.readTree("{\"variants\":[\"good\",\"bad\"],\"parameters\":{\"sizes\":[5000,1000]}}"));

        assertThat(run.variants()).containsExactly("good", "bad");
        assertThat(run.parameters())
                .containsExactly(Map.entry("sizes", List.of(5000, 1000)), Map.entry("repetitions", 5));
    }

    @Test
    void withoutParametersEveryDefaultIsUsed() throws IOException {
        // Os padrões de sizes chegam a 20000; com a carga padrão (1000000), nenhuma regra de negócio barra.
        RunRequestValidator fullSeed =
                new RunRequestValidator(MAPPER, new MockEnvironment().withProperty("LAB_SEED_PRODUCTS", "1000000"));
        ValidatedRun run = fullSeed.validate(real("list-intersection"), MAPPER.readTree("{\"variants\":[\"bad\"]}"));

        assertThat(run.parameters())
                .containsExactly(Map.entry("sizes", List.of(1000, 5000, 10000, 20000)), Map.entry("repetitions", 5));
    }

    // AC-T019.2: regras 1 a 5

    @ParameterizedTest
    @ValueSource(strings = {"[]", "1", "\"x\"", "null"})
    void bodyMustBeAnObject(String body) throws IOException {
        assertThat(listIntersection(body)).containsExactly(error("body", "precisa ser um objeto JSON"));
    }

    @Test
    void unknownTopLevelPropertiesAreReportedInRequestOrder() throws IOException {
        assertThat(listIntersection("{\"z\":1,\"variants\":[\"bad\"],\"a\":2}"))
                .containsExactly(error("z", "propriedade desconhecida"), error("a", "propriedade desconhecida"));
    }

    @Test
    void variantsAreRequired() throws IOException {
        assertThat(listIntersection("{}")).containsExactly(error("variants", "obrigatório"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "\"bad\"", "null", "{}"})
    void variantsMustBeANonEmptyList(String variants) throws IOException {
        assertThat(listIntersection("{\"variants\":" + variants + "}"))
                .containsExactly(error("variants", "precisa ser uma lista com ao menos uma versão"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"[\"worst\"]", "[\"bad\",1]", "[\"bad\",null]", "[\"BAD\"]"})
    void variantsMustExistInTheDescriptor(String variants) throws IOException {
        assertThat(listIntersection("{\"variants\":" + variants + "}"))
                .containsExactly(
                        error("variants", "versão desconhecida; as versões deste experimento são bad, medium, good"));
    }

    @Test
    void variantsCannotRepeat() throws IOException {
        assertThat(listIntersection("{\"variants\":[\"bad\",\"good\",\"bad\"]}"))
                .containsExactly(error("variants", "versão repetida"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "1", "\"x\"", "null"})
    void parametersMustBeAnObject(String parameters) throws IOException {
        assertThat(listIntersection("{\"variants\":[\"bad\"],\"parameters\":" + parameters + "}"))
                .containsExactly(error("parameters", "precisa ser um objeto"));
    }

    @Test
    void unknownParameter() throws IOException {
        assertThat(listIntersection("{\"variants\":[\"bad\"],\"parameters\":{\"foo\":1}}"))
                .containsExactly(error("foo", "parâmetro desconhecido"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.5", "3.0", "\"3\"", "true", "null", "[3]", "3000000000"})
    void integerMustBeAnInt(String value) throws IOException {
        assertThat(listIntersection("{\"variants\":[\"bad\"],\"parameters\":{\"repetitions\":" + value + "}}"))
                .containsExactly(error("repetitions", "precisa ser um inteiro"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 11, -1})
    void integerOutsideMinAndMax(int value) throws IOException {
        assertThat(listIntersection("{\"variants\":[\"bad\"],\"parameters\":{\"repetitions\":" + value + "}}"))
                .containsExactly(error("repetitions", "precisa estar entre 1 e 10"));
    }

    @Test
    void integerAtTheBoundsIsAccepted() throws IOException {
        for (int value : new int[] {1, 10}) {
            ValidatedRun run = validator.validate(
                    real("list-intersection"),
                    MAPPER.readTree("{\"variants\":[\"bad\"],\"parameters\":{\"sizes\":[1000],\"repetitions\":" + value
                            + "}}"));
            assertThat(run.parameters()).containsEntry("repetitions", value);
        }
    }

    @Test
    void integerOutsideAllowed() throws IOException {
        assertThat(errors(
                        real("flash-sale-last-units"), "{\"variants\":[\"good\"],\"parameters\":{\"raceWindowMs\":2}}"))
                .containsExactly(error("raceWindowMs", "precisa ser um destes valores: 0, 1, 5"));
    }

    @Test
    void integerWithOnlyOneBound() {
        assertThat(errors(bounds(), "{\"variants\":[\"bad\"],\"parameters\":{\"atLeast\":1,\"atMost\":10}}"))
                .containsExactly(
                        error("atLeast", "precisa ser no mínimo 2"), error("atMost", "precisa ser no máximo 9"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "\"x\"", "1000", "null", "{}"})
    void integerSetMustBeANonEmptyList(String value) throws IOException {
        assertThat(listIntersection("{\"variants\":[\"bad\"],\"parameters\":{\"sizes\":" + value + "}}"))
                .containsExactly(error("sizes", "precisa ser uma lista com ao menos um valor"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"[1000,1.5]", "[\"1000\"]", "[1000,null]", "[true]", "[3000000000]"})
    void integerSetMustHoldOnlyInts(String value) throws IOException {
        assertThat(listIntersection("{\"variants\":[\"bad\"],\"parameters\":{\"sizes\":" + value + "}}"))
                .containsExactly(error("sizes", "precisa ter só inteiros"));
    }

    @Test
    void integerSetCannotRepeat() throws IOException {
        assertThat(listIntersection("{\"variants\":[\"bad\"],\"parameters\":{\"sizes\":[1000,5000,1000]}}"))
                .containsExactly(error("sizes", "valor repetido"));
    }

    @Test
    void integerSetOutsideAllowed() throws IOException {
        assertThat(listIntersection("{\"variants\":[\"bad\"],\"parameters\":{\"sizes\":[1000,1234]}}"))
                .containsExactly(
                        error("sizes", "precisa ter só valores destes: 1000, 5000, 10000, 20000, 100000, 1000000"));
    }

    @Test
    void integerSetOutsideMinAndMax() {
        assertThat(errors(
                        bounds(),
                        "{\"variants\":[\"bad\"],\"parameters\":"
                                + "{\"between\":[3,8],\"setAtLeast\":[1],\"setAtMost\":[10]}}"))
                .containsExactly(
                        error("between", "precisa ter só valores entre 3 e 7"),
                        error("setAtLeast", "precisa ter só valores no mínimo 2"),
                        error("setAtMost", "precisa ter só valores no máximo 9"));
    }

    /** AC-T019.2: um erro por campo, todos os campos conferidos, na ordem fixa. */
    @Test
    void errorsKeepTheFixedOrder() throws IOException {
        assertThat(listIntersection(
                        "{\"x\":1,\"variants\":[\"bad\",\"bad\"],\"parameters\":{\"repetitions\":1.5,\"foo\":1}}"))
                .containsExactly(
                        error("x", "propriedade desconhecida"),
                        error("variants", "versão repetida"),
                        error("repetitions", "precisa ser um inteiro"),
                        error("foo", "parâmetro desconhecido"));
    }

    @Test
    void parametersAreCheckedInRequestOrder() throws IOException {
        assertThat(listIntersection("{\"variants\":[\"bad\"],\"parameters\":{\"repetitions\":0,\"sizes\":[]}}"))
                .containsExactly(
                        error("repetitions", "precisa estar entre 1 e 10"),
                        error("sizes", "precisa ser uma lista com ao menos um valor"));
    }

    // Regras de negócio (os dois 422)

    @Test
    void sizeAboveTheVariantLimit() throws IOException {
        SizeExceedsVariantLimitException e = catchThrowableOfType(
                SizeExceedsVariantLimitException.class,
                () -> validator.validate(
                        real("list-intersection"),
                        MAPPER.readTree("{\"variants\":[\"good\",\"bad\"],\"parameters\":{\"sizes\":[1000,100000]}}")));

        assertThat(e).isNotNull();
        assertThat(e.variant()).isEqualTo("bad");
        assertThat(e.maxSize()).isEqualTo(20000);
        assertThat(e.size()).isEqualTo(100000);
    }

    @Test
    void sizeAtTheVariantLimitButAboveTheSeedIsInsufficientData() {
        InsufficientDataException e = catchThrowableOfType(
                InsufficientDataException.class,
                () -> validator.validate(
                        real("list-intersection"),
                        MAPPER.readTree("{\"variants\":[\"bad\"],\"parameters\":{\"sizes\":[20000]}}")));

        assertThat(e).isNotNull();
        assertThat(e.size()).isEqualTo(20000);
        assertThat(e.seedProducts()).isEqualTo(10000);
    }

    @Test
    void sizeEqualToTheSeedIsAccepted() throws IOException {
        ValidatedRun run = validator.validate(
                real("list-intersection"),
                MAPPER.readTree("{\"variants\":[\"bad\",\"good\"],\"parameters\":{\"sizes\":[10000]}}"));

        assertThat(run.parameters()).containsEntry("sizes", List.of(10000));
    }

    @Test
    void theVariantLimitComesBeforeInsufficientData() {
        assertThatThrownBy(() -> validator.validate(
                        real("list-intersection"),
                        MAPPER.readTree("{\"variants\":[\"bad\"],\"parameters\":{\"sizes\":[100000]}}")))
                .isInstanceOf(SizeExceedsVariantLimitException.class);
    }

    @Test
    void experimentWithoutSizeHasNoBusinessRule() throws IOException {
        JsonNode body = MAPPER.readTree("{\"variants\":[\"bad\",\"good\"],\"parameters\":{\"buyers\":2000}}");

        ValidatedRun run = validator.validate(real("flash-sale-last-units"), body);

        assertThat(run.parameters())
                .containsExactly(
                        Map.entry("initialStock", 10), Map.entry("buyers", 2000), Map.entry("raceWindowMs", 1));
    }
}
