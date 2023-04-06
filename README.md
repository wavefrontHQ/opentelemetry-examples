# Sending Data to Our Service

If you use OpenTelemetry, you can configure the application to send traces or metrics to VMware Aria Operations for Applications (formerly known as Tanzu Observability by Wavefront) using the Wavefront proxy or the OpenTelemetry Collector.

Send trace data or metrics data to our service using only the Wavefront proxy:

<img src="images/opentelemetry_proxy_tracing.png" alt="A data flow diagram that shows how the data flows from your application to the proxy, and then to our service" style="width:750px;"/>

Follow these steps:

1. [Install the Wavefront proxy](https://docs.wavefront.com/proxies_installing.html) version 11.3 or higher.
1. Configure the Wavefront proxy to send OpenTelemetry data to our service. See the [Wavefront proxy settings for OpenTelemetry](https://docs.wavefront.com/proxies_configuring.html#opentelemetry-proxy-properties).
    * **Trace data**:
      <br/> port 4317 (recommended) with `otlpGrpcListenerPorts` **or** port 4318 (recommended) with `otlpHttpListenerPorts`
    * **Metrics data**: 
      * Port 4317 (recommended) with `otlpGrpcListenerPorts` **or** port 4318 (recommended) with `otlpHttpListenerPorts`
      * To receive the OpenTelemetry resource attributes that your application sends for metrics data, set `otlpResourceAttrsOnMetricsIncluded` to `true`.
        <br/>**Note**: Be aware that setting this to `true` increases the chance of metrics exceeding the [annotations count limit on your cluster](https://docs.wavefront.com/wavefront_limits.html#default-customer-specific-limits), causing the metrics to be dropped by the Wavefront proxy.
      
      For example, the command to start the proxy on Docker:
      ```
      docker run -d \
      -e WAVEFRONT_URL=https://<INSTANCE>.wavefront.com/api/ \
      -e WAVEFRONT_TOKEN=<TOKEN> \
      -e JAVA_HEAP_USAGE=512M \
      -e WAVEFRONT_PROXY_ARGS="--otlpGrpcListenerPorts 4317" \
      -p 2878:2878 \
      -p 4317:4317 \
      wavefronthq/proxy:latest
      ```
      <br/>For example, on Linux, Mac, and Windows:
        * Open the [`wavefront.conf`](https://docs.wavefront.com/proxies_configuring.html#proxy-file-paths) file
        * Add `otlpGrpcListenerPorts=4317`
        * Save the file.


1. Configure your application to send trace data to the Wavefront proxy. 
    <br/>By default, OpenTelemetry SDKs send data over gRPC to `http://localhost:4317`.
1. Explore trace and metrics data:
    * **Trace data**: 
      <br/>You can use our [tracing dashboards](https://docs.wavefront.com/tracing_basics.html#visualize-distributed-tracing-data) to visualize the requests as traces, which consists of a hierarchy of spans. This visualization helps you pinpoint where the request is spending most of its time and discover problems.
    * **Metrics data**:
        <br/>Explore the metrics data you sent with charts and dashboards.
        * Try out the [Dashboards and Charts tutorial](https://docs.wavefront.com/tutorial_dashboards.html), or watch the video on that page to get started.
        * Create [dashboards](https://docs.wavefront.com/ui_dashboards.html) and [charts](https://docs.wavefront.com/ui_charts.html) using the data you sent to our service. 
          <br/>You need to have the required permissions to do these tasks.


### Metrics Conversion 

The OpenTelemetry metrics your applications send are converted to the [our data format](https://docs.wavefront.com/wavefront_data_format.html) as follows:

![There is a table that shows how the OpenTelemetry metrics are converted to the Wavefront metrics format](images/opentelemetry_metrics_data_conversion.png)

For more information on our metrics, see [Metric Types](https://docs.wavefront.com/metric_types.html).


## Tutorials

Our OpenTelemetry GitHub repository includes specific examples for using the OpenTelemetry collector in Java, Python, .NET, and more. 
 
* If you are on our Documentation, expand the tutorials section under OpenTelemetry, and try out a tutorial.
* If you are on the GitHub repository, for example, go to the `java-examples` folder and follow the steps in the README to instrument Java Apps with OpenTelemetry. 

<!-- 
## License
[Apache 2.0 License - NEEDS TO BE LINKED ONCE ADDED]()
-->

## Getting Support
* If you run into any issues with the examples, let us know by creating a GitHub issue on our [OpenTelemetry GitHub repository](https://github.com/wavefrontHQ/opentelemetry-examples).
* If you didn't find the information you are looking for in our [Documentation](https://docs.wavefront.com/), create a GitHub issue or PR in our [docs repository](https://github.com/wavefrontHQ/docs).
