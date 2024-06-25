# byteman

Use this command to download byteman:

    ./mvnw dependency:copy

# run application

To run:

    ./mvnw spring-boot:run 

## with reactor 3.5

    ./mvnw spring-boot:run -Preactor35

## with reactor 3.6

    ./mvnw spring-boot:run -Preactor36

## with byteman

    ./mvnw spring-boot:run -Preactor36,byteman

## counting calls

    ./mvnw spring-boot:run -Preactor36,byteman | grep 'put(requestId)' | wc -l

# examples

See difference in times a value is put in MDC:

For the simple case, there is no difference in output, but many more calls:

```
./mvnw spring-boot:run -Preactor35,byteman | grep 'put(requestId)' | wc -l
     27
./mvnw spring-boot:run -Preactor36,byteman | grep 'put(requestId)' | wc -l
     135
```

For the httpClient case (which actually makes a difference, the propagation gets fixed)

```
./mvnw spring-boot:run -PhttpClient,reactor35,byteman | grep 'put(requestId)' | wc -l
       1
./mvnw spring-boot:run -PhttpClient,reactor36,byteman | grep 'put(requestId)' | wc -l
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
./mvnw spring-boot:run -Preactor36,byteman | grep 'FluxContextWriteRestoringThreadLocals.*ContextWriteRestoringThreadLocalsSubscriber\.onNext' | wc -l
     104
```