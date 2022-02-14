package main

import (
	"context"
	"flag"
	"fmt"
	"log"
	"os"
	"time"

	"github.com/wavefronthq/opentelemetry-examples/go-example/metrics/gooteltest"
	"go.opentelemetry.io/otel/attribute"
	"go.opentelemetry.io/otel/exporters/otlp/otlpmetric"
	"go.opentelemetry.io/otel/exporters/otlp/otlpmetric/otlpmetricgrpc"
	"go.opentelemetry.io/otel/metric"
	"go.opentelemetry.io/otel/metric/global"
	"go.opentelemetry.io/otel/sdk/metric/aggregator/histogram"
	controller "go.opentelemetry.io/otel/sdk/metric/controller/basic"
	processor "go.opentelemetry.io/otel/sdk/metric/processor/basic"
	"go.opentelemetry.io/otel/sdk/metric/selector/simple"
	"go.opentelemetry.io/otel/sdk/resource"
	semconv "go.opentelemetry.io/otel/semconv/v1.7.0"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
)

var (
	fConfig string
)

// initMetric starts the connection with the OTEL collector and returns a
// no-arg function that can be called to shut down the connection. It also
// registers a global meter provider which is used to register metrics to
// be sent to the OTEL collector.
func initMetric(config *gooteltest.Config) func() {
	ctx, cancel := context.WithCancel(context.Background())

	conn, err := grpc.DialContext(
		ctx,
		config.OtelCollector,
		grpc.WithTransportCredentials(
			insecure.NewCredentials()), grpc.WithBlock())
	reportErr(err, "failed to create gRPC connection to collector")

	// Wrap the raw grpc connection to OTEL collector with an exporter.
	metricExporter, err := newExporter(ctx, conn)
	reportErr(err, "failed to create metric exporter")

	res, err := newResource(ctx)
	reportErr(err, "failed to create res")

	// Create a contoller with the exporter which will be the global meter
	// provider. Note that the explicit boundaries of all histograms must be
	// set globally here.
	cont := controller.New(
		processor.NewFactory(
			simple.NewWithHistogramDistribution(
				histogram.WithExplicitBoundaries(
					[]float64{1.0, 2.0, 5.0, 10.0},
				),
			),
			metricExporter,
		),
		controller.WithExporter(metricExporter),
		controller.WithCollectPeriod(config.CollectPeriod),
		controller.WithResource(res),
	)

	// Start the controller
	reportErr(cont.Start(context.Background()), "failed to start controller")

	// Register controller as global meter provider.
	global.SetMeterProvider(cont)

	// Our quit function that we return will stop the controller and cancel
	// the context on the exporter.
	return func() {
		_ = cont.Stop(context.Background())
		cancel()
	}
}

func newExporter(
	ctx context.Context,
	conn *grpc.ClientConn,
) (*otlpmetric.Exporter, error) {
	return otlpmetricgrpc.New(ctx, otlpmetricgrpc.WithGRPCConn(conn))
}

func reportErr(err error, message string) {
	if err != nil {
		log.Printf("%s: %v", message, err)
	}
}

func newResource(ctx context.Context) (*resource.Resource, error) {
	return resource.New(ctx,
		resource.WithAttributes(
			// the service name used to display traces in backends
			semconv.ServiceNameKey.String("otel-otlp-go-service"),
			attribute.String("application", "otel-otlp-go-app"),
		),
	)
}

// registerMetricObservers registers asynchronous metrics with given meter.
// Only "gauge" and "sum" metrics are asynchronous. histogram metrics get
// registered in another function.
//
// meter is what we are registring with. metrics is the list of metric names
// and types from the yaml file. engine supplies the values for the metrics.
func registerMetricObservers(
	meter metric.Meter,
	metrics []gooteltest.MetricInfo,
	engine *gooteltest.Engine,
) {
	for _, m := range metrics {
		switch m.Type {
		case "gauge":
			registerGaugeMetric(meter, m.Name, engine)
		case "sum":
			registerSumMetric(meter, m.Name, engine)
		}
	}
}

func registerGaugeMetric(
	meter metric.Meter,
	name string,
	engine *gooteltest.Engine,
) {
	metric.Must(meter).NewFloat64GaugeObserver(
		name,
		func(_ context.Context, result metric.Float64ObserverResult) {
			result.Observe(engine.NextValue(name))
		},
	)
}

func registerSumMetric(
	meter metric.Meter,
	name string,
	engine *gooteltest.Engine,
) {
	metric.Must(meter).NewFloat64CounterObserver(
		name,
		func(_ context.Context, result metric.Float64ObserverResult) {
			result.Observe(engine.NextValue(name))
		},
	)
}

// registerHistograms registers the histogram metrics with given meter.
// collectPeriod is how often we send histogram data. meter is what we are
// registering with. metrics is the list of metric names and types from the
// yaml file. engine supplies the values for the histograms.
func registerHistograms(
	collectPeriod time.Duration,
	meter metric.Meter,
	metrics []gooteltest.MetricInfo,
	engine *gooteltest.Engine,
) {
	// A map of histogram name to the histogram.
	histograms := make(map[string]metric.Float64Histogram)

	for _, m := range metrics {
		if m.Type != "histogram" {
			continue
		}
		histograms[m.Name] = metric.Must(meter).NewFloat64Histogram(m.Name)
	}
	ticker := time.NewTicker(collectPeriod)
	measurements := make([]metric.Measurement, len(histograms))

	// This go function is what sends the histogram values to the collector
	// in a loop.
	go func() {
		for {
			// wait for collectPeriod seconds to elapse
			<-ticker.C

			// Build measurements slice of next values.
			idx := 0
			for name, histogram := range histograms {
				measurements[idx] = histogram.Measurement(
					engine.NextValue(name))
				idx++
			}

			// Send the values to the collector.
			ctx := context.Background()
			meter.RecordBatch(
				ctx,
				[]attribute.KeyValue{},
				measurements...)
		}
	}()
}

func main() {
	flag.Parse()
	if fConfig == "" {
		fmt.Println("Need to specify -config flag.")
		flag.Usage()
		os.Exit(1)
	}
	config, err := gooteltest.ReadConfigFromFile(fConfig)
	if err != nil {
		log.Fatalf("Error opening config file: %v", err)
	}
	shutdown := initMetric(config)
	defer shutdown()
	engine := gooteltest.NewEngine(config.ValueSets)
	meter := global.Meter("opamp")
	registerMetricObservers(meter, config.Metrics, engine)
	registerHistograms(config.CollectPeriod, meter, config.Metrics, engine)

	var waitForever chan struct{}
	<-waitForever
}

func init() {
	flag.StringVar(&fConfig, "config", "", "Config file path")
}
