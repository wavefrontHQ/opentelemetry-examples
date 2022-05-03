# Auto Instrumenting Redis Java App with OpenTelemetry

This section shows a working example of a Redis-Java application auto-instrumented with OpenTelemetry.

### Prerequisite

* Install the Tanzu Observability proxy. See
  this [README](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/README.md#install-wavefront-proxy).
* Set up an OpenTelemetry Collector for Tanzu Observability. See
  this [README](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/README.md#install-the-opentelemetry-collector)
  .

### Step 1: Add a Maven Project

Locate the ```pom.xml``` in ```redis-example``` project in IDE, and right click and select ```Add as a Maven Project```.
We have put all the dependencies in
the [```pom.xml```](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/redis-example/pom.xml)
file.

### Step 2: Run the Application

Build the jar file: 

`cd redis-example`

`mvn clean package`


Run the application:

`java -javaagent:<path to otel agent> -Dotel.service.name=employeeService -Dotel.resource.attributes=application=employeeApp -Dotel.traces.exporter=otlp -Dotel.metrics.exporter=otlp -jar ./target/redis-example.jar`

The ```main``` method in our Java application will trigger our app to generate and emit a trace of a transaction. When
the trace data collected from the OpenTelemetry collector are ingested, we can examine them in
the [Tanzu Observability user interface](https://docs.wavefront.com/tracing_ui_overview.html).
  
