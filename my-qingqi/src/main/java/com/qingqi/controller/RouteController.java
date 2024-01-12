package com.qingqi.controller;

import cn.hutool.core.map.MapUtil;
import com.qingqi.pojo.Route;
import com.qingqi.service.RouteService;
import com.qingqi.utils.DistanceUtils;
import com.qingqi.vo.ErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("route")
public class RouteController {
    @Autowired
    private RouteService routeService;

    /**
     * create route
     *
     * @return
     */
    @PostMapping
    public Object createRoute(){
        String routeId = this.routeService.createRoute();
        if (null != routeId) {
//            Created successfully
            return MapUtil.builder("routeId", routeId).build();
        }
//            Creation failure
        return ErrorResult.builder().errCode("500").errMessage("Failed to create a line！").build();
    }

    @DeleteMapping("{routeId}")
    public Object deleteRoute(@PathVariable("routeId") String routeId){
        Boolean result = this.routeService.deleteRoute(routeId);
        if (result) {
//            successfully delete
            return null;
        }
//        fail to delete
        return ErrorResult.builder().errCode("500").errMessage("Line deletion failure！").build();
    }

    /**
     * update route
     *
     * @param routeId
     * @param title
     * @return
     */
    @PutMapping
    public Object updateRoute(@RequestParam("routeId")String routeId, @RequestParam("title")String title) {
        return this.routeService.updateRoute(routeId, title);
    }

    /**
     * Query route data based on route ID
     *
     * @param routeId       Route ID
     * @param longitude     The longitude of the current user, which is used to calculate the distance between the current user and the route
     * @param latitude      Latitude of the current user
     * @return
     */
    @GetMapping("{routeId}")
    public Object queryRoute(@PathVariable("routeId") String routeId,
                             @RequestParam(value = "longitude",required = false) Double longitude,
                             @RequestParam(value = "latitude", required = false) Double latitude){
        Route route = this.routeService.queryRouteById(routeId);
        if (null != route) {
            if (longitude != null && latitude != null) {
                //            Calculate the distance between the current user and the route
                double distance = DistanceUtils.getDistance(longitude, latitude,
                        route.getLocation().getX(), route.getLocation().getY());
                route.setRouteDistance(distance);
            }
            return route;
        }
        return ErrorResult.builder()
                .errCode("404")
                .errMessage("Route does not exist!").build();
    }

    /**
     * Submission route
     *
     * @param routeId
     * @return
     */
    @PutMapping("share/{routeId}")
    public Object shareRoute(@PathVariable("routeId") String routeId) {
        Boolean result = this.routeService.shareRoute(routeId);
        if (result) {
            return null;
        }
        return ErrorResult.builder()
                .errCode("500")
                .errMessage("Submission route failed！").build();
    }

    /**
     * Find nearby routes
     *
     * @param longitude Longitude of the current user's location
     * @param latitude  Latitude of the current user location
     * @param distance  The unit is km. The default value is 10km
     * @return
     */
    @GetMapping("near")
    public Object queryNearRoute(@RequestParam("longitude") Double longitude,
                                 @RequestParam("latitude") Double latitude,
                                 @RequestParam(value = "distance", defaultValue = "10") Double distance) {
        return this.routeService.queryNearRoute(longitude, latitude, distance);
    }

    /**
     * Route fellow
     *
     * @param routeId
     * @return
     */
    @GetMapping("nearUser/{routeId}")
    public Object queryRouteNearUser(@PathVariable("routeId") String routeId) {
        return this.routeService.queryRouteNearUser(routeId);
//        return MapUtil.builder()
//                .put("count", 0)
//                .put("records", Collections.EMPTY_LIST).build();
    }

    /**
     * Start moving along the route
     * @param routeId   Destination route id
     * @return          id of a newly created route
     */
    @PostMapping("{routeId}")
    public Object runFromRoute(@PathVariable("routeId")String routeId){
        String myRouteId = this.routeService.runFromRoute(routeId);
        if (null == myRouteId) {
            return ErrorResult.builder().errCode("500").errMessage("Failure to start the movement along the route！").build();
        }
        return MapUtil.builder("routeId", myRouteId).build();
    }

    /**
     * Historical route
     *
     * @param userId
     * @return
     */
    @GetMapping("history")
    public Object queryHistoryRoute(@RequestParam(value = "userId", required = false) Long userId,
                                    @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                    @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return this.routeService.queryHistoryRoute(userId, pageNum, pageSize);
    }

    /**
     * Historical routes (shown by date)
     *
     * @return
     */
    @GetMapping("history/date")
    public Object queryHistoryRouteByDate(@RequestParam(value = "userId", required = false) Long userId,
                                          @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                          @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return this.routeService.queryHistoryRouteByDate(userId, pageNum, pageSize);
    }
}
