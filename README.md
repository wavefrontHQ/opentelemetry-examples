# Sending Data to Tanzu Observability by Wavefront

If your application uses an OpenTelemetry SDK, you can configure the application to send traces or metrics to Tanzu Observability using the Wavefront Proxy or the OpenTelemetry Collector.

## Table of Content

* [Send Trace Data](#send-trace-data)
* [Send Metrics Data](#send-metrics-data)
* [Tutorials](#tutorials)
<!-- * [License](#license) -->
* [Getting Support](#getting-support)


## Send Trace Data
If your application uses an OpenTelemetry SDK, you can configure the application to send trace data to Tanzu Observability using any of the following options:

* [**Directly send OpenTelemetry data to the Wavefront proxy**](resources/tracing/README.md#send-data-using-the-wavefront-proxy---recommended) - [Recommended]
  <img src="images/opentelemetry_proxy_tracing.png" alt="A data flow diagram that shows how the data flows from your application to the proxy, and then to Tanzu Observability" style="width:750px;"/>
* Or [**use the OpenTelemetry Collector and the Wavefront proxy**](resources/tracing/README.md#send-data-using-the-opentelemetry-collector)
  ![A data flow diagram that shows how the data flows from your application to the collector, to the proxy, and then to Tanzu Observability](images/opentelemetry_collector_tracing.png)

You can then use our tracing dashboards to visualize any request as a trace, which consists of a hierarchy of spans. This visualization helps you pinpoint where the request is spending most of its time and discover problems.

## Send Metrics Data

If your application uses an OpenTelemetry SDK, you can configure the application to send metrics data to Tanzu Observability using the Tanzu Observability OpenTelemetry Collector. See [OpenTelemetry Metrics Data](resources/metrics/README.md) for details.

![A data flow diagram that shows how the data flows from your application to the collector, to the proxy, and then to Tanzu Observability.](images/opentelemetry_collector_metrics.png)

## OpenTelemetry Collector

Follow these steps to configure the OpenTelemetry Collector:

1. Download the `otelcol-contrib` binary from the latest release of
the [OpenTelemetry Collector project](https://github.com/open-telemetry/opentelemetry-collector-contrib/releases) and add it to a preferred directory.

1. Create a file named `otel_collector_config.yaml` in the same directory.
1. Copy the configurations in the  [`otel_collector_config.yaml`](resources/otel_collector_config.yml) file to the new file you created.  

See [OpenTelemetry collector configurations](https://opentelemetry.io/docs/collector/configuration/) to learn more.

## Tutorials

This repository includes specific examples for using the OpenTelemetry collector in Java, Python, .NET, and more. 

For example, navigate to the `java-examples` folder and follow the steps in the README to instrument Java Apps with OpenTelemetry. 

<!-- 
## License
[Apache 2.0 License - NEEDS TO BE LINKED ONCE ADDED]()
-->

## Getting Support
* Reach out to us on our public [Slack channel](https://www.wavefront.com/join-public-slack).
* If you run into any issues with the examples, let us know by creating a GitHub issue.
* If you didn't find the information you are looking for in our [Wavefront Documentation](https://docs.wavefront.com/), create a GitHub issue or PR in our [docs repository](https://github.com/wavefrontHQ/docs).
