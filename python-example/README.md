# Instrumenting Python Apps with OpenTelemetry

## Auto Instrumentation

We will go through a working example of a Python application auto-instrumented with OpenTelemetry. To keep things
simple, we will create a basic “Hello World” application using Flask, instrument it with OpenTelemetry’s Python client
library to generate trace data and send it to an OpenTelemetry Collector. The Collector will then export the trace data
to the Wavefront Proxy which will eventually export the trace data to the Tanzu Observability UI.

![Here is how it works:](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/resources/TraceFlow.png?raw=true)

If you have not set up an OpenTelemetry Collector or Wavefront proxy yet, then check
out [this guide](https://github.com/wavefrontHQ/opentelemetry-examples/blob/main/README.md).

#### Step 1: Get your example application

You can easily instrument your application, but if you do not have one then refer to a following simple application. Our
example application is a locally hosted server that responds with “Hello, World!“ every time we access it.

```python
from flask import Flask

app = Flask(__name__)


@app.route('/')
def index():
    return 'Hello World'


app.run(host='0.0.0.0', port=80)
```

Let's save this file as ```server.py```.

#### Step 2: Installing OpenTelemetry Components

In our next step, we will need to install all OpenTelemetry components that are required to auto-instrument our
application:

* ```opentelemetry-distro```
* ```opentelemetry-instrumentation```

To install these packages, we run the following commands from our application directory:

```
pip3 install opentelemetry-distro
```

```
pip3 install opentelemetry-instrumentation
```

These packages provide good automatic instrumentation of our web requests, which in our case are also based on Flask.
This means that we don’t need to change anything in our Python application to capture and emit trace data.

#### Step 3: Installing Application-Specific OpenTelemetry Packages

In this step, we will run a command to install all instrumented packages used in our application. To do this, we need to
run the following command from our application directory:

```
opentelemetry-bootstrap --action=install
```

#### Step 4: Installing and Configuring the OpenTelemetry Exporter

Now, we need to install the OpenTelemetry exporter and configure it to send traces from our application to the required
endpoint on our local machine. Let’s install the exporter first:

```
pip3 install opentelemetry-exporter-otlp
```

Now, we are going to configure environment variables specific to our exporter:

```
export OTEL_TRACES_EXPORTER=otlp
```

```
export OTEL_EXPORTER_OTLP_ENDPOINT="http://localhost:4317"
```

Note: Change the value of 'service.name' attribute to your desired service name.

```
export OTEL_RESOURCE_ATTRIBUTES="service.name=myApplication"
```

In the above configuration, we export our trace data using OpenTelemetry Protocol (OTLP). In addition, we set the export
endpoint as ```localhost:4317``` and assign the name ```myApplication``` to our tracing service as a resource attribute.

#### Step 5: Run your application

The collector is now running and listening to incoming traces on port 4317.

Our next step is to start our application:

```
opentelemetry-instrument python3 server.py
```

All that is left for us to do at this point is to visit [localhost](http://localhost) and refresh the page, triggering
our app to generate and emit a trace of that transaction. When the trace data collected from the OpenTelemetry collector
are ingested, you can examine them in the Tanzu Observability user interface.

## Manual-Instrumentation

Okay, automation is great, but eventually you are going to want to add detail. Spans are already decorated with
standardized attributes, but once you’re settled in, you will want to start adding more detail. In some cases, you may
want to augment the auto-instrumentation with manual instrumentation in your python code in order to collect more
fine-grained trace data on specific pieces of your code.

#### Prerequisite: Installing OpenTelemetry Components

To ease this process, we have put all the dependencies in
the [```requirements.txt```](https://github.com/wavefrontHQ/opentelemetry-examples/blob/main/python/requirements.txt)
file. All you need to do is run the following command.

```
pip3 install -r requirements.txt
```

#### Step 1: Instrument your application

To keep things simple, we will create a basic “Hello World” application using Flask, please do refer an
application [```server.py```](https://github.com/wavefrontHQ/opentelemetry-examples/blob/main/python/server.py).

* #### Activate Flask instrumentation
    * First, install the instrumentation package(already taken care of, by ```requirement.txt```). If not, then run
      below command

      ```pip3 install opentelemetry-instrumentation-flask```
    * To activate flast instrumentation, run following code in the application:
      ```FlaskInstrumentor().instrument_app(app)```

* #### Resource attributes
  Note: Change the value of 'service.name' attribute to your desired service name.
    ```python
        resource = Resource(attributes={
        "service.name": "myPythonService"
        })
    ```
* #### OTLPSpanExporter configuration
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
  A span represents a distinct operation - not an individual function, but an entire operation, such as a database
  query. Generally, this means you shouldn't be creating spans in your application code, they should be managed as part
  of the framework or library you are using. But, that said, here is how you do it. Span management has two parts - the
  span lifetime and the span context. The lifetime is managed by starting the span with a tracer, and adding it to a
  trace by assigning it a parent.
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
* #### Recording errors
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

 
#### Step 2: Run your application
The collector is now running and listening to incoming traces on port 4317.

Our next step is to start our application:

```
python3 server.py
```

All that is left for us to do at this point is to visit [localhost](http://localhost)/[exception](http://localhost/exception) and refresh the page, triggering
our app to generate and emit a trace of that transaction. When the trace data collected from the OpenTelemetry collector
are ingested, you can examine them in the Tanzu Observability user interface.
  
