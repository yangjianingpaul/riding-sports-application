# cycling route

## Start riding
![](/resources/start_riding.png)

- When the user clicks the 'Start Ride' button, the back-end service that creates the route is invoked.
- Creating a route involves two aspects of processing:
    - Route data is stored in the MongoDB table. The entity is Route, and the corresponding table is tb_route.
    - Call Baidu Map Yingyan track service to create the entity corresponding to the route.

## Stop riding
![](/resources/StopRiding.png)

- In the process of riding, the user presses the pause button, if it is long press, it is to end the movement, if it is short press, it is to pause.
- If the user chooses to end the movement and the movement time is less than 3 minutes, the movement is invalid and the corresponding route data needs to be deleted.
- If the exercise time is longer than 3 minutes, it is effective exercise and needs to be completed normally. The specific process is explained later.
- Deleting the route is also two processes, the first is to delete the data in MongoDB, and the second is to delete the entity in Baidu map hawk-eye service.

## Report geographical location

- In the process of cycling, the geographical location of the user is reported every 15 seconds, and multiple points are moved to form a movement track.
- It should be noted that during the movement, the location is only reported to Baidu map, and MongoDB does not store it. After the movement is over, all the trajectory points in the route are queried and stored in MongoDB.
- When the motion location is reported, the user's own geographic location data needs to be updated to MongoDB.

## Query route
~~~java
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
                             @RequestParam(value = "latitude", required = false) Double latitude)
~~~

## Submission route
~~~java
    /**
     * Submission route
     *
     * @param routeId
     * @return
     */
    @PutMapping("share/{routeId}")
    public Object shareRoute(@PathVariable("routeId") String routeId)
~~~

## A nearby route/a nearby person
~~~java
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
                                 @RequestParam(value = "distance", defaultValue = "10") Double distance)
~~~

## Ride along the route
~~~java
    /**
     * Start moving along the route
     * @param routeId   Destination route id
     * @return          id of a newly created route
     */
    @PostMapping("{routeId}")
    public Object runFromRoute(@PathVariable("routeId")String routeId)
~~~

## Check for people who are riding
~~~java
    /**
     * Route fellow
     *
     * @param routeId
     * @return
     */
    @GetMapping("nearUser/{routeId}")
    public Object queryRouteNearUser(@PathVariable("routeId") String routeId)
~~~

## My historical route
~~~java
    /**
     * Historical route
     *
     * @param userId
     * @return
     */
    @GetMapping("history")
    public Object queryHistoryRoute(@RequestParam(value = "userId", required = false) Long userId,
                                    @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                    @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize)
~~~

~~~java
    /**
     * Historical routes (shown by date)
     *
     * @return
     */
    @GetMapping("history/date")
    public Object queryHistoryRouteByDate(@RequestParam(value = "userId", required = false) Long userId,
                                          @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                          @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize)
~~~