package gooteltest

import (
	"errors"
	"fmt"
	"io"
	"os"
	"sync"
	"time"

	"gopkg.in/yaml.v2"
)

const (
	MetricTypeGauge               = "gauge"
	MetricTypeSum                 = "sum"
	MetricTypeHistogram           = "histogram"
	DeltaAggregationSelector      = "delta"
	CumulativeAggregationSelector = "cumulative"
)

var metricTypeNames = map[string]bool{
	MetricTypeGauge: true, MetricTypeSum: true, MetricTypeHistogram: true}

// MetricInfo gives the name and type of particular metric. type must be
// 'gauge', 'sum', or 'histogram'
type MetricInfo struct {
	Name string `yaml:"name"`
	Type string `yaml:"type"`
}

// MetricValue is a metric name metric value pair.
type MetricValue struct {
	Name  string  `yaml:"name"`
	Value float64 `yaml:"value"`
}

// MetricValueSet is a set of values to send for each metric. If the value
// for a metric is omitted, it means use the last known value of the metric
// in an earlier MetricValueSet.
type MetricValueSet struct {
	ValueSet []MetricValue `yaml:"valueSet"`
}

// Config represents the yaml configuration file which controls the metrics
// sent to the OTEL collector.
type Config struct {

	// Controls how often metric values get sent. If set to '10s', metrics
	// get sent every 10 seconds. Default is 10s.
	CollectPeriod time.Duration `yaml:"collectPeriod"`

	// The host and port of the grpc OTEL collector. Default is
	// 'localhost:4317'
	AggregationTemporalitySelector string `yaml:"aggregationTemporalitySelector"`

	// The names and types of the metrics being sent.
	Metrics []MetricInfo `yaml:"metrics"`

	// The metric values to be sent. A single ValueSet gets sent every 10s
	// or whatever CollectPeriod is set to. After the last ValueSet is sent
	// it loops back to the first.
	ValueSets []MetricValueSet `yaml:"valueSets"`
}

// ReadConfig reads the yaml config file from reader r.
func ReadConfig(r io.Reader) (*Config, error) {
	decoder := yaml.NewDecoder(r)
	decoder.SetStrict(true)
	var result Config
	if err := decoder.Decode(&result); err != nil {
		return nil, err
	}
	result.fixDefaults()
	if err := checkConfig(&result); err != nil {
		return nil, err
	}
	return &result, nil
}

// ReadConfigFromFile reads the yaml config file from disk.
func ReadConfigFromFile(fileName string) (*Config, error) {
	f, err := os.Open(fileName)
	if err != nil {
		return nil, err
	}
	defer f.Close()
	return ReadConfig(f)
}

func (c *Config) fixDefaults() {
	if c.CollectPeriod == 0 {
		c.CollectPeriod = 10 * time.Second
	}
	if c.AggregationTemporalitySelector == "" {
		c.AggregationTemporalitySelector = CumulativeAggregationSelector
	}
}

// Engine instances keeps track of the metric values. It plays back
// the metric values in the yaml file.
type Engine struct {

	// The values for the metrics are stored here. This map never changes.
	// The key is composite. The first part of the key is the metric name;
	// the second part of the key is an integer indicating the
	// MetricValueSet. 0 means the first; 1 means the second etc. The key
	// has to be composite because the value for a particular metric
	// changes between MetricValueSets.
	values map[stringInt]float64

	// This is the total number of MetricValueSets and it never changes.
	indexCount int

	// lock protects the fields below.
	lock sync.Mutex

	// For each metric name, indexes gives the 0 based MetricValueSet
	// with the next value for that metric.  This field changes with each
	// call to NextValue. The value for each metric name will be between
	// 0 and indexCount - 1 inclusive.
	indexes map[string]int
}

// NewEngine returns a new Engine from the MetricValueSets in the yaml
// file.
func NewEngine(valueSets []MetricValueSet) *Engine {
	valuesAtIndex := make(map[string]float64)
	values := make(map[stringInt]float64)
	for idx, valueSet := range valueSets {
		for _, metricValue := range valueSet.ValueSet {
			valuesAtIndex[metricValue.Name] = metricValue.Value
		}
		for k, v := range valuesAtIndex {
			values[stringInt{name: k, idx: idx}] = v
		}
	}
	return &Engine{
		values:     values,
		indexCount: len(valueSets),
		indexes:    make(map[string]int),
	}
}

// NextValue returns the next value for the given metric name. This method
// is not idempotent. Each call to it gives the next value for that metric.
func (e *Engine) NextValue(name string) float64 {
	return e.values[stringInt{name: name, idx: e.getAndIncrementIndex(name)}]
}

func (e *Engine) getAndIncrementIndex(name string) int {
	e.lock.Lock()
	defer e.lock.Unlock()
	result := e.indexes[name]
	e.indexes[name] = (result + 1) % e.indexCount
	return result
}

func checkConfig(config *Config) error {
	if config.CollectPeriod <= 0 {
		return errors.New("collectPeriod must be a positive duration")
	}

	if !(config.AggregationTemporalitySelector == DeltaAggregationSelector ||
		config.AggregationTemporalitySelector == CumulativeAggregationSelector) {
		return errors.New("aggregationTemporalitySelector can be either delta or cumulative")
	}

	namesSeen := make(map[string]struct{})
	for _, metric := range config.Metrics {
		if _, ok := namesSeen[metric.Name]; ok {
			return fmt.Errorf("Duplicate metric: %s", metric.Name)
		}
		namesSeen[metric.Name] = struct{}{}
		if !metricTypeNames[metric.Type] {
			return fmt.Errorf("Unknown metric type: %s", metric.Type)
		}
	}
	for _, valueSet := range config.ValueSets {
		for _, metricValue := range valueSet.ValueSet {
			if _, ok := namesSeen[metricValue.Name]; !ok {
				return fmt.Errorf(
					"Unknown metric name '%s' in values section",
					metricValue.Name,
				)
			}
		}
	}
	return nil
}

type stringInt struct {
	name string
	idx  int
}
