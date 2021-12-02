# Steps to manually-instrument Java app

### Step 1: Install Wavefront proxy
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

### Step 2: Install the OpenTelemetry Collector
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

### Step 3: Run your application
The collector is now running and listening to incoming traces on port 4317. Just start your application either from the CMD line or from an IDE.

### Step 3: Try it out
The Java application will generate traces of a transaction and will send them to the Otel-Collector. The Otel-Collector will then pick up these traces and send them to the distributed tracing backend(wavefront) defined by the exporter in the collector config file.