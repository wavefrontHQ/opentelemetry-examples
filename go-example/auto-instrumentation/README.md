# Instrumenting Golang Apps with OpenTelemetry

## Auto Instrumentation

This section shows a working example of a Go application auto-instrumented with OpenTelemetry. See
this [working example](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/go-example/auto-instrumentation.go)
.

### Prerequisite

* Install the Tanzu Observability proxy. See
  this [README](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/README.md#install-wavefront-proxy).
* Set up an OpenTelemetry Collector for Tanzu Observability. See
  this [README](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/README.md#install-the-opentelemetry-collector)
  .

### Step 1: Install OpenTelemetry Packages

All the required dependencies are listed
in [`go.mod`](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/go-example/go.mod). To install
OpenTelemetry packages for Golang, run this command:

```
go mod tidy
```

### Step 2: Creating an Exporter

The SDK connects telemetry from the OpenTelemetry API to exporters. Exporters are packages that allow telemetry data to
be emitted to collector for further analysis and/or enrichment.

```
exporter, err := otlptrace.New(
  context.Background(),
  otlptracegrpc.NewClient(
  otlptracegrpc.WithInsecure(),
  otlptracegrpc.WithEndpoint("localhost:4317"),
  ),
)
```

### Step 3: Creating a Resource

Telemetry data can be crucial to solving issues with a service. The catch is, we need a way to identify what service, or
even what service instance, that data is coming from.

```
resources, err := resource.New(
  context.Background(),
  resource.WithAttributes(
  attribute.String("service.name", "<my-service-name>"),
  attribute.String("application", "<my-application-name>"),
  ),
)
```

### Step 4: Installing a Tracer Provider

TracerProvider is a centralized point where instrumentation will get a Tracer from and funnels the telemetry data from
these Tracers to export pipelines.

```
otel.SetTracerProvider(
  sdktrace.NewTracerProvider(
  sdktrace.WithSampler(sdktrace.AlwaysSample()),
  sdktrace.WithSpanProcessor(sdktrace.NewBatchSpanProcessor(exporter)),
  sdktrace.WithSyncer(exporter),
  sdktrace.WithResource(resources),
  ),
)
```

### Step 5: Run the application

* Start the application, either from your IDE or from the terminal.

* Visit ```http://localhost:8080``` and refresh the page. The application generates and emits a trace of that
  transaction. When the trace data collected from the OpenTelemetry collector are ingested, we can examine them in
  the [Tanzu Observability user interface](https://docs.wavefront.com/tracing_ui_overview.html).

## Manual-Instrumentation

This section shows a working example of a Go application manually-instrumented with OpenTelemetry. See
this [working example](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/go-example/manual-instrumentation.go)
.

### Prerequisite

* Install the Tanzu Observability proxy. See
  this [README](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/README.md#install-wavefront-proxy).
* Set up an OpenTelemetry Collector for Tanzu Observability. See
  this [README](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/README.md#install-the-opentelemetry-collector)
  .

### Step 1: Install OpenTelemetry Packages

All the required dependencies are listed
in [`go.mod`](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/go-example/go.mod). To install
OpenTelemetry packages for Golang, run this command:

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