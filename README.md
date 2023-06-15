# Sending Data to Our Service

If you use OpenTelemetry, you can configure the application to send traces or metrics to VMware Aria Operations for Applications (formerly known as Tanzu Observability by Wavefront) using the Wavefront proxy.

<img src="images/opentelemetry_proxy_tracing.png" alt="A data flow diagram that shows how the data flows from your application to the proxy, and then to our service" style="width:750px;"/>

## Configure the Wavefront Proxy

**Note**: Starting June 26, 2023, VMware Aria Operations for Applications is a service on the VMware Cloud services platform (CSP). With this update, the Wavefront proxy supports authentication to Operations for Applications with a VMware Cloud services API token or OAuth app. For more information, see [Proxy Authentication Types](proxies_installing.html#proxy-authentication-types).

1. [Install the Wavefront proxy](https://docs.wavefront.com/proxies_installing.html).
1. Configure the Wavefront proxy to send OpenTelemetry data to our service. See the [Wavefront proxy settings for OpenTelemetry](https://docs.wavefront.com/proxies_configuring.html#opentelemetry-proxy-properties).
  * **Trace data**: Port 4317 (recommended) with `otlpGrpcListenerPorts` **or** port 4318 (recommended) with `otlpHttpListenerPorts`.
  * **Metrics data**: 
    * Port 4317 (recommended) with `otlpGrpcListenerPorts` **or** port 4318 (recommended) with `otlpHttpListenerPorts`.
    * To receive the OpenTelemetry resource attributes that your application sends for metrics data, set `otlpResourceAttrsOnMetricsIncluded` to `true`.
      <br/>**Note**: Be aware that setting this to `true` increases the chance of metrics exceeding the [annotations count limit on your cluster](https://docs.wavefront.com/wavefront_limits.html#default-customer-specific-limits), causing the metrics to be dropped by the Wavefront proxy.

### Examples

The following examples run the Wavefront Proxy on Docker and send trace data to our service.

* Applications using OAuth: 
  <br/>**Note**: The proxy requires a VMware Cloud services API token with the **Proxies** service role.

  ```
  docker run -d \
  -e WAVEFRONT_URL=https://<INSTANCE>.wavefront.com/api/ \
  -e CSP_APP_ID <Your_CSP_Application_ID> \
  -e CSP_APP_SECRET <Your_CSP_Application_secret_Key> \
  -e CSP_ORG_ID <Your_CSP_Organization_ID> \
  -e JAVA_HEAP_USAGE=512m \
  - otlpGrpcListenerPorts 4317
  -p 2878:2878 \
  -p 4317:4317 \
  wavefronthq/proxy:latest
  ```
* Applications using the CSP API token:
  <br/>**Note**: The proxy requires a VMware Cloud services API token with the **Proxies** service role.

  ```
  docker run -d \
  -e WAVEFRONT_URL=https://<INSTANCE>.wavefront.com/api/ \
  -e CSP_API_TOKEN=<Your_CSP_API_Token> \
  -e JAVA_HEAP_USAGE=512m \
  - otlpGrpcListenerPorts 4317
  -p 2878:2878 \
  -p 4317:4317 \
  wavefronthq/proxy:latest
  ```

* If you are not using CSP (original Operations for Applications subscriptions), the Wavefront proxy 13.0 still supports authentication with Operations for Applications tokens.

  ```
  docker run -d \
  -e WAVEFRONT_URL=https://<INSTANCE>.wavefront.com/api/ \
  -e WAVEFRONT_TOKEN=<TOKEN> \
  -e JAVA_HEAP_USAGE=512M \
  - otlpGrpcListenerPorts 4317 \
  -p 2878:2878 \
  -p 4317:4317 \
  wavefronthq/proxy:latest
  ```

## Send and View Data

Follow these steps to send traces or metrics to our service:

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


## Metrics Conversion 

The OpenTelemetry metrics your applications send are converted to [our data format](https://docs.wavefront.com/wavefront_data_format.html) as follows:

![A table that shows how the OpenTelemetry metrics are converted to the Wavefront metrics format](images/opentelemetry_metrics_data_conversion.png)

For more information on our metrics, see [Metric Types](https://docs.wavefront.com/metric_types.html).


## Tutorials

Our OpenTelemetry GitHub repository includes specific examples for Java, Python, .NET, and more. 
 
* If you are on our Documentation, expand the tutorials section under OpenTelemetry, and try out a tutorial.
* If you are on the GitHub repository, for example, go to the `java-examples` folder and follow the steps in the README to instrument Java Apps with OpenTelemetry. 

<!-- 
## License
[Apache 2.0 License - NEEDS TO BE LINKED ONCE ADDED]()
-->

## Getting Support
* If you run into any issues with the examples, let us know by creating a GitHub issue on our [OpenTelemetry GitHub repository](https://github.com/wavefrontHQ/opentelemetry-examples).
* If you didn't find the information you are looking for in our [Documentation](https://docs.wavefront.com/), create a GitHub issue or PR in our [docs repository](https://github.com/wavefrontHQ/docs).
