package com.example.demo.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.handler.predicate.RoutePredicateFactory;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
public class RouteManager implements ApplicationEventPublisherAware {
    @Autowired
    private RouteDefinitionWriter routeDefinitionWriter;
    @Autowired
    RouteDefinitionLocator locator;
    private ApplicationEventPublisher publisher;
    @Autowired
    private PathRoutePredicateFactory pathRoutePredicateFactory;

    public void addRoute(String routeId, String uri, String path) {

        locator.getRouteDefinitions()
                .onErrorComplete()
                .filter(r -> r.getId().equals(routeId))
                .map(r-> {
                    log.info("addRoute() exists route");
                    var predicateDefinition = new PredicateDefinition();
                    var predicates = new ArrayList<PredicateDefinition>();
                    predicateDefinition.addArg("Path", path);
                    predicateDefinition.setName("Path");
                    predicates.addAll(r.getPredicates());
                    predicates.add(predicateDefinition);
                    r.setUri(URI.create(uri));
                    r.setPredicates(predicates);
                    return r;
                })
                .any(r -> r.getId().equals(routeId))
                .filter(existsRoute -> !existsRoute.booleanValue())
                .map(existsRoute -> {
                    log.info("addRoute() not exists route");
                    var routeDefinition = new RouteDefinition();
                    routeDefinition.setId(routeId);
                    routeDefinition.setUri(URI.create(uri));
                    routeDefinition.setPredicates(Stream.of(new PredicateDefinition())
                            .peek(p->p.addArg("Path",path))
                            .peek(p->p.setName("Path"))
                            .toList());
                    routeDefinitionWriter.save(Mono.just(routeDefinition)).subscribe();
                    return existsRoute;
                })
                .onErrorResume(throwable -> {
                    log.error("message = {}",throwable.getMessage());
                    return Mono.error(throwable);
                })
                .subscribe();

        this.publisher.publishEvent(new RefreshRoutesEvent(this));
    }

    public void removeRoute(String routeId) {
        routeDefinitionWriter.delete(Mono.just(routeId)).subscribe();
    }

    public Mono<List<RouteDefinition>> getRoutes() {
        return locator.getRouteDefinitions().collectList();
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }
}
