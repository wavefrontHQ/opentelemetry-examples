# Instrumenting Java Apps with OpenTelemetry

## Auto-Instrumentation

For instrumentation, we will use the Java agent provided by OpenTelemetry, which can be attached to any Java
application. This agent will dynamically inject bytecode to collect telemetry data so that you don’t need to add any
manual instrumentation.

If you have not tried Auto-instrumenting your Java application yet, then you can check
out [this guide](https://tanzu.vmware.com/content/blog/getting-started-opentelemetry-vmware-tanzu-observability#devops).

## Manual-Instrumentation

Libraries that want to export telemetry data using OpenTelemetry MUST only depend on the opentelemetry-api package and
should never configure or depend on the OpenTelemetry SDK. The SDK configuration must be provided by Applications which
should also depend on the opentelemetry-sdk package, or any other implementation of the OpenTelemetry API. This way,
libraries will obtain a real implementation only if the user application is configured for it. For more details, check
out the [Library Guidelines](https://opentelemetry.io/docs/reference/specification/library-guidelines/).

#### Prerequisite: Installing OpenTelemetry Components

Note: If you have not installed an OpenTelemetry Collector or Wavefront proxy yet, then check
out [this guide](https://github.com/wavefrontHQ/opentelemetry-examples/blob/main/README.md).

To ease the process of installing OpenTelemetry components, we have put all the dependencies in
the [```pom.xml```](https://github.com/wavefrontHQ/opentelemetry-examples/blob/main/java/pom.xml)
file.

Dependencies that need to be included in the ```pom.xml``` are:

```xml

<properties>
    <version.opentelemetry-alpha>0.15.0-alpha</version.opentelemetry-alpha>
    <version.opentelemetry-semconv>1.9.0-alpha</version.opentelemetry-semconv>
    <version.opentelemetry>0.15.0</version.opentelemetry>
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

#### Step 1: Instrument your application

* #### Get OpenTelemetry interface
    * The first step is to get a handle to an instance of the OpenTelemetry interface. If you are an application
      developer, you need to configure an instance of the OpenTelemetrySdk as early as possible in your application.
      This can be done using the OpenTelemetrySdk.builder() method.

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
      As an aside, if you are writing library instrumentation, it is strongly recommended that you provide your users
      the ability to inject an instance of ```OpenTelemetry``` into your instrumentation code. If this is not possible
      for some reason, you can fall back to using an instance from the ```GlobalOpenTelemetry``` class. Note that you
      can’t force end-users to configure the global, so this is the most brittle option for library instrumentation.

* #### Tracing
  First, a Tracer must be acquired, which is responsible for creating spans and interacting with the Context. A tracer
  is acquired by using the OpenTelemetry API specifying the name and version of the library instrumenting the
  instrumented library or application to be monitored.
  ```java
    private static Tracer getTracer() {
        tracer = openTelemetry.getTracer(INSTRUMENTATION_LIBRARY_NAME, INSTRUMENTATION_VERSION);         
        return tracer;
    }
  ```
  Note: the ```name``` and ```optional version``` of the tracer are purely informational. All Tracers that are created
  by a single OpenTelemetry instance will interoperate, regardless of name.

* #### Create a nested span, add attributes
  Note: These are default values, changing this is optional.
  ```python
    span_exporter = OTLPSpanExporter(
        # endpoint="localhost:4317",
        # credentials=ChannelCredentials(credentials),
        # headers=(("metadata", "metadata")),
        )
  ```
* #### Setup tracer
  Tracer, an object that tracks the currently active span and allows you to create (or activate) new spans.
    ```python
      trace.set_tracer_provider(tracer_provider)
      span_processor = BatchSpanProcessor(span_exporter)
      tracer_provider.add_span_processor(span_processor)
    
      tracer = trace.get_tracer_provider().get_tracer(__name__)
    ```
* #### Creating a child span
  To create a basic span, you only need to specify the name of the span. The start and end time of the span is
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
* #### More info
  The above-mentioned example is a very basic example. Please refer
  to [the guide](https://opentelemetry.io/docs/instrumentation/java/manual_instrumentation/) for more details
  like ```events```, ```links```, ```context propagation```, etc.

#### Step 2: Run your application

The collector is now running and listening to incoming traces on port 4317. Just start your application either from the
CMD line or from an IDE.

The ```main``` method in our Java application will trigger our app to generate and emit a trace of a transaction. When
the trace data collected from the OpenTelemetry collector are ingested, you can examine them in the Tanzu Observability
user interface.
  
