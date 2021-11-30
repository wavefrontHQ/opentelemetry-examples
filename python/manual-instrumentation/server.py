from time import sleep

from flask import Flask
# add opentelemetry as a dependency
from opentelemetry import trace
from opentelemetry.trace.status import StatusCode

app = Flask(__name__)

# create a tracer and name it after your package
tracer = trace.get_tracer(__name__)


@app.route('/')
def index():
    # add latency to the parent span
    sleep(20 / 1000)

    # always create a new context when starting a span
    with tracer.start_as_current_span("server_span") as span:
        # add an event to the child span
        span.add_event("event message",
                       {"event_attributes": 1})
        # get_current_span will now return the same span
        trace.get_current_span().set_attribute("http.route", "some_route")
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
        span.set_status(StatusCode.ERROR)
    return "Some Exception"

app.run(host='0.0.0.0', port=80)
