# The gooteltest oteltester

## Compiling
```sh
go install github.com/wavefronthq/opentelemetry-examples/go-example/metrics/gooteltest/cmd/oteltester@latest
```

## running
```sh
~/go/bin/gooteltest --config example.yaml
```
When you run the tester it runs forever sending the metrics in the yaml file to the OTEL collector in a loop.

## The config file
See the sample config file in example.yaml.  
### Config file fields

| FieldName | Description |
| --------- | ----------- |
| collectPeriod | This is the amount of time to wait before sending the next group of metrics. Default is 10s |
| otelCollector | This is the host and port that the OTEL collector is listening on for metrics. Default is localhost:4317 |
| metrics | The metrics in this file. Each metric has a name and type. types can be _gauge_, _sum_, or _histogram_ |
| valueSets | Contains all metric values to send |
| valueSet | valueSets consists of one or more ValueSet. Each valueSet lists the name and value of each metric |

## A note on histograms
Currently the explicit boundaries must be set for all histograms in the application. This is a limitation of the current Go OTEL API. For now, the buckets for histograms are hardcoded as _less than 1_, _between 1 and 2_, _between 2 and 5_, _between 5 and 10_, and _greater than 10_

Histogram metrics in the config file have scalar values. Each time this application sends a histogram metric value, it increments the corresponding bucket in the histogram metric.
