# Instrumenting .NET Apps with OpenTelemetry

## Auto-Instrumentation

We will go through a working example of a .NET application auto-instrumented with OpenTelemetry. To keep things simple,
we will create a basic “Hello World” web application, instrument it with OpenTelemetry library to generate trace data
and send it to an OpenTelemetry Collector. The Collector will then export the trace data to the Wavefront Proxy which
will eventually export the trace data to the Tanzu Observability UI.

![Here is how it works:](https://github.com/wavefrontHQ/opentelemetry-examples/blob/master/resources/TraceFlow.png?raw=true)

If we have not set up an OpenTelemetry Collector or Wavefront proxy yet, then check
out [this guide](https://github.com/wavefrontHQ/opentelemetry-examples/blob/main/README.md).

#### Step 1: Get our example application

The instrumentation works with any application, for this walk through we will refer to the following simple application.
Our example application is a locally hosted server that responds with “Hello, World!“ every time we access it.

```c#
using Microsoft.AspNetCore.Builder;

var builder = WebApplication.CreateBuilder(args);

var app = builder.Build();

app.MapGet("/", () => "Hello World!");

app.Run();
```

Let's save this file as ```Program.cs```.

#### Step 2: Installing OpenTelemetry Components

Several libraries complement the .NET [OpenTelemetry](https://www.nuget.org/packages/OpenTelemetry/) implementation that
makes integration straightforward. For instrumenting tracing in ASP.NET Core, we
use [OpenTelemetry.Instrumentation.AspNetCore](https://www.nuget.org/packages/OpenTelemetry.Instrumentation.AspNetCore/)
. In our service, we have used the following packages:

* OpenTelemetry.Exporter.OpenTelemetryProtocol: To export our traces to our OpenTelemetry Collector using OpenTelemetry
  Protocol (OTLP).
  ```
  dotnet add package OpenTelemetry.Exporter.OpenTelemetryProtocol --version 1.1.0
  ```
* OpenTelemetry.Instrumentation.AspNetCore: To collect telemetry about incoming web requests.
  ```
  dotnet add package OpenTelemetry.Instrumentation.AspNetCore --version 1.0.0-rc8
  ```
* OpenTelemetry.Instrumentation.Http: To collect telemetry about outgoing web requests.
  ```
  dotnet add package OpenTelemetry.Instrumentation.Http --version 1.0.0-rc8
  ```
* OpenTelemetry.Extensions.Hosting: To register the .NET OpenTelemetry provider.
  ```
  dotnet add package OpenTelemetry.Extensions.Hosting --version 1.0.0-rc8
  ```

#### Step 3: Configure the Trace Provider

Now we can enable the instrumentation with a single block of code in our startup to:

* Add a trace provider for OpenTelemetry
* Set the service name we want to appear in the trace. Note: change the service-name/application as per application's
  requirements.
* Add the ASP.NET Core instrumentation
* Add an exporter using the OpenTelemetry protocol (OTLP) over gRPC pointing to the OpenTelemetry Collector instance

The code looks like:

```c#
var resourceList = new List<KeyValuePair<string, object>>();
resourceList.Add(new KeyValuePair<string, object>
    ("application", "otel-otlp-.net-app"));
    
builder.Services.AddOpenTelemetryTracing(tracerProviderBuilder =>
{
    tracerProviderBuilder.AddAspNetCoreInstrumentation();
    tracerProviderBuilder.SetResourceBuilder(ResourceBuilder.CreateDefault()
        .AddService("otel-otlp-.net-service").AddAttributes(resourceList));
    tracerProviderBuilder.AddOtlpExporter(options =>
    {
        options.Endpoint = new Uri("http://localhost:4317");
        options.ExportProcessorType = ExportProcessorType.Simple;
    });
});
opentelemetry-bootstrap --action=install
```

That’s all the coding we need! The libraries we used above provide auto-instrumentation of all the incoming and outgoing
web requests.

#### Step 4: Run our application

The collector is now running and listening to incoming traces on port 4317.

Our next step is to start our application either from the CLI or from our IDE. All that is left for us to do at this
point is to visit ```https://localhost:7205``` and refresh the page, triggering our app to generate and emit a trace of
that transaction. When the trace data collected from the OpenTelemetry collector are ingested, we can examine them in
the Tanzu Observability user interface.

## Manual-Instrumentation

Getting all our web requests instrumented was super simple with auto-instrumentation. But there might be lots going on
in our services, and it would be helpful if we broke the span down into parts for finer-grain tracing. To do this, we
can add additional spans manually over sections of the code.

#### Prerequisite

If we have not set up an OpenTelemetry Collector or Wavefront proxy yet, then check
out [this guide](https://github.com/wavefrontHQ/opentelemetry-examples/blob/main/README.md).

#### Step 1: Get our example application

Locate the ```WebApp``` web-application in the ```DOTNET-example``` directory.

#### Step 2: Installing OpenTelemetry Components

Note: Follow the ```Step 2``` mentioned in the ```Auto-Instrumentation``` section.

#### Step 3: Configure the Trace Provider

Note: Follow the ```Step 3``` mentioned in the ```Auto-Instrumentation``` section.

#### Step 4: Add a tracer, create a span

* System.Diagnostics.ActivitySource represents
  an [OpenTelemetry Tracer](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/api.md#tracer)
  ```c#
  var activitySource = new ActivitySource("MyApplicationActivitySource");
  ```
* System.Diagnostics.Activity represents
  an [OpenTelemetry Span](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/api.md#span)
  ```c#
  using (var activity = activitySource.StartActivity("Get some data")){}
  ```

* add some info to the activity using ```AddTag```. This data will be exported to Tanzu Observability.
  ```c#
  activity?.AddTag("sampleTag", "someTag");
  ```
* add baggage using ```AddBagage```. Baggage will flow to child activities. This could be useful to flow a correlation
  id to all child activities, even the ones started on other services.
  ```c#
  activity?.AddBaggage("sampleBaggage", "someBaggage");
  ```

#### Step 5: Run our application

Run the below commands to start our application either from the CLI.

```
cd DOTNET-example
```

```
dotnet run --project WebApp  
```

All that is left for us to do at this point is to visit ```https://localhost:7205``` and refresh the page, triggering
our app to generate and emit a trace of that transaction. When the trace data collected from the OpenTelemetry collector
are ingested, we can examine them in the Tanzu Observability user interface.