# Sending Metrics from Java Apps to OpenTelemetry

This section shows a working example of a Java application that send metrics to the OpenTelemetry
collector.

**WARNING:** This app uses internal OpenTelemetry packages for creating exponential histograms. It 
should 
not be used in the production setting. Follow this [PR](https://github.com/open-telemetry/opentelemetry-java/pull/4472) for a public API.

### Prerequisites

* A Tanzu Observability by Wavefront account, which gives you access to a cluster. 
    If you don’t have a cluster, [sign up for a free trial](https://tanzu.vmware.com/observability-trial).
* Clone the [OpenTelemetry Examples](https://github.com/wavefrontHQ/opentelemetry-examples) repository.
* Install the Docker platform. You’ll run the Wavefront proxy on Docker for this tutorial.
* Install the Wavefront proxy on Docker.
    ```
    docker run -d \
        -e WAVEFRONT_URL=https://{INSTANCE_NAME}.wavefront.com/api/ \
        -e WAVEFRONT_TOKEN={TOKEN} \
        -e JAVA_HEAP_USAGE=512m \
        -e WAVEFRONT_PROXY_ARGS="--customTracingListenerPorts 30001" \
        -p 2878:2878 \
        -p 30001:30001 \
        wavefronthq/proxy:latest
    ```
    Replace:
    * `{INSTANCE_NAME}` with the Tanzu Observability instance (for example, https://longboard.wavefront.com).
    * `{TOKEN}` with a Tanzu Observability API token linked to an account with Proxy permission.
      See [Generating and an API Token](https://docs.wavefront.com/wavefront_api.html#generating-an-api-token).
    
    See [Install a Proxy](http://docs.wavefront.com/proxies_installing.html#install-a-proxy) to find other options for installing the proxy on your environment.
    
* Set up an OpenTelemetry Collector for Tanzu Observability:
    1. Download the `otelcol-contrib` binary from the latest release of the [OpenTelemetry Collector project](https://github.com/open-telemetry/opentelemetry-collector-releases/releases).
    1. In the same directory, create a file named `otel_collector_config.yaml`.
    1. Copy the configurations in the [preconfigured YAML file](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/otel_collector_config.yaml) to the file you just created. For details on OpenTelemetry configurations, see [OpenTelemetry Collector Configuration](https://opentelemetry.io/docs/collector/configuration/).
    1. On your console, navigate to the directory you downloaded in the step above and run the following command to start OpenTelemetry Collector:
        ```
        ./otelcol-contrib --config otel_collector_config.yaml
        ```

## Send Data to Tanzu Observability

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