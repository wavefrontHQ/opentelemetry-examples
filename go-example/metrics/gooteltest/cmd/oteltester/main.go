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
	"go.opentelemetry.io/otel/sdk/metric/export/aggregation"
	processor "go.opentelemetry.io/otel/sdk/metric/processor/basic"
	"go.opentelemetry.io/otel/sdk/metric/selector/simple"
	"go.opentelemetry.io/otel/sdk/resource"
	semconv "go.opentelemetry.io/otel/semconv/v1.7.0"
)

const (
	MetricTypeGauge          = "gauge"
	MetricTypeSum            = "sum"
	MetricTypeHistogram      = "histogram"
	DeltaAggregationSelector = "delta"
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

	temporalitySelector := aggregation.CumulativeTemporalitySelector()
	if config.AggregationTemporalitySelector == DeltaAggregationSelector {
		temporalitySelector = aggregation.DeltaTemporalitySelector()
	}
	// Wrap the raw grpc connection to OTEL collector with an exporter.
	metricExporter, err := newExporter(ctx, temporalitySelector)
	reportErr(err, "failed to create metric exporter")

	res, err := newResource(ctx)
	reportErr(err, "failed to create res")

	// Create a contoller with the exporter which will be the global meter
	// provider. Note that the explicit boundaries of all histograms must be
	// set globally here.
	cont := controller.New(
		processor.NewFactory(
			simple.NewWithHistogramDistribution(
				histogram.WithExplicitBoundaries([]float64{1.0, 2.0, 5.0, 10.0}),
			),
			temporalitySelector,
		),
		controller.WithResource(res),
		controller.WithExporter(metricExporter),
		controller.WithCollectPeriod(config.CollectPeriod),
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

func newExporter(ctx context.Context, temporalitySelector aggregation.TemporalitySelector) (*otlpmetric.Exporter, error) {
	return otlpmetric.New(
		ctx,
		otlpmetricgrpc.NewClient(),
		otlpmetric.WithMetricAggregationTemporalitySelector(temporalitySelector))
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

func registerGaugeMetric(
	meter metric.Meter,
	name string,
	engine *gooteltest.Engine,
) {

	gaugeObserver, err := meter.AsyncFloat64().Gauge(name)
	if err != nil {
		log.Fatalf("failed to initialize instrument: %v", err)
	}
	gaugeObserver.Observe(context.Background(), engine.NextValue(name))
}

func registerSumMetric(
	meter metric.Meter,
	name string,
	prefix string,
	engine *gooteltest.Engine,
) {
	counter, err := meter.SyncFloat64().Counter(prefix + name)
	if err != nil {
		log.Fatalf("failed to initialize instrument: %v", err)
	}

	counter.Add(context.Background(), engine.NextValue(name))
}

func registerHistograms(
	meter metric.Meter,
	name string,
	prefix string,
	engine *gooteltest.Engine,
) {
	histogram, err := meter.SyncFloat64().Histogram(prefix + name)
	if err != nil {
		log.Fatalf("failed to initialize instrument: %v", err)
	}

	histogram.Record(context.Background(), engine.NextValue(name))
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

	prefix := "cum_"
	if config.AggregationTemporalitySelector == DeltaAggregationSelector {
		prefix = "delta_"
	}

	shutdown := initMetric(config)
	defer shutdown()
	engine := gooteltest.NewEngine(config.ValueSets)
	meter := global.Meter("opamp")

	go forever(meter, config, engine, prefix)
	select {} // block forever
}

func forever(meter metric.Meter,
	config *gooteltest.Config,
	engine *gooteltest.Engine,
	prefix string) {
	for {
		for _, m := range config.Metrics {
			switch m.Type {
			case MetricTypeGauge:
				registerGaugeMetric(meter, m.Name, engine)
			case MetricTypeSum:
				registerSumMetric(meter, m.Name, prefix, engine)
			case MetricTypeHistogram:
				registerHistograms(meter, m.Name, prefix, engine)
			}
		}
		time.Sleep(config.CollectPeriod)
	}
}

func init() {
	flag.StringVar(&fConfig, "config", "", "Config file path")
}
