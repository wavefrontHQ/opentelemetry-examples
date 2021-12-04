from time import sleep

from flask import Flask
from opentelemetry import trace
from opentelemetry.sdk.resources import Resource
from opentelemetry.instrumentation.flask import FlaskInstrumentor
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.trace.status import Status, StatusCode

app = Flask(__name__)

# activate flask-instrumentation during its initialization
FlaskInstrumentor().instrument_app(app)

# resource can be required for some backends like wavefront
# note: change the value of service.name attribute to your service name
resource = Resource(attributes={
    "service.name": "myPythonService"
})

span_exporter = OTLPSpanExporter(
    # optional, these are default values
    # endpoint="localhost:4317",
    # credentials=ChannelCredentials(credentials),
    # headers=(("metadata", "metadata")),
)
tracer_provider = TracerProvider(resource=resource)
trace.set_tracer_provider(tracer_provider)
span_processor = BatchSpanProcessor(span_exporter)
tracer_provider.add_span_processor(span_processor)

# Configure the tracer to use the collector exporter
tracer = trace.get_tracer_provider().get_tracer(__name__)


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


app.run(host='0.0.0.0', port=80)
