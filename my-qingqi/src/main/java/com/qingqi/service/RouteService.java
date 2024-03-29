package com.qingqi.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.StrUtil;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.qingqi.pojo.Route;
import com.qingqi.pojo.RunRoute;
import com.qingqi.pojo.User;
import com.qingqi.utils.TimeUtils;
import com.qingqi.utils.UserThreadLocal;
import com.qingqi.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RouteService {
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private BaiduService baiduService;
    @Autowired
    private RouteInfoService routeInfoService;
    @Autowired
    private UserService userService;
    public String createRoute() {
        Route route = new Route();
        route.setId(ObjectId.get());
        route.setUserId(UserThreadLocal.get());
        route.setStatus(1);
        route.setIsShare(false);
        route.setStartTime(System.currentTimeMillis());
        this.mongoTemplate.save(route);
        String routeId = route.getId().toString();
        boolean bool = this.baiduService.createEntity(routeId);
        if (bool) {
            return routeId;
        }
        return null;
    }

    /**
     * 删除路线
     *
     * @param routeId
     * @return
     */
    public Boolean deleteRoute(String routeId) {
        Query query = this.createQuery(routeId);
        boolean result = this.mongoTemplate.remove(query, Route.class).getDeletedCount() == 1;
        if (result) {
//            删除百度鹰眼中的实体
            if (this.baiduService.deleteEntity(routeId)) {
//                删除沿着路线运动的关系数据
//                Update runRouteUpdate = Update.update("status", 0).set("update", System.currentTimeMillis());
                Query runRouteQuery = Query.query(Criteria.where("routeId").is(routeId)
                        .and("userId").is(UserThreadLocal.get()));

                DeleteResult deleteResult = this.mongoTemplate.remove(runRouteQuery, RunRoute.class);
                return deleteResult.getDeletedCount() == 1;
            }
        }
        return false;
    }

    /**
     * 构造查询对象，设置了两个条件，其中userId是确保只删除自己的数据
     * @param routeId
     * @return
     */
    private Query createQuery(String routeId){
        return Query.query(Criteria.where("id").is(new ObjectId(routeId))
                .and("userId").is(UserThreadLocal.get()));
    }

    /**
     * 更新路线（结束运动）
     *
     * @param routeId   路线ID
     * @param title     路线标题
     * @return
     */
    public Object updateRoute(String routeId, String title) {
//        判断路线是否已经结束，如果已经结束就不能再次结束
        Route route = this.queryRouteById(routeId);
        if (null == route) {
            return ErrorResult.builder()
                    .errCode("500").errMessage("结束运动失败，路线不存在。").build();
        }

        if (route.getStatus() == 0) {
            return ErrorResult.builder()
                    .errCode("501").errMessage("结束运动失败，该路线已经结束。").build();
        }

//        更新路线数据
        Update update = Update.update("title", title)
                .set("status", 0 )
                .set("endTime", System.currentTimeMillis());

        UpdateResult updateResult = this.mongoTemplate.updateFirst(this.createQuery(routeId), update, Route.class);
        if (updateResult.getModifiedCount() == 1) {
//            查询百度地图鹰眼服务中的路线轨迹点，更新到路线数据中，异步操作
            this.routeInfoService.updateRouteInfo(routeId, UserThreadLocal.get());

//            结束沿着路线骑行的关系
            Update runRouteUpdate = Update.update("status", 0).set("update", System.currentTimeMillis());
            Query runRouteQuery = Query.query(Criteria.where("routeId").is(routeId)
                    .and("userId").is(UserThreadLocal.get())
                    .and("status").is(1));
            UpdateResult runRouteUpdateResult = this.mongoTemplate.updateFirst(runRouteQuery, runRouteUpdate, RunRoute.class);
//            更新成功
            return runRouteUpdateResult.getModifiedCount() == 1;
        }
        return ErrorResult.builder()
                .errCode("502").errMessage("结束运动失败。").build();
    }

    /**
     * 根据主键查询路线对象
     *
     * @param routeId
     * @return
     */
    public Route queryRouteById(String routeId){
        return this.mongoTemplate.findById(new ObjectId(routeId), Route.class);
    }

    /**
     * Submission route：Modify isShare to true
     *
     * @param routeId
     * @return
     */
    public Boolean shareRoute(String routeId) {
        Update update = Update.update("isShare", true);
        Query query = this.createQuery(routeId);
        UpdateResult updateResult = this.mongoTemplate.updateFirst(query, update, Route.class);
        return updateResult.getModifiedCount() == 1;
    }

    /**
     * Find nearby routes
     *
     * @param longitude Longitude of the current user's location
     * @param latitude  Latitude of the current user location
     * @param distance  The unit is km. The default value is 10km
     * @return
     */
    public Object queryNearRoute(Double longitude, Double latitude, Double distance) {
//        Construct query, nearby search data, condition: The route has ended and has been shared
        NearQuery nearQuery = NearQuery.near(longitude, latitude, Metrics.KILOMETERS)
                .maxDistance(distance)
                .query(Query.query(Criteria.where("isShare").is(true).and("status").is(0)));
        GeoResults<Route> geoResults = this.mongoTemplate.geoNear(nearQuery, Route.class);
        if (CollUtil.isEmpty(geoResults.getContent())){
//            no data
            return Collections.emptyList();
        }
        return geoResults.getContent().stream().map(result -> {
            Route route = result.getContent();
//            data copy
            NearRouteVo nearRouteVo = BeanUtil.toBeanIgnoreError(route, NearRouteVo.class);
            nearRouteVo.setLongitude(route.getLocation().getX());
            nearRouteVo.setLatitude(route.getLocation().getY());
            nearRouteVo.setRange(NumberUtil.round(result.getDistance().getValue(), 2).doubleValue());   //路线与我的距离
            return nearRouteVo;
        }).collect(Collectors.toList());
    }

    /**
     *  根据时间范围查询运动次数
     * @param userId    用户id
     * @param miniTime  时间范围的最小值
     * @param maxTime   时间范围的最大值
     * @return          数量
     */
    public Integer queryRouteCountByDate(Long userId, long miniTime, long maxTime) {
        Query query = Query.query(Criteria.where("userId").is(userId)
                .and("status").is(0)
                .and("endTime").gte(miniTime)
                .andOperator(Criteria.where("endTime").lte(maxTime)));
        return Convert.toInt(this.mongoTemplate.count(query, Route.class));
    }

    /**
     * Start moving along the route
     * @param routeId   Destination route id
     * @return          id of a newly created route
     */
    public String runFromRoute(String routeId) {
//        Create your own route queryNearRoute
        String myRouteId = this.createRoute();
        if (null == myRouteId) {
            return null;
        }

        RunRoute runRoute = new RunRoute();
        runRoute.setRunRouteId(routeId);
        runRoute.setRouteId(routeId);
        runRoute.setCreated(System.currentTimeMillis());
        runRoute.setUpdated(runRoute.getCreated());
        runRoute.setStatus(1);
        runRoute.setUserId(UserThreadLocal.get());
        runRoute.setId(ObjectId.get());
        this.mongoTemplate.save(runRoute);
        return myRouteId;
    }

    /**
     * 路线同行的人
     * @param routeId
     * @return
     */
    public Object queryRouteNearUser(String routeId) {
        PageRequest pageRequest = PageRequest.of(0, 9, Sort.by(Sort.Order.desc("created")));
        Query query = Query.query(Criteria.where("runRouteId").is(routeId)
                .and("status").is(1)).with(pageRequest);
        List<RunRoute> runRoutes = this.mongoTemplate.find(query, RunRoute.class);
        if (CollUtil.isEmpty(runRoutes)) {
//            没有查到数据
            return MapUtil.builder().put("count", 0)
                    .put("records", Collections.EMPTY_LIST).build();
        }

//        查询用户数据
        List<Object> userIdList = CollUtil.getFieldValues(runRoutes, "userId");
        Map<Long, User> userMap = this.userService.queryUserMap(userIdList);
//        构造响应结果
        List<Map<Object, Object>> resultList = runRoutes.stream().map(runRoute -> {
            User user = userMap.get(runRoute.getUserId());
            return MapUtil.builder()
                    .put("userId", runRoute.getUserId())
                    .put("logo", user.getLogo())
                    .put("nickName", user.getNickName()).build();
        }).collect(Collectors.toList());
//        查询数量
        Query countQuery = Query.query(Criteria.where("runRouteId").is(routeId)
                .and("status").is(1));
        long count = this.mongoTemplate.count(countQuery, RunRoute.class);
        return MapUtil.builder().put("count", count)
                .put("records", resultList).build();
    }

    /**
     * 查询用户的历史路线
     *
     * @param userId
     * @return
     */
    public Object queryHistoryRoute(Long userId, Integer pageNum, Integer pageSize) {
        Criteria criteria;
        if (userId != null) {
            // 查询别人的数据，只查询已投稿的路线
            criteria = Criteria.where("userId").is(userId).and("status").is(0)
                    .and("isShare").is(true);
        } else {
            //查询自己的数据
            criteria = Criteria.where("userId").is(UserThreadLocal.get())
                    .and("status").is(0);
        }

        PageRequest pageRequest = PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Order.desc("startTime")));

        Query query = Query.query(criteria).with(pageRequest);

        List<Route> routeList = this.mongoTemplate.find(query, Route.class);
        PageResult pageResult = new PageResult();
        pageResult.setPageNum(pageNum);
        pageResult.setPageSize(pageSize);
        pageResult.setTotal(this.mongoTemplate.count(Query.query(criteria), Route.class));
        pageResult.setRecords(routeList.stream().map(
                route -> {
                    RouteVo routeVo = BeanUtil.toBeanIgnoreError(route, RouteVo.class);
                    try {
                        routeVo.setTimeValue(route.getEndPoint().getLocTime() - route.getStartPoint().getLocTime());
                    } catch (Exception e) {
                        routeVo.setTimeValue(0L);
                    }
                    return routeVo;
                }
        ).collect(Collectors.toList()));
        return pageResult;
    }

    /**
     * 查询历史路线(日期分组)
     *
     * @param pageNum
     * @param pageSize
     * @return
     */
    public Object queryHistoryRouteByDate(Long userId, Integer pageNum, Integer pageSize) {
        //查询自己的历史路线
        Criteria criteria;
        if (userId != null) {
            // 查询别人的数据，只查询已投稿的路线
            criteria = Criteria.where("userId").is(userId).and("status").is(0)
                    .and("isShare").is(true);
        } else {
            //查询自己的数据
            criteria = Criteria.where("userId").is(UserThreadLocal.get())
                    .and("status").is(0);
        }

        Query query = Query.query(criteria).with(Sort.by(Sort.Order.desc("startTime")));
        //查询出所有的路线
        List<Route> routeList = this.mongoTemplate.find(query, Route.class);

        //定义响应结果结构，按照日期显示
        List<HistoryRouteVo> result = new ArrayList<>();
        //下面对于历史列表进行处理，按照日期显示
        for (Route route : routeList) {
            String dateFormat = DateUtil.format(new Date(route.getEndTime()), "MM月dd日");
            String yearFormat = DateUtil.format(new Date(route.getEndTime()), "yyyy年");

            HistoryRouteVo historyRouteVo = null;
            for (HistoryRouteVo vo : result) {
                if (StrUtil.equals(vo.getDate(), dateFormat) && StrUtil.equals(vo.getYear(), yearFormat)) {
                    historyRouteVo = vo;
                    break;
                }
            }

            if (null == historyRouteVo) {
                historyRouteVo = new HistoryRouteVo();
                historyRouteVo.setDate(dateFormat);
                historyRouteVo.setYear(yearFormat);
                result.add(historyRouteVo);
            }

            RouteVo routeVo = BeanUtil.toBeanIgnoreError(route, RouteVo.class);
            try {
                routeVo.setTimeValue(route.getEndPoint().getLocTime() - route.getStartPoint().getLocTime());
            } catch (Exception e) {
                routeVo.setTimeValue(0L);
            }
            historyRouteVo.getRouteList().add(routeVo);
        }

        //分页返回数据
        int[] startEnd = PageUtil.transToStartEnd(pageNum - 1, pageSize);
        int start = Math.max(startEnd[0], 0);
        int end = Math.min(startEnd[1], result.size());

        //最终返回的数据
        List<HistoryRouteVo> subResult = ListUtil.sub(result, start, end);

        //计算运动总时间
        for (HistoryRouteVo historyRouteVo : subResult) {
            Long totalTime = historyRouteVo.getRouteList().stream()
                    .mapToLong(value -> value.getTimeValue())
                    .sum();
            historyRouteVo.setTotalTime(TimeUtils.formatTime(totalTime));
        }

        PageResult pageResult = new PageResult();
        pageResult.setPageNum(pageNum);
        pageResult.setPageSize(pageSize);
        pageResult.setTotal(Convert.toLong(result.size()));
        pageResult.setRecords(subResult);
        return pageResult;
    }

    /**
     * Query routes by time range (including time boundaries)
     *
     * @param userId
     * @param minDate
     * @param maxDate
     * @return
     */
    public List<Route> queryRouteListByDate(Long userId, Long minDate, Long maxDate) {
        Query query = Query.query(Criteria.where("endTime").gte(minDate)
                .and("status").is(0)
                .and("userId").is(userId)
                .andOperator(Criteria.where("endTime").lte(maxDate)));
        return this.mongoTemplate.find(query, Route.class);
    }
}
