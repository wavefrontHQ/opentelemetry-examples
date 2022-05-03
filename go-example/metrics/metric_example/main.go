package main

import (
	"context"
	"log"
	"math/rand"
	"sync/atomic"
	"time"

	"github.com/gin-gonic/gin"
	"go.opentelemetry.io/otel/attribute"
	"go.opentelemetry.io/otel/exporters/otlp/otlpmetric/otlpmetricgrpc"
	"go.opentelemetry.io/otel/metric"
	"go.opentelemetry.io/otel/metric/global"
	controller "go.opentelemetry.io/otel/sdk/metric/controller/basic"
	processor "go.opentelemetry.io/otel/sdk/metric/processor/basic"
	"go.opentelemetry.io/otel/sdk/metric/selector/simple"
	"go.opentelemetry.io/otel/sdk/resource"
	semconv "go.opentelemetry.io/otel/semconv/v1.7.0"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
)

// initMetric starts the connection with the OTEL collector and returns a
// no-arg function that can be called to shut down the connection. It also
// registers a global meter provider which is used to register metrics to
// be sent to the OTEL collector.
func initMetric() func() {
	ctx, cancel := context.WithCancel(context.Background())

	conn, err := grpc.DialContext(
		ctx,
		"localhost:4317",
		grpc.WithTransportCredentials(
			insecure.NewCredentials()), grpc.WithBlock())
	if err != nil {
		log.Fatal("failed to create gRPC connection to collector: ", err)
	}

	// Wrap the raw grpc connection to OTEL collector with an exporter.
	metricExporter, err := otlpmetricgrpc.New(
		ctx, otlpmetricgrpc.WithGRPCConn(conn))
	if err != nil {
		log.Fatal("failed to create metric exporter: ", err)
	}

	// Create new resource
	res, err := resource.New(ctx,
		resource.WithAttributes(
			semconv.ServiceNameKey.String("example-service"),
			attribute.String("application", "example-app"),
		),
	)
	if err != nil {
		log.Fatal("failed to create resource: ", err)
	}

	// Create a contoller with the exporter which will be the global meter
	// provider.
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

	// Register controller as global meter provider.
	global.SetMeterProvider(cont)

	// Our quit function that we return will stop the controller and cancel
	// the context on the exporter.
	return func() {
		_ = cont.Stop(context.Background())
		cancel()
	}
}

func main() {

	// count of requests to server
	var requestCount int64

	// initialize sending metrics
	cleanup := initMetric()
	defer cleanup()

	r := gin.Default()

	r.GET("/", func(c *gin.Context) {
		atomic.AddInt64(&requestCount, 1)
		c.String(200, "Hello, World!")
	})

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

	// Run the server
	r.Run(":8090")
}
