package com.vmware;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sumit Deo (deosu@vmware.com)
 */
public class App {

    private final static Logger logger = LoggerFactory.getLogger(App.class);
    public static final String INSTRUMENTATION_LIBRARY_NAME = "instrumentation-library-name";
    public static final String INSTRUMENTATION_VERSION = "1.0.0";
    static Tracer tracer;

    public static void main(String[] args) throws InterruptedException {

        /*
        Tracer must be acquired, which is responsible for creating spans and interacting with the
          Context
         */
        tracer = getTracer();

        // An automated way to propagate the parent span on the current thread
        for (int index = 0; index < 3; index++) {
            /*
            Create a span by specifying the name of the span. The start and end time of the span
            is automatically set by the OpenTelemetry SDK
             */
            Span parentSpan = tracer.spanBuilder("parentSpan" + index).setNoParent().startSpan();
            logger.info("In parent method. TraceID : {}", parentSpan.getSpanContext().getTraceId());

            try {
                // Put the span into the current Context
                parentSpan.makeCurrent();

                /*
                Annotate the span with attributes specific to the represented operation, to
                provide additional context
                 */
                parentSpan.setAttribute("parentIndex", index);

                // Sleep to simulate some work being done before calling `childMethod`
                Thread.sleep(500);
                childMethod(parentSpan, index);

                // Sleep to simulate work being done after `childMethod` returns
                Thread.sleep(500 * index);
            } catch (Throwable throwable) {
                parentSpan.setStatus(StatusCode.ERROR, "Exception message: " + throwable.getMessage());
                return;
            } finally {
                // Closing the scope does not end the span, this has to be done manually
                parentSpan.end();
            }
        }

        // Sleep for a bit to let everything settle
        Thread.sleep(2000);
    }

    private static void childMethod(Span parentSpan, int index) {
        tracer = getTracer();

        // `setParent(...)` is not required, `Span.current()` is automatically added as the parent
        Span childSpan = tracer.spanBuilder("childSpan").setParent(Context.current().with(parentSpan))
                .startSpan();
        logger.info("In child method. TraceID : {}", childSpan.getSpanContext().getTraceId());
        Attributes eventAttrs = Attributes.builder().put("a-key", "a-val").build();
        childSpan.addEvent("child-event", eventAttrs);

        // Put the span into the current Context
        try (Scope scope = childSpan.makeCurrent()) {
            if (index == 1) {
                childSpan.setStatus(StatusCode.ERROR, "Errored (arbitrarily) because index=1");
            }
            Thread.sleep(1000);
        } catch (Throwable throwable) {
            childSpan.setStatus(StatusCode.ERROR, "Something wrong with the child span");
        } finally {
            childSpan.end();
        }
    }

    private static synchronized Tracer getTracer() {
        if (tracer == null) {

            /*
            It is important to initialize your SDK as early as possible in your application's
            lifecycle
             */
            OpenTelemetry openTelemetry = OTelConfig.initOpenTelemetry();

            // Get a tracer
            tracer = openTelemetry.getTracer(INSTRUMENTATION_LIBRARY_NAME, INSTRUMENTATION_VERSION);
        }

        return tracer;
    }
}
