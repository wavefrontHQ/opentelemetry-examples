# Sending Metrics from Golang Apps to OpenTelemetry

This section shows a working example of a Go application that send metrics to the OpenTelemetry collector

### Prerequisite

* Install the Tanzu Observability proxy. See this [README](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/README.md#install-tanzu-observability-proxy).
* Set up an OpenTelemetry Collector for Tanzu Observability. See this [README](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/README.md#install-the-opentelemetry-collector).

### Step 1: Install OpenTelemetry Packages

All the required dependencies are listed in [`go.mod`](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/go-example/metrics/metric_example/go.mod). To install OpenTelemetry packages for Golang, run this command:

```
go mod tidy
```

### Step 2: Create a GRPC connection to the OpenTelemetry collector

The SDK must have a connection to the OpenTelemetry collector

```
conn, err := grpc.DialContext(
    ctx,
    "localhost:4317",
    grpc.WithTransportCredentials(
        insecure.NewCredentials()), grpc.WithBlock())
```

### Step 3: Create an Exporter

The SDK connects telemetry from the OpenTelemetry API to exporters. Exporters are packages that allow telemetry data to be emitted to collector for further analysis and/or enrichment.

```
metricExporter, err := otlpmetricgrpc.New(ctx, otlpmetricgrpc.WithGRPCConn(conn))
```

### Step 4: Create a Resource

Telemetry data can be crucial to solving issues with a service. The catch is, we need a way to identify what service, or even what service instance, that data is coming from.

```
res, err := resource.New(ctx,
    resource.WithAttributes(
        semconv.ServiceNameKey.String("example-service"),
        attribute.String("application", "example-app"),
    ),
)
```

### Step 5: Create and start a Controller

A controller is an event loop that periodically dispatches metrics to the OpenTelemetry collector

```
cont := controller.New(
    processor.NewFactory(
        simple.NewWithInexpensiveDistribution(),
        metricExporter,
    ),
    controller.WithExporter(metricExporter),
    controller.WithCollectPeriod(5*time.Second), // Update every 5 seconds
    controller.WithResource(res),
)

// Start the controller
if err := cont.Start(context.Background()); err != nil {
    log.Fatal("failed to start controller: ", err)
}
```

### Step 6: Register the controller as the global meter provider

Controllers provide meters that can report various metrics

```
global.SetMeterProvider(cont)
```

### Step 7: Create a meter. Register callbacks on it for each metric.

Rather than sending metric values synchronously, applications can register callback functions that provide the current value for each metric.  Whenever the controller is ready to send metrics, it invokes the callbacks to gather the metric values.

```
meter := global.Meter("example-meter")

// Register a gauge metric that reports a random number between 0 and 1.
metric.Must(meter).NewFloat64GaugeObserver(

    // Name of gauge metric
    "random-gauge-metric",

    // Callback function that gets called every 5 seconds (or whaatever the
    // collect period is set to) to update the metric.
    func(_ context.Context, result metric.Float64ObserverResult) {

        // Report the random number
        result.Observe(rand.Float64())
    },
)

// Register a sum metric that reports how many requests were made to this
// server.
metric.Must(meter).NewFloat64CounterObserver(

    // Name of counter metric
    "request-count",

    // Callback to update the metric
    func(_ context.Context, result metric.Float64ObserverResult) {
        result.Observe(float64(atomic.LoadInt64(&requestCount)))
    },
)
```

### Step 8: Run the application

Start the application, either from your IDE or from the terminal. The application generates and emits metrics to the OpenTelemetry collector.
