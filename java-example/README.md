# Instrumenting Java Apps with OpenTelemetry

## Auto Instrumentation

This section shows a working example of a Java application auto-instrumented with OpenTelemetry.

To auto-instrument a Java application, check
out [this guide](https://tanzu.vmware.com/content/blog/getting-started-opentelemetry-vmware-tanzu-observability#devops).

## Manual Instrumentation

This section shows a working example of a Java application manually-instrumented with OpenTelemetry API and
configuration through the OpenTelemetry SDK. By default, the OpenTelemetry API returns no-op implementations of the
classes. Configuring the OpenTelemetry SDK enables the data to be processed and exported in useful ways.

### Prerequisite

* Install the Tanzu Observability proxy. See
  this [README](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/README.md#install-wavefront-proxy).
* Set up an OpenTelemetry Collector for Tanzu Observability. See
  this [README](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/README.md#install-the-opentelemetry-collector)
  .

### Step 1: Add a Maven Project

Locate the ```pom.xml``` in ```java-example``` project in IDE, and right click and select ```Add as a Maven Project```.
We have put all the dependencies in
the [```pom.xml```](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/java-example/pom.xml)
file.

Dependencies included in the ```pom.xml``` are:

```xml

<properties>
    <version.opentelemetry-alpha>1.9.1</version.opentelemetry-alpha>
    <version.opentelemetry-semconv>1.9.1-alpha</version.opentelemetry-semconv>
    <version.opentelemetry>1.9.1</version.opentelemetry>
    <version.grpc>1.35.0</version.grpc>
</properties>

<dependencyManagement>
<dependencies>
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-bom</artifactId>
        <version>${version.opentelemetry}</version>
        <type>pom</type>
        <scope>import</scope>
    </dependency>
</dependencies>
</dependencyManagement>

<dependencies>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-api</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-protobuf</artifactId>
    <version>${version.grpc}</version>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-netty-shaded</artifactId>
    <version>${version.grpc}</version>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-semconv</artifactId>
    <version>${version.opentelemetry-semconv}</version>
</dependency>
</dependencies>
```

### Step 2: Instrument the Application

* #### Get an OpenTelemetry Interface
  The first step is to get a handle to an instance of the OpenTelemetry interface. As an application developer, we need
  to configure an instance of the OpenTelemetrySdk as early as possible in our application. This can be done using the
  OpenTelemetrySdk.builder() method.

  ```java
      static OpenTelemetry initOpenTelemetry() {
        OtlpGrpcSpanExporter spanExporter = getOtlpGrpcSpanExporter();
        BatchSpanProcessor spanProcessor = getBatchSpanProcessor(spanExporter);
        Resource serviceNameResource = Resource
                .create(Attributes.of(ResourceAttributes.SERVICE_NAME, SERVICE_NAME));
        SdkTracerProvider tracerProvider = getSdkTracerProvider(spanProcessor, serviceNameResource);
        OpenTelemetrySdk openTelemetrySdk = getOpenTelemetrySdk(tracerProvider);
        Runtime.getRuntime().addShutdownHook(new Thread(tracerProvider::shutdown));

        return openTelemetrySdk;
      }

      private static OpenTelemetrySdk getOpenTelemetrySdk(SdkTracerProvider tracerProvider) {
        OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider)
          .buildAndRegisterGlobal();
        return openTelemetrySdk;
      }

      private static SdkTracerProvider getSdkTracerProvider(BatchSpanProcessor spanProcessor, Resource serviceNameResource) {
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder().addSpanProcessor(spanProcessor)
          .setResource(Resource.getDefault().merge(serviceNameResource)).build();
        return tracerProvider;
      }

      private static BatchSpanProcessor getBatchSpanProcessor(OtlpGrpcSpanExporter spanExporter) {
        BatchSpanProcessor spanProcessor = BatchSpanProcessor.builder(spanExporter)
          .setScheduleDelay(100, TimeUnit.MILLISECONDS).build();
        return spanProcessor;
      }

      private static OtlpGrpcSpanExporter getOtlpGrpcSpanExporter() {
        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
          .setEndpoint(OTEL_COLLECTOR_ENDPOINT)
          .setTimeout(2, TimeUnit.SECONDS)
          .build();
       return spanExporter;
      }
  ```
  As an aside, if we are writing library instrumentation, it is strongly recommended that we provide our users the
  ability to inject an instance of ```OpenTelemetry``` into our instrumentation code. If this is not possible for some
  reason, we can fall back to using an instance from the ```GlobalOpenTelemetry``` class. Note that we can’t force
  end-users to configure the global, so this is the most brittle option for library instrumentation.

* #### Get a Tracer
  First, a Tracer must be acquired, which is responsible for creating spans and interacting with the Context. A tracer
  is acquired by using the OpenTelemetry API specifying the name and version of the library instrumenting the
  instrumented library or application to be monitored.
  ```java
    private static Tracer getTracer() {
        tracer = openTelemetry.getTracer(<my_instrumentation_library_name>, <my_instrumentation_library_version>);         
        return tracer;
    }
  ```
  Note: the ```my_instrumentation_library_name``` and ```my_instrumentation_library_version``` of the tracer are purely
  informational. All Tracers that are created by a single OpenTelemetry instance will interoperate, regardless of name.

* #### Create a Nested Span, Add an Attribute
  To create a basic span, we only need to specify the name of the span. The start and end time of the span is
  automatically set by the OpenTelemetry SDK. Most of the time, we want to correlate spans for nested operations. In
  OpenTelemetry spans can be created freely and it’s up to the implementor to annotate them with attributes specific to
  the represented operation. Attributes provide additional context on a span about the specific operation it tracks,
  such as results or operation properties. For the ```main``` method to call the ```child``` method, the spans could be
  manually linked in the following way:
    ```java
      public static void main(String[] args) throws InterruptedException {

        /*this will make sure that a proper service.name attribute is set on all the
         spans/metrics.*/
        System.setProperty(OTEL_RESOURCE_ATTRIBUTES_KEY, OTEL_RESOURCE_ATTRIBUTES_VALUE);

        /*tracer must be acquired, which is responsible for creating spans and interacting with the Context*/
        tracer = getTracer();

        /*an automated way to propagate the parent span on the current thread*/
        for (int index = 0; index < 3; index++) {
            /*create a span by specifying the name of the span. The start and end time of the span is automatically set by the OpenTelemetry SDK*/
            Span parentSpan = tracer.spanBuilder("parentSpan").startSpan();
            logger.info("In parent method. TraceID : {}", parentSpan.getSpanContext().getTraceIdAsHexString());

            /*put the span into the current Context*/
            try (Scope scope = parentSpan.makeCurrent()) {

                /*annotate the span with attributes specific to the represented operation, to provide additional context*/
                parentSpan.setAttribute("parentIndex", index);
                childMethod(parentSpan);
            } catch (Throwable throwable) {
                parentSpan.setStatus(StatusCode.ERROR, "Something wrong with the parent span");
            } finally {
                /*closing the scope does not end the span, this has to be done manually*/
                parentSpan.end();
            }
        }

        /*sleep for a bit to let everything settle*/
        Thread.sleep(2000);
    }

    private static void childMethod(Span parentSpan) {

        tracer = getTracer();

        /*setParent(...) is not required, `Span.current()` is automatically added as the parent*/
        Span childSpan = tracer.spanBuilder("childSpan").setParent(Context.current().with(parentSpan))
                .startSpan();
        logger.info("In child method. TraceID : {}", childSpan.getSpanContext().getTraceIdAsHexString());

        /*put the span into the current Context*/
        try (Scope scope = childSpan.makeCurrent()) {
            Thread.sleep(1000);
        } catch (Throwable throwable) {
            childSpan.setStatus(StatusCode.ERROR, "Something wrong with the child span");
        } finally {
            childSpan.end();
        }
    }
    ```
* #### More Information
  The above-mentioned example is a very basic example. Please refer
  to [the guide](https://opentelemetry.io/docs/instrumentation/java/manual_instrumentation/) for more details
  like ```events```, ```links```, ```context propagation```, etc.

### Step 3: Run the Application

Run the application either from the CLI line or from an IDE.

The ```main``` method in our Java application will trigger our app to generate and emit a trace of a transaction. When
the trace data collected from the OpenTelemetry collector are ingested, we can examine them in
the [Tanzu Observability user interface](https://docs.wavefront.com/tracing_ui_overview.html).
  
