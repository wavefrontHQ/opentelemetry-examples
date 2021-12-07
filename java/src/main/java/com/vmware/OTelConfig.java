package com.vmware;

import io.opentelemetry.api.OpenTelemetry;
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
    private static final String SERVICE_NAME = "otel-otlp-example";
    public static final String OTEL_COLLECTOR_ENDPOINT = "http://localhost:4317";


    //Adds a BatchSpanProcessor initialized with OtlpGrpcSpanExporter to the TracerSdkProvider.

    static OpenTelemetry initOpenTelemetry() {
        OtlpGrpcSpanExporter spanExporter = getOtlpGrpcSpanExporter();
        BatchSpanProcessor spanProcessor = getBatchSpanProcessor(spanExporter);
        Resource serviceNameResource = Resource
                .create(Attributes.of(ResourceAttributes.SERVICE_NAME, SERVICE_NAME));
        SdkTracerProvider tracerProvider = getSdkTracerProvider(spanProcessor, serviceNameResource);
        OpenTelemetrySdk openTelemetrySdk = getOpenTelemetrySdk(tracerProvider);
        Runtime.getRuntime().addShutdownHook(new Thread(tracerProvider::shutdown));

        return openTelemetrySdk;
    }

    private static OpenTelemetrySdk getOpenTelemetrySdk(SdkTracerProvider tracerProvider) {
        return OpenTelemetrySdk.builder().setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();
    }

    private static SdkTracerProvider getSdkTracerProvider(BatchSpanProcessor spanProcessor, Resource serviceNameResource) {
        return SdkTracerProvider.builder().addSpanProcessor(spanProcessor)
                .setResource(Resource.getDefault().merge(serviceNameResource)).build();
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
