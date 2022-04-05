# Sending Trace Data to Tanzu Observability by Wavefront

This README is for all users who want to send OpenTelemetry trace data or metrics data to Tanzu Observability. This README explains how to install Tanzu Observability proxy and install the OpenTelemetry collector. This repository includes specific examples for using the OpenTelemetry collector in java, python, and .NET, etc.

## Overview

If your application uses OpenTelemetry, you can use the Tanzu Observability exporter to send trace data or metrics data to Tanzu Observability UI. When trace data is in Tanzu Observability UI, you can use tracing dashboards to visualize any request as a trace that consists of a hierarchy of spans. This visualization helps you pinpoint where the request is spending most of its time and discover problems.

![Here is how it works:](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/resources/TraceFlow.png?raw=true)

## Prerequisites

* A Tanzu Observability account (If you don't have one already, you
  can [sign up for one](https://tanzu.vmware.com/observability))

### Install Tanzu Observability Proxy

Use docker run to install the Tanzu Observability proxy. You have to specify.

* The Tanzu Observability instance (for example, https://longboard.wavefront.com).
* A Tanzu Observability API token that is linked to an account with Proxy permission.
  See [Generating and an API Token](https://docs.wavefront.com/wavefront_api.html#generating-an-api-token).

```
docker run -d \
      -e WAVEFRONT_URL=https://{INSTANCE_NAME}.wavefront.com/api/ \
      -e WAVEFRONT_TOKEN={TOKEN} \
      -e JAVA_HEAP_USAGE=512m \
      -e WAVEFRONT_PROXY_ARGS="--customTracingListenerPorts 30001" \
      -p 2878:2878 \
      -p 30001:30001 \
      wavefronthq/proxy:latest
```

### Install the OpenTelemetry Collector

Download the binary from the latest release of
the [OpenTelemetry Collector project](https://github.com/open-telemetry/opentelemetry-collector-contrib/releases) and
add it to a preferred directory.

In the same directory, create the `otel_collector_config.yaml` file and copy the below configuration into the yaml file. (Learn more about [OpenTelemetry collector configuration](https://opentelemetry.io/docs/collector/configuration/)).

```
receivers:
   otlp:
      protocols:
          grpc:
              endpoint: "localhost:4317"
exporters:
    tanzuobservability:
      traces:
        endpoint: "http://localhost:30001"
      metrics:
        endpoint: "http://localhost:2878"
  # Proxy hostname and customTracing ListenerPort
processors:
    batch:
      timeout: 10s
    memory_limiter:
      check_interval: 1s
      limit_percentage: 50
      spike_limit_percentage: 30


service:
    pipelines:
      metrics:
        receivers: [otlp]
        exporters: [tanzuobservability]
        processors: [memory_limiter, batch]
      traces:
        receivers: [otlp]
        exporters: [tanzuobservability]
        processors: [memory_limiter, batch]
```

Navigate to the directory from your console and run the following command to start OTel collector:

```
./otelcontribcol_darwin_amd64 --config otel_collector_config.yaml
```
