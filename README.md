# byteman

Use this command to download byteman:

    ./mvnw dependency:copy

# run application

To run:

    ./mvnw spring-boot:run -Pgenerator

Choose profile from: `generator,http,http-efficient`.

* `generator` - simple generator for characters
* `http` - uses JDK http client to show that with 3.6 the context is present and missing with 3.5
* `http-efficient` - a more efficient way of using JDK http client for this specific case, bypassing some Flux processing

Choose second profile for version:

* `reactor35`
* `reactor36`
* `reactor396s`

Add `byteman` profile to include byteman profiling to count calls to certain methods, e.g. `MDC.put(...)`

## check dependencies

    ./mvnw dependency:tree -Preactor35
    ./mvnw dependency:tree -Preactor36
    ./mvnw dependency:tree -Preactor369s

## with reactor 3.5

    ./mvnw spring-boot:run -Preactor35

## with reactor 3.6

    ./mvnw spring-boot:run -Preactor36

## with reactor 3.6.9-SNAPSHOT

    ./mvnw spring-boot:run -Preactor369s

## with byteman

    ./mvnw spring-boot:run -Preactor36,byteman

## counting calls

    ./mvnw spring-boot:run -Pgenerator,reactor36,byteman | grep 'put(requestId)' | wc -l

# examples

See difference in times a value is put in MDC, we look at `MDC.put("requestId")` only, there are more calls for `traceId`. 

For the generate characters case, there is no difference in output, but _many_ more calls:

```
./mvnw spring-boot:run -Pgenerator,reactor35,byteman | grep 'put(requestId)' | wc -l
     27
./mvnw spring-boot:run -Pgenerator,reactor36,byteman | grep 'put(requestId)' | wc -l
     135
```

For the `http` case (which actually makes a difference, the propagation gets fixed)

```
./mvnw spring-boot:run -Phttp,reactor35,byteman | grep 'put(requestId)' | wc -l
       1
./mvnw spring-boot:run -Phttp,reactor36,byteman | grep 'put(requestId)' | wc -l
     25
```

Most calls seems to originate from `FluxContextWriteRestoringThreadLocals$ContextWriteRestoringThreadLocalsSubscriber.onNext`:

```
[BYTEMAN] org.slf4j.MDC#put(requestId) in thread HttpClient-1-Worker-0
org.slf4j.MDC.put(MDC.java:128)
nl.stokpop.performance.reactive.ReactiveDemoStandAlone._init_$lambda$9(ReactiveDemoStandAlone.kt:36)
io.micrometer.context.ContextRegistry$1.setValue(ContextRegistry.java:121)
io.micrometer.context.DefaultContextSnapshot.setThreadLocal(DefaultContextSnapshot.java:102)
io.micrometer.context.DefaultContextSnapshotFactory.setAllThreadLocalsFrom(DefaultContextSnapshotFactory.java:125)
io.micrometer.context.DefaultContextSnapshotFactory.setThreadLocalsFrom(DefaultContextSnapshotFactory.java:109)
reactor.core.publisher.ContextPropagation.setThreadLocals(ContextPropagation.java:85)
reactor.core.publisher.FluxContextWriteRestoringThreadLocals$ContextWriteRestoringThreadLocalsSubscriber.onNext(FluxContextWriteRestoringThreadLocals.java:117)
org.reactivestreams.FlowAdapters$FlowToReactiveSubscriber.onNext(FlowAdapters.java:211)
```

```
./mvnw spring-boot:run -Pgenerator,reactor35,byteman | grep 'FluxContextWriteRestoringThreadLocals.*ContextWriteRestoringThreadLocalsSubscriber\.onNext' | wc -l
     0
```

```
./mvnw spring-boot:run -Pgenerator,reactor36,byteman | grep 'FluxContextWriteRestoringThreadLocals.*ContextWriteRestoringThreadLocalsSubscriber\.onNext' | wc -l
     104
```