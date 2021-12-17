using System.Diagnostics;
using OpenTelemetry;
using OpenTelemetry.Resources;
using OpenTelemetry.Trace;

var builder = WebApplication.CreateBuilder(args);

var resourceList = new List<KeyValuePair<string, object>>();
resourceList.Add(new KeyValuePair<string, object>
    ("application", "otel-otlp-.net-app"));

builder.Services.AddOpenTelemetryTracing(tracerProviderBuilder =>
{
    tracerProviderBuilder.AddAspNetCoreInstrumentation();
    tracerProviderBuilder.AddHttpClientInstrumentation();
    tracerProviderBuilder.SetResourceBuilder(ResourceBuilder.CreateDefault()
        .AddService("otel-otlp-.net-service").AddAttributes(resourceList));
    tracerProviderBuilder.AddSource("ExampleTracer");
    tracerProviderBuilder.AddOtlpExporter(options =>
    {
        options.Endpoint = new Uri("http://localhost:4317");
        options.ExportProcessorType = ExportProcessorType.Simple;
    });
});


var app = builder.Build();
// Create a route (GET /) that will make an http call, log a trace
var httpClient = new HttpClient();
var activitySource = new ActivitySource("MyApplicationActivitySource");

// ASP.NET Core starts an activity when handling a request
app.MapGet("/", async (ILogger<Program> logger) =>
{
    // The sampleActivity is automatically linked to the parent activity (the one from
    // ASP.NET Core in this case).
    // You can get the current activity using Activity.Current.
    using (var activity = activitySource.StartActivity("Get some data"))
    {
        // note that "sampleActivity" can be null here if nobody listen events generated
        // by the "SampleActivitySource" activity source.
        activity?.AddTag("sampleTag", "someTag");
        activity?.AddBaggage("sampleBaggage", "someBaggage");

        // Http calls are tracked by AddHttpClientInstrumentation
        var str = await httpClient.GetStringAsync("https://tanzu.vmware.com/observability");

        logger.LogInformation("Response1 length: {Length}", str.Length);
    }
    
    return "Hello World";
});

app.Run();
