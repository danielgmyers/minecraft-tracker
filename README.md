# Minecraft Server Performance Statistics Tracker

The purpose of this mod is to generate metrics useful in understanding the performance of a server. Presently, these metrics are generated:

1. Ticks Per Second (TPS) and Milliseconds Per Tick (ms/T) for the overall server.
2. TPS and ms/T for each dimension (e.g. the Overworld, the Nether, etc).
3. Total player count on the server.
4. Player count per dimension.

Any modded dimensions present on the server are automatically tracked by this mod.

The performance impact of this mod should be negligible.

## Configuration

When the server is first started, the mod will create a configuration file named `tracker.properties` in the `config` directory of the server if it does not already exist. The configuration file is pre-populated with the default values for each configuration option.

`reporter-type` - Controls the method that will be used to report performance metrics. Valid values are:
* NONE - disables statistics gathering and reporting. **This is the default.**
* APPLICATION_LOG - writes statistics to the server's log file. Typically this is the console output for the server.
* CLOUDWATCH_DIRECT - submits statistics to Amazon CloudWatch.

Again, the default is **NONE** which means if you don't change it, no data will be tracked!

`cloudwatch-metric-namespace` - When using the CLOUDWATCH_DIRECT reporter type, this configures the metrics namespace that the metrics will be stored in. Note that Amazon CloudWatch disallows namespace names that begin with "AWS". The default is **minecraft-tracker**.

## Reporting Modes

### APPLICATION_LOG

This mode uses the server's log (typically the console output) to record performance statistics. For example:

```
[22:38:00] [Server thread/INFO]: Last minute minecraft:overworld stats: 60 data points. TPS: 15 (min), 20 (avg), 25 (max). Tick durations: 15 ms (min), 34 ms (avg), 96 ms (max).
[22:38:00] [Server thread/INFO]: Last minute minecraft:the_nether stats: 60 data points. TPS: 16 (min), 20 (avg), 24 (max). Tick durations: 5 ms (min), 12 ms (avg), 62 ms (max).
[22:38:00] [Server thread/INFO]: Last minute minecraft:the_end stats: 60 data points. TPS: 16 (min), 20 (avg), 24 (max). Tick durations: 0 ms (min), 0 ms (avg), 1 ms (max).
[22:38:00] [Server thread/INFO]: Last minute server stats: 60 data points. TPS: 15 (min), 20 (avg), 25 (max). Tick durations: 27 ms (min), 48 ms (avg), 115 ms (max).
[22:38:00] [Server thread/INFO]: Last minute minecraft:overworld player counts: 60 data points. 1 (min), 1 (avg), 1 (max).
[22:38:00] [Server thread/INFO]: Last minute server player counts: 60 data points. 1 (min), 1 (avg), 1 (max).
```

The order in which these lines are logged is not guaranteed. If a dimension is not ticking (e.g. because no players are in it), the statistics line for that dimension may not be emitted at all.

### CLOUDWATCH_DIRECT

The mode submits metrics to Amazon CloudWatch. For example, you may see something like these in your CloudWatch console:

```
minecraft:overworld.minute.tick-count
minecraft:overworld.minute.tick-millis
minecraft:the_end.minute.tick-count
minecraft:the_end.minute.tick-millis
minecraft:the_nether.minute.tick-count
minecraft:the_nether.minute.tick-millis
server.minute.tick-count
server.minute.tick-millis
minecraft:overworld.minute.player-count
minecraft:the_end.minute.player-count
minecraft:the_nether.minute.player-count
server.minute.player-count
```

If a dimension is not ticking (e.g. because no players are in it), datapoints for the corresponding metrics may not be generated.

Statistics are submitted using CloudWatch's `StatisticSet` mechanism, so for any of those metrics you can look at `Minimum`, `Maximum`, `Average`, `Sum`, or `SampleCount`, but percentile statistics will not work.

API calls to Amazon CloudWatch are made asynchronously (that is, in the background) and do not impact the duration of ticks. Additionally, metrics are submitted in batches of 20 or once per minute, whichever comes first.

The mod uses the default credentials provider; see [the AWS SDK for Java documentation](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain) for more information about how credentials are located. These credentials will need the `cloudwatch:PutMetricData` permission.


## Definitions

### TPS
Ticks Per Second. Minecraft servers try to run their main game loop (or "tick") twenty times per second.

Players typically find servers frustrating to play on if average TPS drops below 10.

### ms/T
Milliseconds Per Tick. This measures how long, in milliseconds, it takes minecraft to process one run through its game loop. Ticks can take up to 50 milliseconds before TPS must drop below 20.

Players typically find servers frustrating to play on if average ms/T rises above 100ms.

