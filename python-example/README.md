# Instrumenting Python Apps with OpenTelemetry

## Auto Instrumentation

This section shows a working example of a Python application auto-instrumented with OpenTelemetry.

### Prerequisite

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
    See [Install a Proxy](http://docs.wavefront.com/proxies_installing.html#install-a-proxy) to find other options for installing the proxy on your environment.
    
* **Set up an OpenTelemetry Collector for Tanzu Observability**:
    1. Download the `otelcol-contrib` binary from the latest release of
    the [OpenTelemetry Collector project](https://github.com/open-telemetry/opentelemetry-collector-contrib/releases) and add it to a preferred directory.
    1. Create a file named `otel_collector_config.yaml` in the same directory.
    1. Copy the configurations in the  [`otel_collector_config.yaml`](https://github.com/wavefrontHQ/opentelemetry-examples/blob/78f43e78b292c99bf00e6294712caf4ee940fc67/doc-resources/otel_collector_config.yaml) file to the new file you created.  

    See [OpenTelemetry collector configurations](https://opentelemetry.io/docs/collector/configuration/) to learn more.


**Tip:** We recommend trying [`virtualenv`](https://sourabhbajaj.com/mac-setup/Python/virtualenv.html) to create an
isolated Python environment.

### Step 1: Create or Download a Sample Application

Any application can be easily instrumented, for this walk through we will refer to a locally hosted application that
responds with “Hello, World!“ each time it is accessed.

```python
from flask import Flask

app = Flask(__name__)


@app.route('/')
def index():
    return 'Hello World'


app.run(host='0.0.0.0', port=80)
```

Let's save this file as ```server.py```.

### Step 2: Install OpenTelemetry Packages

The following OpenTelemetry packages are required to auto-instrument an application:

* ```Flask```
* ```opentelemetry-distro```
* ```opentelemetry-instrumentation```
* ```opentelemetry-bootstrap```
* ```opentelemetry-exporter-otlp```

To install these packages, run the following commands from the application directory:

```
pip3 install Flask
```

```
pip3 install opentelemetry-distro
```

```
pip3 install opentelemetry-instrumentation
```

```
opentelemetry-bootstrap --action=install
```

```
pip3 install opentelemetry-exporter-otlp
```

### Step 3: Configuring the OpenTelemetry Exporter

Now, configure the OpenTelemetry Exporter to send traces from our application to the required endpoint on our local
machine:

```
export OTEL_TRACES_EXPORTER=otlp
```

```
export OTEL_EXPORTER_OTLP_ENDPOINT="http://localhost:4317"
```

Note: Change the value of `my_service_name` and `my_application_name` attribute as per application's requirements.

```
export OTEL_RESOURCE_ATTRIBUTES="service.name=<my_service_name>,application=<my_application_name>"
```

In the above configuration, we export our trace data using OpenTelemetry Protocol (OTLP). In addition, we set the export
endpoint as ```localhost:4317``` and assign the name ```my_application_name``` to our tracing service as a resource
attribute.

### Step 4: Run the Application and Generate a Trace

The collector is now running and listening to incoming traces on port 4317. Now we’re ready to generate traces.

* Start the application

```
opentelemetry-instrument python3 server.py
```

* Visit ```http://localhost``` and refresh the page. The application generates and emits a trace of that transaction.
  When the trace data collected from the OpenTelemetry collector are ingested, we can examine them in
  the [Tanzu Observability user interface](https://docs.wavefront.com/tracing_ui_overview.html).

## Manual-Instrumentation

Automation is great, but eventually we want to add detail. This section shows a working example of a Python application
manually-instrumented with OpenTelemetry.

### Prerequisite

* Install the Tanzu Observability proxy. See
  this [README](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/README.md#install-wavefront-proxy).
* Set up an OpenTelemetry Collector for Tanzu Observability. See
  this [README](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/README.md#install-the-opentelemetry-collector)
  .

**Tip:** We recommend trying [`virtualenv`](https://sourabhbajaj.com/mac-setup/Python/virtualenv.html) to create an
isolated Python environment. Please do not use the same virtual environment if it is already used in
auto-instrumentation.

### Step 1: Install OpenTelemetry Packages

To install OpenTelemetry packages for Python, run this command:

```
pip3 install -r requirements.txt
```

**Tip:** We’ve put all the dependencies in the requirements.txt file.

### Step 2: Instrument the Application

To keep things
simple, [this example](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/python-example/server.py)
creates a basic “Hello World” application using Flask.

* #### Activate Flask Instrumentation
    * First, install the instrumentation package. already taken care of.
    * To activate flask instrumentation, run following code in the application:
      ```python
        app = Flask(__name__)
        FlaskInstrumentor().instrument_app(app)
      ```

* #### Resource Attributes
  Note: Change the value of `my_service_name` and `my_application_name` attribute as per application's requirements.
    ```python
        resource = Resource(attributes={
          "service.name": "<my_service_name>",
          "application": "<my_application_name>"
        })
    ```
* #### OTLPSpanExporter Configuration
  Note: These are default values, changing this is optional.
  ```python
    span_exporter = OTLPSpanExporter(
        # endpoint="localhost:4317",
        # credentials=ChannelCredentials(credentials),
        # headers=(("metadata", "metadata")),
        )
  ```
* #### Setup Tracer
  Tracer, an object that tracks the currently active span and allows us to create (or activate) new spans.
    ```python
      trace.set_tracer_provider(tracer_provider)
      span_processor = BatchSpanProcessor(span_exporter)
      tracer_provider.add_span_processor(span_processor)
    
      tracer = trace.get_tracer_provider().get_tracer(__name__)
    ```
* #### Creating a Child Span
  A span represents a distinct operation - not an individual function, but an entire operation, such as a database
  query. Generally, this means we shouldn't be creating spans in our application code, they should be managed as part of
  the framework or library we are using. But, that said, here is how we do it. Span management has two parts - the span
  lifetime and the span context. The lifetime is managed by starting the span with a tracer, and adding it to a trace by
  assigning it a parent.
  ```python
    @app.route('/')
    def index():
        # add latency to the parent span
        sleep(20 / 1000)

      # always create a new context when starting a span
      with tracer.start_as_current_span("child_span") as span:
        # add an event to the child span
        span.add_event("event message",
                       {"event_attributes": 1})
        # get_current_span will now return the same span
        trace.get_current_span().set_attribute("http.route", "some route")
        # add latency to the child span
        sleep(30 / 1000)
        return "Hello World"
  ```
* #### Recording Errors
  Exceptions are reported as events, but they should be properly formatted. As a convenience, OpenTelemetry provides a
  record_exception method for capturing them correctly.
    ```python
      @app.route("/exception")
      def exception():
          try:
              1 / 0
          except ZeroDivisionError as error:
              span = trace.get_current_span()
              # record an exception
              span.record_exception(error)
              # fail the operation
              span.set_status(Status(StatusCode.ERROR, "error happened"))
          return "Some Exception"
    ```

#### Step 3: Run the Application

* Start the application

```
opentelemetry-instrument python3 server.py
```

* Visit ```http://localhost:8080``` or ```http://localhost:8080/exception``` and refresh the page. The application generates and
  emits a trace of that transaction. When the trace data collected from the OpenTelemetry collector are ingested, we can
  examine them in the [Tanzu Observability user interface](https://docs.wavefront.com/tracing_ui_overview.html).
