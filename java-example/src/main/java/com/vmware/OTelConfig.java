package com.vmware;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

import java.util.concurrent.TimeUnit;

/**
 * @author Sumit Deo (deosu@vmware.com)
 */
public class OTelConfig {
    private static final String SERVICE_NAME_VALUE = "otel-otlp-java-service";
    public static final String OTEL_COLLECTOR_ENDPOINT = "http://localhost:4317";
    public static final String APP_NAME_VALUE = "otel-otlp-java-app";
    public static final String SERVICE_NAME_KEY = "service.name";
    public static final String APP_NAME_KEY = "application";


    //Adds a BatchSpanProcessor initialized with OtlpGrpcSpanExporter to the TracerSdkProvider.

    static OpenTelemetry initOpenTelemetry() {
        OtlpGrpcSpanExporter spanExporter = getOtlpGrpcSpanExporter();
        BatchSpanProcessor spanProcessor = getBatchSpanProcessor(spanExporter);
        SdkTracerProvider tracerProvider = getSdkTracerProvider(spanProcessor);
        OpenTelemetrySdk openTelemetrySdk = getOpenTelemetrySdk(tracerProvider);
        Runtime.getRuntime().addShutdownHook(new Thread(tracerProvider::shutdown));

        return openTelemetrySdk;
    }

    public static Resource resource() {
        return Resource.getDefault().merge(Resource
                .create(Attributes.builder().put(SERVICE_NAME_KEY, SERVICE_NAME_VALUE).put(APP_NAME_KEY, APP_NAME_VALUE).build()));
    }

    private static OpenTelemetrySdk getOpenTelemetrySdk(SdkTracerProvider tracerProvider) {
        return OpenTelemetrySdk.builder().setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();
    }

    private static SdkTracerProvider getSdkTracerProvider(BatchSpanProcessor spanProcessor) {
        return SdkTracerProvider.builder().addSpanProcessor(spanProcessor)
                .setResource(resource()).build();
    }

    private static BatchSpanProcessor getBatchSpanProcessor(OtlpGrpcSpanExporter spanExporter) {
        return BatchSpanProcessor.builder(spanExporter)
                .setScheduleDelay(100, TimeUnit.MILLISECONDS).build();
    }

    private static OtlpGrpcSpanExporter getOtlpGrpcSpanExporter() {
        return OtlpGrpcSpanExporter.builder()
                .setEndpoint(OTEL_COLLECTOR_ENDPOINT)
                .setTimeout(2, TimeUnit.SECONDS)
                .build();
    }
}
