# Baidu map Yingyan track service
- Yingyan is a track management service that provides SDK and API for developers to easily access and track vehicles/people and other moving objects under management.
- Based on the interface and cloud services provided by Yingyan, developers can quickly build a complete, accurate and high-performance trajectory management system that is entirely your own, which can be applied to fleet management, personnel management and other fields. Documents: https://lbsyun.baidu.com/index.php?title=yingyan
- Basic functions:
    - Line tracing
    - Track storage
    - Line query
    - Line correction and mileage calculation
    - Spatial retrieval
    - Geofencing
    - Line analysis
    - ……

## Yingyan track service basic concept
- service/entity/track/fence
    - https://lbsyun.baidu.com/index.php?title=yingyan/guide/concept

## Create a Yingyan service
- To use the Yingyan Track service, you first need to create a service to store, access, and manage your own batch of terminals and tracks.
- service management system：https://lbsyun.baidu.com/trace/admin/service 
- Each service can manage up to 1 million terminals (people, cars, etc.), and a developer can create up to 10 services.
- If developers have more than 1 million terminals, they can create multiple services to manage them separately.

## Get service id
- After the service is successfully created, the new service is displayed in the Services I Created list.
- System ID (service_id) on the left of the list, for example, 128658, is the unique identifier for accessing the service and must be used in subsequent interface calls.

## Terminal management
- The main implementation of the terminal management interface is to create, update, delete, and query entity.
- For example: adding a cycling route, deleting a cycling route, updating the attribute information of the cycling route (such as: the name of the cycling route), etc.
- documents： https://lbsyun.baidu.com/index.php?title=yingyan/api/v3/entity
- entity Management class interfaces create, update, delete, and query entities. Includes four interfaces:

## Add/Update Entity

```java
    @Test
    public void testEntityAdd(){
        String url = "https://yingyan.baidu.com/api/v3/track/add(update)";
//        Create entity
        String body = HttpRequest.post(url)
                .form("ak", 'your ak')
                .form("service_id", "Unique identifier of the servie") 
                .form("entity_name", "The name of the entity as its unique identifier")    
                .form("entity_desc", "entity readability description")
                .form("routeName", "Custom field, route name")
                .execute().body();

        System.out.println(body);
    }
```

## Delete Entity

```java
    @Test
    public void testEntityDelete(){
        String url = "https://yingyan.baidu.com/api/v3/entity/delete";
//        Create entity
        String body = HttpRequest.post(url)
                .form("ak", ak)
                .form("service_id", 238239)
                .form("entity_name", "route_1_1001")
                .execute().body();

        System.out.println(body);
    }
```

## Query Entity

```java
    @Test
    public void testEntityList(){
        String url = "https://yingyan.baidu.com/api/v3/entity/list";
//        Create entity
        String body = HttpRequest.get(url)
                .form("ak", ak)
                .form("service_id", 238239)
                .form("filter", "entity_names:route_1_1002")
                .form("coord_type_input", "bd09ll")
                .execute().body();

        System.out.println(body);
    }
```

## Line Upload

```java
    @Test
    public void testEntityAdd(){
        String url = "https://yingyan.baidu.com/api/v3/track/addpoint";

        List<Object> pointList = new ArrayList();
        pointList.add(MapUtil.builder().put("entity_name", "route_1_1002")
                .put("latitude", "latitude 1")
                .put("longitude", "longitude 1")
                .put("loc_time", "System.currentTimeMillis() / 1000")
                ......
                .put("latitude", "latitude n")
                .put("longitude", "longitude n")
                .put("loc_time", "System.currentTimeMillis() / 1000").build());

//        Create entity
        String body = HttpRequest.post(url)
                .form("ak", 'your ak')
                .form("service_id", 238239)
                .form("point_list", JSONUtil.toJsonStr(pointList))
                .execute().body();
        System.out.println(body);
    }
```

## Query track
- You can query track points in a route based on the entity name.
- Baidu map query route trajectory information is to support trajectory correction.
    - There are more or less certain errors in various positioning methods. When the GPS signal is not good (for example, in dense buildings, under viaducts, tunnels, etc.), WI-FI or base station positioning may be used, especially when the surrounding WI-FI hotspots are relatively few, base station positioning will be used, and the positioning error will be increased, resulting in the phenomenon of trajectory drift.
    - To correct track drift and improve track and mileage accuracy, Hawkeye provides high-performance track correction in the track query service.
- Query real-time location, track mileage within a period of time, and track points within a period of time.
- documents：https://lbsyun.baidu.com/index.php?title=yingyan/api/v3/trackprocess

```java
    @Test
    public void testGetTrack(){
        String url = "https://yingyan.baidu.com/api/v3/track/gettrack";
        Long startTime = DateUtil.parse("2021-07-05 00:00:00").getTime() / 1000;
        Long endTime = DateUtil.parse("2021-07-05 23:59:59").getTime() / 1000;
//        Create entity
        String body = HttpRequest.get(url)
                .form("ak", 'your ak')
                .form("service_id", "Unique identifier of the server") 
                .form("entity_name", "The name of the entity as its unique identifier")    
                .form("start_time", startTime)
                .form("end_time", endTime)
                .execute().body();

        System.out.println(body);
    }
```