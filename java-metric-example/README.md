# Sending Metrics from Java Apps to OpenTelemetry

This section shows a working example of a Java application that send metrics to the OpenTelemetry
collector.

**WARNING:** This app uses internal OpenTelemetry packages for creating exponential histograms. It 
should not be used in the production setting. Follow this [PR](https://github.com/open-telemetry/opentelemetry-java/pull/4472) for a public API.

### Prerequisites

* A VMware Aria Operations for Applications (formerly known as Tanzu Observability by Wavefront) account, which gives you access to a cluster.
* Clone the [OpenTelemetry Examples](https://github.com/wavefrontHQ/opentelemetry-examples) repository.
* [Install the Wavefront proxy](http://docs.wavefront.com/proxies_installing.html#install-a-proxy).
  <br/>**Note**: When running the Wavefront proxy:
  * Make sure that the `WAVEFRONT_PROXY_ARGS` environment variable contains `--otlpGrpcListenerPorts 4317`.
  * And expose the OpenTelemetry port via `-p 4317:4317`.
* Set up an OpenTelemetry Collector:
    1. Download the `otelcol-contrib` binary from the latest release of the [OpenTelemetry Collector project](https://github.com/open-telemetry/opentelemetry-collector-releases/releases).
    1. In the same directory, create a file named `otel_collector_config.yaml`.
    1. Copy the configurations in the [preconfigured YAML file](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/otel_collector_config.yaml) to the file you just created. For details on OpenTelemetry configurations, see [OpenTelemetry Collector Configuration](https://opentelemetry.io/docs/collector/configuration/).
    1. On your console, navigate to the directory you downloaded in the step above and run the following command to start OpenTelemetry Collector:
        ```
        ./otelcol-contrib --config otel_collector_config.yaml
        ```

## Send Data to Our Service

1. Open the `pom.xml` file in the `java-metric-example` directory using your IDE.
2. Right-click and select **Add as a Maven Project**.

   The [```pom.xml```](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/java-metric-example/pom.xml)
   file is configured with the required dependencies.

3. Run one of the following command using the terminal to build a jar:
   1. Cumulative Histogram
       ```
         mvn package -P cumHistogram 
       ```
   2. Delta Histogram
      ```
        mvn package -P deltaHistogram 
      ```
   3. Cumulative Exponential Histogram
       ```
         mvn package -P expCumHistogram 
       ```
   4. Delta Exponential Histogram
      ```
        mvn package -P expDeltaHistogram 
      ```
4. Run the following command to start the application:
    ```
    java -jar target/java-metric-example-1.0-SNAPSHOT.jar
    ```

The ```main``` method in this Java application triggers the application to generate and emit 
OTLP metric data.