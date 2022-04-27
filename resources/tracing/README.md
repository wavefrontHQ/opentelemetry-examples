OpenTracing and OpenCensus have merged to form OpenTelemetry. OpenTelemetry provides a single set of APIs, libraries, agents, and collector services to capture distributed traces and metrics from your application. If your application uses OpenTelemetry, you can configure the application to send traces to Tanzu Observability by Wavefront.

## How to Send Data

Before you get started, pick how you send data to Tanzu Observability by Wavefront. What your application uses determines what makes sense: 
* If your application uses SpringBoot, use Spring Cloud Sleuth.
* If you are a new user, and you are configuring your application to send data to Tanzu Observability, use OpenTelemetry. If you run into issues when configuring Tanzu Observability with OpenTelemetry, contact [Technical Support](https://docs.wavefront.com/wavefront_support_feedback.html#support) for help.
* If your application is already using OpenTracing, continue using OpenTracing. See [OpenTracing Compatibility](https://opentelemetry.io/docs/reference/specification/compatibility/opentracing) for guidance on transitioning from OpenTracing to OpenTelemetry.

## Send Trace Data

If your application uses an OpenTelemetry SDK, you can configure the application to send trace data to Tanzu Observability:
* Using the OpenTelemetry Collector 
* Or by directly sending OpenTelemetry data to the Wavefront proxy. 

You can then use our tracing dashboards to visualize any request as a trace, which consists of a hierarchy of spans. This visualization helps you pinpoint where the request is spending most of its time and discover problems.

### Send Data Using the Wavefront Proxy - (Recommended) 

Send data from your application to the Wavefront Proxy. This is the recommended and simplest approach to get your data into Tanzu Observability.

Here's how the data flows:
![Shows how the data flows from your application to Tanzu Observability](images/opentelemetry_proxy_tracing.png)

Follow these steps:

1. [Install the Wavefront Proxy](https://docs.wavefront.com/proxies_installing.html) version 11 or higher.
1. Open the port on the Wavefront Proxy to send OpenTelemetry spans to Tanzu Observability. 
    * port 4317 (recommended) with `otlpGrpcListenerPorts` 
    * or port 4318 (recommended) with `otlpHttpListenerPorts`  
  
    See the [Wavefront proxy settings for OpenTelemetry](https://docs.wavefront.com/proxies_configuring.html#opentelemetry-proxy-properties).
    <br/>For example, on Linux, Mac, and Windows, open the [`wavefront.conf`](https://docs.wavefront.com/proxies_configuring.html#proxy-file-paths) file, add the line `otlpGrpcListenerPorts=4317`, and save the file.
1. Configure your application to send trace data to the Wavefront Proxy. 
    <br/>**Note**: By default, OpenTelemetry SDKs send data over gRPC to `http://localhost:4317`.
1. Explore the trace data using our [tracing dashboards](https://docs.wavefront.com/tracing_basics.html#visualize-distributed-tracing-data).


### Send Data Using the OpenTelemetry Collector

If you have already configured your application to send data to the OpenTelemetry Collector, the data flows from your application to Tanzu Observability as shown in the diagram:

**Note**: You need to use OpenTelemetry Collector Contrib version v0.28.0 or later to export traces to Tanzu Observability." %} 

![Shows how the data flows from your application to the OpenTelemetry Collector to Tanzu Observability](images/opentelemetry_collector_tracing.png)

Follow these steps:

1. [Install the Wavefront Proxy](https://docs.wavefront.com/proxies_installing.html).
    **Note**:
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
    1. Create a directory to store all the files.
    1. Download the binary from the latest release of the [OpenTelemetry Collector project](https://github.com/open-telemetry/opentelemetry-collector-contrib/releases) to the directory you created.
    1. In the same directory, create a file named `otel_collector_config.yaml`.
    1. Copy the configurations in the [preconfigured YAML file](https://github.com/wavefrontHQ/opentelemetry-examples/blob/78f43e78b292c99bf00e6294712caf4ee940fc67/resources/otel_collector_config.yaml) to the file you just created. For details on OpenTelemetry configurations, see [OpenTelemetry Collector Configuration](https://opentelemetry.io/docs/collector/configuration/).
    1. On your console, navigate to the directory you created in the step above and run the following command to start OpenTelemetry Collector:
        ```
        ./otelcontribcol_darwin_amd64 --config otel_collector_config.yaml
        ```
1. Explore the trace data sent using our [tracing dashboards](https://docs.wavefront.com/tracing_basics.html#visualize-distributed-tracing-data).


## Next Steps

- [Try out the Tutorials](https://docs.wavefront.com/opentelemetry_java_tutorial.html) and see how you can send your data to Tanzu Observability!
- To enable proxy debug logs for the OpenTelemetry data sent directly to the Wavefront Porxy, see [Enable Proxy Debug Logs for OpenTelemetry Data](https://docs.wavefront.com/opentelemetry_logs.html).
