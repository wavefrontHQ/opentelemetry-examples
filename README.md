# Sending Data to Tanzu Observability by Wavefront

If your application uses an OpenTelemetry SDK, you can configure the application to send traces or metrics to Tanzu Observability using the Wavefront Proxy or the OpenTelemetry Collector.

## Send Trace Data
If your application uses an OpenTelemetry SDK, you can configure the application to send trace data to Tanzu Observability using any of the following options:

### Directly Send Data Using the Wavefront Proxy - (Recommended)
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

### Send Data Using the OpenTelemetry Collector and the Wavefront Proxy
If you have already configured your application to send data to the OpenTelemetry Collector, the data flows from your application to Tanzu Observability as shown in the diagram:
 
![Shows how the data flows from your application to the OpenTelemetry Collector to Tanzu Observability](images/opentelemetry_collector_tracing.png)

Follow these steps:

1. [Install the Wavefront Proxy](https://docs.wavefront.com/proxies_installing.html).
      <ul>
      <li>
        Open port 30001, with <code>customTracingListenerPorts=30001</code>, for the proxy to generate span-level RED metrics.
        </li>
        <li>
          Ensure that port 2878 is open to send spans and metrics to the Wavefront service. For example, on Linux, Mac, and Windows, open the <a href="proxies_configuring.html#proxy-file-paths"><code>wavefront.conf</code></a> file and confirm that <code>pushListenerPorts</code> is set to 2878, and that this configuration is uncommented. 
        </li>
      </ul>
         
1. Configure your application to send trace data to the OpenTelemetry Collector. See the [OpenTelemetry documentation](https://opentelemetry.io/docs/collector/) for details.
1. Export the data from the OpenTelemetry Collector to the Tanzu Observability (Wavefront) trace exporter:
    1. Download the `otelcol-contrib` binary from the latest release of the [OpenTelemetry Collector project](https://github.com/open-telemetry/opentelemetry-collector-releases/releases).
    **Note**: You need to use OpenTelemetry Collector Contrib version v0.28.0 or later to export traces to Tanzu Observability.
    1. In the same directory, create a file named `otel_collector_config.yaml`.
    1. Copy the configurations in the [preconfigured YAML file](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/otel_collector_config.yaml) to the file you just created. For details on OpenTelemetry configurations, see [OpenTelemetry Collector Configuration](https://opentelemetry.io/docs/collector/configuration/).
    1. On your console, navigate to the directory you downloaded in the step above and run the following command to start OpenTelemetry Collector:
        ```
        ./otelcol-contrib --config otel_collector_config.yaml
        ``` 
1. Explore the trace data sent using our [tracing dashboards](https://docs.wavefront.com/tracing_basics.html#visualize-distributed-tracing-data).


You can then use our tracing dashboards to visualize the requests as traces, which consists of a hierarchy of spans. This visualization helps you pinpoint where the request is spending most of its time and discover problems.

## Send Metrics Data

If your application uses an OpenTelemetry SDK, you can configure the application to send metrics data to Tanzu Observability using the OpenTelemetry Collector and the Wavefront proxy. Metrics data includes time series, counters, and histograms. When the data is in Tanzu Observability, you can use charts and dashboards to visualize the data and create alerts.

Here's how it works:
![The diagram shows how the data flows from an application to OpenTelemetry collector, which has the OpenTelemetry exporter, to the wavefront proxy, which has the OpenTelemetry receiver, and finally to Tanzu Observability.](images/opentelemetry_collector_metrics.png)

Follow these steps:

1. [Install the Wavefront Proxy](https://docs.wavefront.com/proxies_installing.html).
      <ul>
        <li>
          If you have already installed the Wavefront proxy, make sure it is version 10.14 or later. 
        </li>
        <li>
          Ensure that port 2878 is open to send metrics to Tanzu Observability. For example, on Linux, Mac, and Windows, open the <a href="proxies_configuring.html#proxy-file-paths"><code>wavefront.conf</code></a> file and confirm that <code>pushListenerPorts</code> is set to 2878, and that this configuration is uncommented.
        </li>
      </ul>

1. Configure your application to send the metrics data to the OpenTelemetry Collector. 
1. Export the data from the OpenTelemetry Collector to the Tanzu Observability (Wavefront) metrics exporter:
    1. Download the `otelcol-contrib` binary from the latest release of the [OpenTelemetry Collector project](https://github.com/open-telemetry/opentelemetry-collector-releases/releases).
    **Note**: You need to use OpenTelemetry Collector Contrib version v0.51.0 or later to export metrics to Tanzu Observability.
    1. In the same directory, create a file named `otel_collector_config.yaml`.
    1. Copy the configurations in the [preconfigured YAML file](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/otel_collector_config.yaml) to the file you just created. For details on OpenTelemetry configurations, see [OpenTelemetry Collector Configuration](https://opentelemetry.io/docs/collector/configuration/).
    1. On your console, navigate to the directory you downloaded in the step above and run the following command to start OpenTelemetry Collector:
        ```
        ./otelcol-contrib --config otel_collector_config.yaml
        ``` 
1. Explore the metrics data you sent with charts and dashboards.
    * Try out the [Dashboards and Charts tutorial](https://docs.wavefront.com/tutorial_dashboards.html) or watch the video on that page to get started.
    * Create [dashboards](https://docs.wavefront.com/ui_dashboards.html) and [charts](https://docs.wavefront.com/ui_charts.html) for the data you sent to Tanzu Observability. 
      <br/>You need to have the required permissions to do these tasks.
    Example:
    ![shows the OpenTelemetry collector data in a chart](images/tracing_opentelemetry_collector_chart.png)

## Metrics Conversion 

The OpenTelemetry metrics your applications send are converted to the [Wavefront data format](https://docs.wavefront.com/wavefront_data_format.html) as follows:

![There is a table that shows how the OpenTelemetry metrics are converted to the Wavefront metrics format](images/opentelemetry_metrics_data_conversion.png)

For more information on the Tanzu Observability metrics, see [Metric Types](https://docs.wavefront.com/metric_types.html).


## Tutorials

The Wavefront OpenTelemetry GitHub repository includes specific examples for using the OpenTelemetry collector in Java, Python, .NET, and more. 
 
* If you are on Wavefront Documentation, expand the tutorials section under OpenTelemetry, and try out a tutorial.
* If you are on the GitHub repository, for example, go to the `java-examples` folder and follow the steps in the README to instrument Java Apps with OpenTelemetry. 

<!-- 
## License
[Apache 2.0 License - NEEDS TO BE LINKED ONCE ADDED]()
-->

## Getting Support
* If you run into any issues with the examples, let us know by creating a GitHub issue on the [Wavefront OpenTelemetry GitHub repository](https://github.com/wavefrontHQ/opentelemetry-examples).
* If you didn't find the information you are looking for in our [Wavefront Documentation](https://docs.wavefront.com/), create a GitHub issue or PR in our [docs repository](https://github.com/wavefrontHQ/docs).
