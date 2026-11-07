package dev.tsukuyomi.libra.policy;

import dev.tsukuyomi.libra.domain.entity.Policy;
import dev.tsukuyomi.libra.domain.enums.JsIdenfier;
import dev.tsukuyomi.libra.exeception.LibraEvaluationException;
import lombok.RequiredArgsConstructor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.ResourceLimits;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class GraalJSPolicyEvaluator implements IPolicyEvaluator, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(GraalJSPolicyEvaluator.class);

    private static final Engine ENGINE = Engine.newBuilder("js")
            .option("engine.WarnInterpreterOnly", "false")
            .build();

    private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "js-watchdog");
        t.setDaemon(true);
        return t;
    });

    private static final ResourceLimits LIMITS = ResourceLimits.newBuilder()
            .statementLimit(1_000_000, null)
            .build();

    private static final long TIMEOUT_MS = 500;

    @Override
    public Boolean evaluate(Policy policy, JsIdenfier identifier, String inputAttrs) {
        Source source = Source.newBuilder("js", policy.getJS(identifier), policy.getName(identifier) + ".js").buildLiteral();
        Context ctx = Context.newBuilder("js")
                .engine(ENGINE)
                .allowAllAccess(false)
                .resourceLimits(LIMITS)
                .build();
        ctx.initialize("js");
        ScheduledFuture<?> kill = WATCHDOG.schedule(() -> ctx.close(true), TIMEOUT_MS, TimeUnit.MILLISECONDS);
        try {
            ctx.eval(source);
            Value evaluate = getEvaluateFn(ctx);
            Value input = ctx.eval("js", "JSON.parse").execute(inputAttrs);
            Value result = evaluate.execute(input);
            handleNonBooleanResponse(result);
            return result.asBoolean();
        } catch (PolyglotException ex) {
            handlePolyglotException(ex);
        } catch (Exception ex) {
            logger.error("error evaluating policy evaluator", ex);
            throw new LibraEvaluationException("error evaluating policy evaluator", ex);
        } finally {
            kill.cancel(true);
        }
        return null;
    }

    @Override
    public void close() {
        WATCHDOG.shutdownNow();
        ENGINE.close();
    }

    private void handlePolyglotException(PolyglotException ex) {
        if (ex.isCancelled()) {
            logger.error("js execution was cancelled", ex);
            throw new LibraEvaluationException("js execution was cancelled", ex);
        }
        if (ex.isResourceExhausted()) {
            logger.error("js resource exhausted", ex);
            throw new LibraEvaluationException("js resource exhausted", ex);
        }
        throw new LibraEvaluationException("error evaluating policy", ex);
    }

    private void handleNonBooleanResponse(Value result) {
        if (!result.isBoolean()) {
            logger.error("evaluate() must return a boolean");
            throw new LibraEvaluationException("evaluate() must return a boolean");
        }
    }

    private Value getEvaluateFn(Context ctx) {
        Value evaluate = ctx.getBindings("js").getMember("evaluate");
        if (evaluate == null || !evaluate.canExecute()) {
            logger.error("evaluate() function does not exist");
            throw new LibraEvaluationException("evaluate() function does not exist");
        }
        return evaluate;
    }

}
