package dev.tsukuyomi.libra.engine;

import dev.tsukuyomi.libra.domain.entity.Policy;
import dev.tsukuyomi.libra.domain.enums.JsIdenfier;
import dev.tsukuyomi.libra.domain.enums.PolicyStatus;
import dev.tsukuyomi.libra.exeception.LibraEvaluationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class GraalJSPolicyEngineTest {

    // NOTE: never call evaluator.close() here — it closes the static, shared ENGINE.
    private final GraalJSPolicyEngine evaluator = new GraalJSPolicyEngine();

    private static Policy policy(String appliesJs, String policyJs) {
        return Policy.builder()
                .id(1L)
                .tenantID(10L)
                .scopeID(20L)
                .entityID("user-1")
                .code("credit-limit")
                .appliedJs(appliesJs)
                .evaluatedJs(policyJs)
                .status(PolicyStatus.ACTIVE)
                .version(1L)
                .build();
    }

    private static Policy policyJs(String js) {
        return policy("function evaluate(input) { return true; }", js);
    }

    private Boolean eval(String js, String input) {
        return evaluator.evaluate(policyJs(js), JsIdenfier.POLICY, input);
    }

    @Nested
    class HappyPath {

        @Test
        void returnsTrue() {
            assertThat(eval("function evaluate(input) { return true; }", "{}")).isTrue();
        }

        @Test
        void returnsFalse() {
            assertThat(eval("function evaluate(input) { return false; }", "{}")).isFalse();
        }

        @Test
        void readsInputAttributes() {
            String js = "function evaluate(input) { return input.amount <= input.limit; }";

            assertThat(eval(js, "{\"amount\": 50, \"limit\": 100}")).isTrue();
            assertThat(eval(js, "{\"amount\": 150, \"limit\": 100}")).isFalse();
        }

        @Test
        void readsNestedAndArrayAttributes() {
            String js = """
                    function evaluate(input) {
                      return input.user.roles.includes('admin') && input.user.age >= 18;
                    }
                    """;

            assertThat(eval(js, "{\"user\": {\"roles\": [\"viewer\", \"admin\"], \"age\": 30}}")).isTrue();
            assertThat(eval(js, "{\"user\": {\"roles\": [\"viewer\"], \"age\": 30}}")).isFalse();
        }

        @Test
        void supportsHelperFunctionsAndArrowFunctions() {
            String js = """
                    const isHighValue = (amt) => amt > 1000;
                    function evaluate(input) { return !isHighValue(input.amount); }
                    """;

            assertThat(eval(js, "{\"amount\": 10}")).isTrue();
            assertThat(eval(js, "{\"amount\": 10000}")).isFalse();
        }

        @Test
        void usesAppliedJsForAppliesIdentifier() {
            Policy p = policy(
                    "function evaluate(input) { return input.kind === 'payment'; }",
                    "function evaluate(input) { return false; }");

            assertThat(evaluator.evaluate(p, JsIdenfier.APPLIES, "{\"kind\": \"payment\"}")).isTrue();
            assertThat(evaluator.evaluate(p, JsIdenfier.APPLIES, "{\"kind\": \"refund\"}")).isFalse();
        }

        @Test
        void usesEvaluatedJsForPolicyIdentifier() {
            Policy p = policy(
                    "function evaluate(input) { return true; }",
                    "function evaluate(input) { return false; }");

            assertThat(evaluator.evaluate(p, JsIdenfier.POLICY, "{}")).isFalse();
        }

        @Test
        void stateDoesNotLeakBetweenEvaluations() {
            String js = """
                    var counter = (typeof counter === 'undefined') ? 0 : counter;
                    function evaluate(input) { counter++; return counter === 1; }
                    """;

            assertThat(eval(js, "{}")).isTrue();
            assertThat(eval(js, "{}")).isTrue();
        }
    }

    @Nested
    class InvalidScripts {

        @Test
        void missingEvaluateFunctionThrows() {
            assertThatThrownBy(() -> eval("function notEvaluate() { return true; }", "{}"))
                    .isInstanceOf(LibraEvaluationException.class)
                    .satisfies(ex -> assertThat(rootMessages(ex)).contains("evaluate() function does not exist"));
        }

        @Test
        void evaluateNotAFunctionThrows() {
            assertThatThrownBy(() -> eval("var evaluate = 42;", "{}"))
                    .isInstanceOf(LibraEvaluationException.class)
                    .satisfies(ex -> assertThat(rootMessages(ex)).contains("evaluate() function does not exist"));
        }

        @Test
        void nonBooleanResultThrows() {
            assertThatThrownBy(() -> eval("function evaluate(input) { return 'yes'; }", "{}"))
                    .isInstanceOf(LibraEvaluationException.class)
                    .satisfies(ex -> assertThat(rootMessages(ex)).contains("evaluate() must return a boolean"));
        }

        @Test
        void truthyNumberIsNotCoercedToBoolean() {
            assertThatThrownBy(() -> eval("function evaluate(input) { return 1; }", "{}"))
                    .isInstanceOf(LibraEvaluationException.class);
        }

        @Test
        void undefinedResultThrows() {
            assertThatThrownBy(() -> eval("function evaluate(input) { }", "{}"))
                    .isInstanceOf(LibraEvaluationException.class);
        }

        @Test
        void syntaxErrorThrows() {
            assertThatThrownBy(() -> eval("function evaluate(input) { return true; ", "{}"))
                    .isInstanceOf(LibraEvaluationException.class)
                    .hasMessage("error evaluating policy");
        }

        @Test
        void runtimeErrorInScriptThrows() {
            assertThatThrownBy(() -> eval("function evaluate(input) { throw new Error('boom'); }", "{}"))
                    .isInstanceOf(LibraEvaluationException.class)
                    .hasMessage("error evaluating policy");
        }

        @Test
        void accessingUndefinedPropertyThrows() {
            assertThatThrownBy(() -> eval("function evaluate(input) { return input.a.b.c; }", "{}"))
                    .isInstanceOf(LibraEvaluationException.class);
        }

        @Test
        void malformedInputJsonThrows() {
            assertThatThrownBy(() -> eval("function evaluate(input) { return true; }", "{not json"))
                    .isInstanceOf(LibraEvaluationException.class);
        }
    }

    @Nested
    class Sandbox {

        @Test
        void hostJavaAccessIsDenied() {
            String js = """
                    function evaluate(input) {
                      Java.type('java.lang.System').exit(1);
                      return true;
                    }
                    """;

            assertThatThrownBy(() -> eval(js, "{}")).isInstanceOf(LibraEvaluationException.class);
        }

        @Test
        void javaGlobalIsNotExposed() {
            assertThat(eval("function evaluate(input) { return typeof Java === 'undefined'; }", "{}")).isTrue();
        }

        @Test
        void requireIsNotAvailable() {
            assertThat(eval("function evaluate(input) { return typeof require === 'undefined'; }", "{}")).isTrue();
        }
    }

    @Nested
    class ResourceLimits {

        @Test
        void infiniteLoopIsTerminated() {
            String js = "function evaluate(input) { while (true) {} }";

            assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                    assertThatThrownBy(() -> eval(js, "{}"))
                            .isInstanceOf(LibraEvaluationException.class)
                            .hasMessageMatching("js (resource exhausted|execution was cancelled)"));
        }

        @Test
        void infiniteLoopAtTopLevelIsTerminated() {
            String js = "while (true) {} function evaluate(input) { return true; }";

            assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                    assertThatThrownBy(() -> eval(js, "{}"))
                            .isInstanceOf(LibraEvaluationException.class)
                            .hasMessageMatching("js (resource exhausted|execution was cancelled)"));
        }

        @Test
        void evaluatorStillWorksAfterATerminatedScript() {
            assertThatThrownBy(() -> eval("function evaluate(input) { while (true) {} }", "{}"))
                    .isInstanceOf(LibraEvaluationException.class);

            assertThat(eval("function evaluate(input) { return true; }", "{}")).isTrue();
        }
    }

    @Nested
    class Concurrency {

        @Test
        void concurrentEvaluationsAreIsolated() throws Exception {
            String js = "function evaluate(input) { return input.n % 2 === 0; }";
            ExecutorService pool = Executors.newFixedThreadPool(8);
            try {
                var tasks = IntStream.range(0, 64)
                        .mapToObj(n -> (Callable<Boolean>) () -> eval(js, "{\"n\": " + n + "}"))
                        .toList();
                var futures = pool.invokeAll(tasks);
                for (int n = 0; n < futures.size(); n++) {
                    Future<Boolean> f = futures.get(n);
                    assertThat(f.get()).as("n=%d", n).isEqualTo(n % 2 == 0);
                }
            } finally {
                pool.shutdownNow();
            }
        }
    }

    private static String rootMessages(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        for (Throwable t = ex; t != null; t = t.getCause()) {
            sb.append(t.getMessage()).append('\n');
        }
        return sb.toString();
    }
}
