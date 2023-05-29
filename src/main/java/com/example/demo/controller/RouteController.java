package com.example.demo.controller;

import com.example.demo.common.RouteManager;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
public class RouteController {
    @Autowired
    RouteManager routeManager;

    @PostMapping(path = "/gateway/routes/{routeId}"
                ,consumes = MediaType.APPLICATION_JSON_VALUE
                ,produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public BaseResponse addRoute(@PathVariable String routeId, @RequestBody RequestRoute requestRoute) {
        routeManager.addRoute(routeId,requestRoute.routeUri(), requestRoute.routePath());
        return new BaseResponse(0,"success", null);
    }

    @GetMapping("/gateway/routes")
    @ResponseBody
    public Mono<BaseResponse> getRoutes() {
        return routeManager.getRoutes().map(routes -> new BaseResponse(0,"success", routes));
    }
}
record RequestRoute(String routeUri, String routePath, String apiName){}
@JsonInclude(JsonInclude.Include.NON_NULL)
record BaseResponse(int errorCode, String errorMessage,Object result) {}

