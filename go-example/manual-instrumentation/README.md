# Instrumenting Golang Apps with OpenTelemetry

## Manual-Instrumentation

This section shows a working example of a Go application manually-instrumented with OpenTelemetry. See
this [working example](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/go-example/manual-instrumentation/main.go)
.

### Prerequisites

* A Tanzu Observability by Wavefront account, which gives you access to a cluster.
* Clone the [OpenTelemetry Examples](https://github.com/wavefrontHQ/opentelemetry-examples) repository.
* [Install the Wavefront proxy](http://docs.wavefront.com/proxies_installing.html#install-a-proxy).
  <br/>**Note**: When running the Wavefront proxy:
  * Make sure that the `WAVEFRONT_PROXY_ARGS` environment variable contains `--otlpGrpcListenerPorts 4317`.
  * And expose the OpenTelemetry port via `-p 4317:4317`.
    
* Set up an OpenTelemetry Collector for Tanzu Observability:
    1. Download the `otelcol-contrib` binary from the latest release of the [OpenTelemetry Collector project](https://github.com/open-telemetry/opentelemetry-collector-releases/releases).
    1. In the same directory, create a file named `otel_collector_config.yaml`.
    1. Copy the configurations in the [preconfigured YAML file](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/otel_collector_config.yaml) to the file you just created. For details on OpenTelemetry configurations, see [OpenTelemetry Collector Configuration](https://opentelemetry.io/docs/collector/configuration/).
    1. On your console, navigate to the directory you downloaded in the step above and run the following command to start OpenTelemetry Collector:
        ```
        ./otelcol-contrib --config otel_collector_config.yaml
        ```

### Step 1: Install OpenTelemetry Packages

All the required dependencies are listed
in [`go.mod`](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/go-example/manual-instrumentation/go.mod).

```
require (
	go.opentelemetry.io/otel v1.3.0
	go.opentelemetry.io/otel/exporters/otlp/otlptrace v1.3.0
	go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracegrpc v1.3.0
	go.opentelemetry.io/otel/sdk v1.3.0
	go.opentelemetry.io/otel/trace v1.3.0
	google.golang.org/grpc v1.43.0
)
```

To install OpenTelemetry packages for Golang, run this command:

```
go mod tidy
```

### Step 2: Creating an Exporter

The SDK connects telemetry from the OpenTelemetry API to exporters. Exporters are packages that allow telemetry data to
be emitted to collector for further analysis and/or enrichment.

```
func newExporter(err error, ctx context.Context, conn *grpc.ClientConn) *otlptrace.Exporter {
  traceExporter, err := otlptracegrpc.New(ctx, otlptracegrpc.WithGRPCConn(conn))
  return traceExporter
}
```

### Step 3: Creating a Resource

Telemetry data can be crucial to solving issues with a service. The catch is, we need a way to identify what service, or
even what service instance, that data is coming from.

```
func newResource(ctx context.Context) (*resource.Resource, error) {
  res, err := resource.New(ctx,
    resource.WithAttributes(
        semconv.ServiceNameKey.String("<my-service-name>"),
        attribute.String("application", "<my_application-name>"), 
	), 
  )
  return res, err
}
```

### Step 4: Installing a Tracer Provider

TracerProvider is a centralized point where instrumentation will get a Tracer from and funnels the telemetry data from
these Tracers to export pipelines.

```
func newTraceProvider(res *resource.Resource, bsp sdktrace.SpanProcessor) *sdktrace.TracerProvider {
  tracerProvider := sdktrace.NewTracerProvider(
    sdktrace.WithSampler(sdktrace.AlwaysSample()),
    sdktrace.WithResource(res),
    sdktrace.WithSpanProcessor(bsp),
  )
  return tracerProvider
}
```

### Step 5: Register the trace exporter

Register the trace exporter with a TracerProvider, using a batch span processor to aggregate spans before export.

```
func initTracer() {

  ctx := context.Background()
  
  res, err := newResource(ctx)
  
  conn, err := grpc.DialContext(ctx, "localhost:4317", grpc.WithTransportCredentials(insecure.NewCredentials()), grpc.WithBlock())
  
  traceExporter := newExporter(err, ctx, conn)
  
  batchSpanProcessor := sdktrace.NewBatchSpanProcessor(traceExporter)
  
  tracerProvider := newTraceProvider(res, batchSpanProcessor)
  
  otel.SetTracerProvider(tracerProvider)
}
```

### Step 6: Create nested spans, add attribute & event

* A **span** represents a distinct operation - not an individual function, but an entire operation, such as a database
  query. Generally, this means we shouldn't be creating spans in our application code, they should be managed as part of
  the framework or library we are using.
* **Attributes** are keys and values that are applied as metadata to your spans and are useful for aggregating,
  filtering, and grouping traces.
* An **event** is a human-readable message on a span that represents “something happening” during it’s lifetime.

```
func parentFunction(ctx context.Context, tracer trace.Tracer) {
  ctx, parentSpan := tracer.Start(
  ctx,
    "<my-parent-span-name>",
    trace.WithAttributes(attribute.String("<my-attribute-key>", "<my-attribute-value>")))
  parentSpan.AddEvent("<my-event-name>")
  defer parentSpan.End()
  childFunction(ctx, tracer)
}

func childFunction(ctx context.Context, tracer trace.Tracer) {
  ctx, childSpan := tracer.Start(
    ctx,
    "<my-child-span-name>", )
  defer childSpan.End()
}
```

### Step 6: Recording Errors

Exceptions are reported as events, but they should be properly formatted. As a convenience, OpenTelemetry provides a
RecordError method for capturing them correctly.

```
func exceptionFunction(ctx context.Context, tracer trace.Tracer) {
  ctx, exceptionSpan := tracer.Start(
    ctx,
    "exceptionSpanName",
    trace.WithAttributes(attribute.String("exceptionAttributeKey1", "exceptionAttributeValue1")))
    
  defer exceptionSpan.End()
  
  log.Printf("Call division function.")
  
  _, err := divide(10, 0)
  
  if err != nil {
    exceptionSpan.RecordError(err)
    exceptionSpan.SetStatus(codes.Error, err.Error())
  }
}

func divide(x int, y int) (int, error) {
  if y == 0 {
    return -1, errors.New("division by zero")
  }
  return x / y, nil
}
```

### Step 7: Run the application

Start the application, either from your IDE or from the terminal. The application generates and emits a trace of that
transaction. When the trace data collected from the OpenTelemetry collector are ingested, we can examine them in
the [Tanzu Observability user interface](https://docs.wavefront.com/tracing_ui_overview.html).
