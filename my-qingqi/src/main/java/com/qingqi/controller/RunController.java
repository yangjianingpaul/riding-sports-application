
package com.qingqi.controller;

import com.qingqi.service.BaiduService;
import com.qingqi.service.RouteService;
import com.qingqi.service.UserLocationService;
import com.qingqi.utils.UserThreadLocal;
import com.qingqi.vo.ErrorResult;
import com.qingqi.vo.RunParamVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequestMapping("run")
@RestController
public class RunController {
    @Autowired
    private BaiduService baiduService;
    @Autowired
    private RouteService routeService;

    @Autowired
    private UserLocationService userLocationService;

    /**
     * Report your location once every 15 seconds in the app
     * @param routeId
     * @param runParamVo
     * @return
     */
    @PostMapping("{routeId}")
    public Object run(@PathVariable("routeId") String routeId, RunParamVo runParamVo){
        Boolean result = this.baiduService.uploadLocation(routeId, runParamVo);
        if (result) {
            Long userId = UserThreadLocal.get();
            Double longitude = runParamVo.getLongitude();
            Double latitude = runParamVo.getLatitude();
//            Asynchronously update your own location data
            this.userLocationService.uploadLocation(userId, longitude, latitude);
            return null;
        }

        return ErrorResult.builder().errCode("500").errMessage("Description Failed to report the geographical location！").build();
    }

    /**
     * Update route (End of campaign)
     *
     * @param routeId   Route ID
     * @param title     Route title
     * @return
     */
    @PutMapping
    public Object updateRoute(@RequestParam("routeId") String routeId, @RequestParam("title") String title){
        return this.routeService.updateRoute(routeId, title);
    }
}
