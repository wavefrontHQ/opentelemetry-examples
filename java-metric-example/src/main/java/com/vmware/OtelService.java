package com.vmware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.metrics.internal.view.ExplicitBucketHistogramAggregation;
import io.opentelemetry.sdk.metrics.internal.view.ExponentialHistogramAggregation;
import io.opentelemetry.sdk.resources.Resource;

/**
 * @author Sumit Deo (deosu@vmware.com)
 */
public class OtelService {

  final ConfigCache cache;
  private final static Logger logger = LoggerFactory.getLogger(OtelService.class);
  public OtelService() {
    cache = ConfigCache.getInstance();
    logger.info("Cached configuration for " + cache.getProperty("description"));
  }

  private Resource resource() {
    logger.info("Loading resources.");
    return Resource.getDefault().merge(Resource
        .create(Attributes.builder()
            .put("application", cache.getProperty("application"))
            .put("service.name", cache.getProperty("service.name"))
            .build()));
  }

  public LongHistogram initHistogramMetricRecorder() {
    AggregationTemporalitySelector temporalitySelector;
    if (cache.getProperty("aggregation.temporality").equals("delta")) {
      temporalitySelector = AggregationTemporalitySelector.deltaPreferred();
      logger.info("Delta aggregation temporality selected.");
    }
    else {
      temporalitySelector = AggregationTemporalitySelector.alwaysCumulative();
      logger.info("Cumulative aggregation temporality selected.");
    }

    logger.info("Initializing OtlpGrpcMetricExporter.");
    OtlpGrpcMetricExporter metricExporter = OtlpGrpcMetricExporter.builder()
        .setEndpoint(cache.getProperty("otel.exporter.otlp.endpoint"))
        .setAggregationTemporalitySelector(temporalitySelector)
        .build();

    logger.info("Initializing PeriodicMetricReader.");
    MetricReader periodicReader =
        PeriodicMetricReader.builder(metricExporter).setInterval(Duration.ofMillis(1000)).build();

    logger.info("Initializing SdkMeterProviderBuilder.");
    SdkMeterProviderBuilder builder = SdkMeterProvider.builder()
        .setResource(resource())
        .registerMetricReader(periodicReader);

    logger.info("Initializing InstrumentSelector with type Histogram.");
    InstrumentSelector selector =
        InstrumentSelector.builder().setType(InstrumentType.HISTOGRAM).build();

    View view;
    if (cache.getProperty("metric.sub.type").equals("exponential")) {
      view = View.builder()
          .setAggregation(ExponentialHistogramAggregation.create(Integer.parseInt(cache.getProperty("scale")),
              Integer.parseInt(cache.getProperty("max.buckets"))))
          .build();
      logger.info("Initializing View with aggregation ExponentialHistogramAggregation. " +
          "MaximumBucketSize: " + cache.getProperty("max.buckets") + " and Scale: " + cache.getProperty("scale"));
    } else {
      String bucketBoundaries = cache.getProperty("metric.bucket.boundaries");
      List<Double> boundaries = new ArrayList<>();

      for (String str : bucketBoundaries.split(",")) {
        boundaries.add(Double.valueOf(str));
      }

      view = View.builder()
          .setAggregation(ExplicitBucketHistogramAggregation.create(boundaries))
          .build();
      logger.info("Initializing View with aggregation ExplicitBucketHistogramAggregation. " +
          "ExplicitBucketCounts: " + boundaries.size());
    }

    logger.info("Initializing SdkMeterProvider.");
    SdkMeterProvider sdkMeterProvider = builder
        .registerView(selector, view)
        .build();

    Runtime.getRuntime().addShutdownHook(new Thread(sdkMeterProvider::shutdown));

    logger.info("Initializing Metric Recorder.");
    return sdkMeterProvider
        .get(cache.getProperty("instrumentation.library.name"))
        .histogramBuilder(cache.getProperty("metric.name"))
        .setDescription(cache.getProperty("description"))
        .setUnit(cache.getProperty("metric.unit"))
        .ofLongs()
        .build();
  }
}
