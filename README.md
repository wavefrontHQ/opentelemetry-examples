# Sending Data to Tanzu Observability by Wavefront

If your application uses an OpenTelemetry SDK, you can configure the application to send traces or metrics to Tanzu Observability using the Wavefront Proxy or the OpenTelemetry Collector.

## Table of Content

* [Send Trace Data](#send-trace-data)
* [Send Metrics Data](#send-metrics-data)
* [Tutorials](#tutorials)
* [Getting Support](#getting-support)
<!-- * [License](#license) Add this before getting started-->

## Send Trace Data
If your application uses an OpenTelemetry SDK, you can configure the application to send trace data to Tanzu Observability using any of the following options:

* **Directly send OpenTelemetry data to the Wavefront proxy - [Recommended]**
  <img src="images/opentelemetry_proxy_tracing.png" alt="A data flow diagram that shows how the data flows from your application to the proxy, and then to Tanzu Observability" style="width:750px;"/>
  Follow these steps:

    1. [Install the Wavefront Proxy](https://docs.wavefront.com/proxies_installing.html) version 11 or higher.
    1. Open the port on the Wavefront Proxy to send OpenTelemetry spans to Tanzu Observability. 
        * port 4317 (recommended) with `otlpGrpcListenerPorts` 
        * or port 4318 (recommended) with `otlpHttpListenerPorts`  
      
        See the [Wavefront proxy settings for OpenTelemetry](https://docs.wavefront.com/proxies_configuring.html#opentelemetry-proxy-properties).
        <br/>For example, on Linux, Mac, and Windows, open the [`wavefront.conf`](https://docs.wavefront.com/proxies_configuring.html#proxy-file-paths) file, add the line `otlpGrpcListenerPorts=4317`, and save the file.
    1. Configure your application to send trace data to the Wavefront Proxy. 
        <br/>By default, OpenTelemetry SDKs send data over gRPC to `http://localhost:4317`.
    1. Explore the trace data using our [tracing dashboards](https://docs.wavefront.com/tracing_basics.html#visualize-distributed-tracing-data).

* Or [**use the OpenTelemetry Collector and the Wavefront proxy**](docs-resources/tracing/README.md#send-data-using-the-opentelemetry-collector)
  ![A data flow diagram that shows how the data flows from your application to the collector, to the proxy, and then to Tanzu Observability](images/opentelemetry_collector_tracing.png)

You can then use our tracing dashboards to visualize the requests as traces, which consists of a hierarchy of spans. This visualization helps you pinpoint where the request is spending most of its time and discover problems.

## Send Metrics Data

<!-- To be added later
If your application uses an OpenTelemetry SDK, you can configure the application to send metrics data to Tanzu Observability using the Tanzu Observability OpenTelemetry Collector. See [OpenTelemetry Metrics Data](docs-resources/metrics/README.md) for details.

![A data flow diagram that shows how the data flows from your application to the collector, to the proxy, and then to Tanzu Observability.](images/opentelemetry_collector_metrics.png)
-->

Metrics support for OpenTelemetry on Tanzu Observability by Wavefront will be made available soon!

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
