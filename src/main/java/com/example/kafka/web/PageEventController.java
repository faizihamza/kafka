package com.example.kafka.web;

import com.example.kafka.entities.PageEvent;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreType;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
public class PageEventController {

    @Autowired
    private StreamBridge streamBridge;

    @Autowired
    private InteractiveQueryService interactiveQueryService;

    @GetMapping("/publish/{topic}/{name}")
    public PageEvent publish(@PathVariable String topic, @PathVariable String name){
        String user = Math.random()>0.5 ? "U1":"U2";
        PageEvent pageEvent = new PageEvent(name,user,new Date(),new Random().nextInt(9000));
        streamBridge.send(topic,pageEvent);
        return pageEvent;
    }

    @GetMapping(path = "/analytics",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String,Long>> analytics(){
        return Flux.interval(Duration.ofSeconds(1))
                .map(sequance ->{
                    Map<String,Long> stringLongHashMap = new HashMap<>();
                    ReadOnlyWindowStore<String,Long> stats = interactiveQueryService.getQueryableStore("page-count", QueryableStoreTypes.windowStore());
                    Instant startTime = Instant.now();
                    Instant from = startTime.minusSeconds(5000);
                    KeyValueIterator<Windowed<String>,Long> fetchAll= stats.fetchAll(from,startTime);
                    while (fetchAll.hasNext()){
                        KeyValue<Windowed<String>,Long> next = fetchAll.next();
                        stringLongHashMap.put(next.key.key(),next.value.longValue());
                    }
                    return stringLongHashMap;
                }).share();
    }
}
