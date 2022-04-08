---
title: OpenTelemetry Tracing Data
tags: [tracing]
permalink: opentelemetry_tracing.html
summary: Send OpenTelemetry trace data to Tanzu Observability. 
---

OpenTracing and OpenCensus have merged to form OpenTelemetry. OpenTelemetry provides a single set of APIs, libraries, agents, and collector services to capture distributed traces and metrics from your application. If your application uses OpenTelemetry, you can configure the application to send traces to Tanzu Observability by Wavefront.

## How to Send Data

Before you get started, pick how you send data to Tanzu Observability by Wavefront. What your application uses determines what makes sense: 
* If your application uses SpringBoot, use Spring Cloud Sleuth.
* If you are a new user, and you are configuring your application to send data to Tanzu Observability, use OpenTelemetry. If you run into issues when configuring Tanzu Observability with OpenTelemetry, contact [Technical Support](wavefront_support_feedback.html#support) for help.
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

1. [Install the Wavefront Proxy](proxies_installing.html) version 11 or higher.
1. Open the port on the Wavefront Proxy to send OpenTelemetry spans to Tanzu Observability. 
    * port 4317 (recommended) with `otlpGrpcListenerPorts` 
    * or port 4318 (recommended) with `otlpHttpListenerPorts`  
  
    See the [Wavefront proxy settings for OpenTelemetry](proxies_configuring.html#opentelemetry-proxy-properties).
    <br/>For example, on Linux, Mac, and Windows, open the [`wavefront.conf`](proxies_configuring.html#proxy-file-paths) file, add the line `otlpGrpcListenerPorts=4317`, and save the file.
1. Configure your application to send trace data to the Wavefront Proxy. 
    {% include note.html content="By default, OpenTelemetry SDKs send data over gRPC to `http://localhost:4317`." %}
1. Explore the trace data using our [tracing dashboards](tracing_basics.html#visualize-distributed-tracing-data).


### Send Data Using the OpenTelemetry Collector

If you have already configured your application to send data to the OpenTelemetry Collector, the data flows from your application to Tanzu Observability as shown in the diagram:

{% include note.html content="You need to use OpenTelemetry Collector Contrib version v0.28.0 or later to export traces to Tanzu Observability." %} 

![Shows how the data flows from your application to the OpenTelemetry Collector to Tanzu Observability](images/opentelemetry_collector_tracing.png)

Follow these steps:

1. [Install the Wavefront Proxy](proxies_installing.html).
    {{site.data.alerts.note}}
      <ul>
      <li>
        Open port 30001, with <code>customTracingListenerPorts=30001</code>, for the proxy to generate span-level RED metrics.
       </li>
       <li>
         Ensure that port 2878 is open to send spans and metrics to the Wavefront service. For example, on Linux, Mac, and Windows, open the <a href="proxies_configuring.html#proxy-file-paths"><code>wavefront.conf</code></a> file and confirm that <code>pushListenerPorts</code> is set to 2878, and that this configuration is uncommented. 
       </li>
       
     </ul>
    {{site.data.alerts.end}}
     
1. Configure your application to send trace data to the OpenTelemetry Collector. See the [OpenTelemetry documentation](https://opentelemetry.io/docs/collector/) for details.
1. Export the data from the OpenTelemetry Collector to the Tanzu Observability (Wavefront) trace exporter:
    1. Create a directory to store all the files.
    1. Download the binary from the latest release of the [OpenTelemetry Collector project](https://github.com/open-telemetry/opentelemetry-collector-contrib/releases) to the directory you created.
    1. In the same directory, create a file named `otel_collector_config.yaml`.
    1. Copy the configurations below into the YAML file.
        ```
        receivers:
           otlp:
              protocols:
                  grpc:
                      endpoint: "<enter your IP address>:4317"
        exporters:
            tanzuobservability:
              traces:
                endpoint: "http://<enter your IP address>:30001"
              metrics:
                endpoint: "http://<enter your IP address>:2878"
          # Proxy hostname and customTracing ListenerPort
        processors:
            batch:
              timeout: 10s
            memory_limiter:
              check_interval: 1s
              limit_percentage: 50
              spike_limit_percentage: 30


        service:
            pipelines:
              metrics:
                receivers: [otlp]
                exporters: [tansuobservability]
                processors: [memory_limiter, batch]
              traces:
                receivers: [otlp]
                exporters: [tanzuobservability]
                processors: [memory_limiter, batch]
          
        ```
        {% include tip.html content="To learn more about OpenTelemetry configurations, see [OpenTelemetry Collector Configuration](https://opentelemetry.io/docs/collector/configuration/)." %}
    1. On your console, navigate to the directory you created in the step above and run the following command to start OpenTelemetry Collector:
        ```
        ./otelcontribcol_darwin_amd64 --config otel_collector_config.yaml
        ```
1. Explore the trace data sent using our [tracing dashboards](tracing_basics.html#visualize-distributed-tracing-data).


## Next Steps

- [Try out the Tutorials](opentelemetry_java_tutorial.html) and see how you can send your data to Tanzu Observability!
- To enable proxy debug logs for the OpenTelemetry data sent directly to the Wavefront Porxy, see [Enable Proxy Debug Logs for OpenTelemetry Data](opentelemetry_logs.html).
