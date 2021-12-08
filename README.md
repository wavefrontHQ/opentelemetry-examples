# Sending Trace Data to Wavefront

If your application uses OpenTelemetry, you can configure the application to send trace data to Wavefront using the
Tanzu Observability (Wavefront) trace exporter. When the data is in Wavefront, you can use our tracing dashboards to
visualize any request as a trace that consists of a hierarchy of spans. This visualization helps you pinpoint where the
request is spending most of its time and discover problems.

![Here is how it works:](https://raw.githubusercontent.com/wavefrontHQ/opentelemetry-examples/main/TraceFlow.png?raw=true)

### Install Wavefront proxy
Configure your Tanzu Observability (Wavefront) URL and the token. (If you’ve signed up for the free trial, [here’s how you can get your token](https://docs.wavefront.com/users_account_managing.html#generate-an-api-token)).
```
docker run -d \
      -e WAVEFRONT_URL=https://{CLUSTER}.wavefront.com/api/ \
      -e WAVEFRONT_TOKEN={TOKEN} \
      -e JAVA_HEAP_USAGE=512m \
      -e WAVEFRONT_PROXY_ARGS="--customTracingListenerPorts 30001" \
      -p 2878:2878 \
      -p 30001:30001 \
      wavefronthq/proxy:latest
```

### Install the OpenTelemetry Collector
Download the binary from the latest release of the [OpenTelemetry Collector project](https://github.com/open-telemetry/opentelemetry-collector-contrib/releases) and add it to a preferred directory.

In the same directory, create the otel_collector_config.yaml file and copy the below configuration to the yaml file. (Learn more about [OpenTelemetry collector configuration](https://opentelemetry.io/docs/collector/configuration/)).

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
  # Proxy hostname and customTracing ListenerPort
processors:
    batch:
      timeout: 10s
      
service:
    pipelines:
      traces:
        receivers: [otlp]
        exporters: [tanzuobservability]
        processors: [batch]
```

Navigate to the directory from your console and run the collector host with the config file using --config parameter and the command.
```
./otelcontribcol_darwin_amd64 --config otel_collector_config.yaml
```
