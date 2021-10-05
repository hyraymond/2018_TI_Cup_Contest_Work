package com.nju.map;

import com.nju.map.DigitalMap.*;
import com.nju.map.DigitalMap.Map;

/**
 * Created by YSL on 2017/10/19.
 * 地图分类器，添加地图时需要添加相应代码
 */

public class MapFactory {
    private static Map map;
    public static Map getMap(String data, int screenWidth, int screenHeight) {
        switch (data) {
            case "cn_js_nj_nju_basicLabBing_third_floor_1":
                map = new LabThirdFloorOne(screenWidth,screenHeight);
                break;
            case "cn_js_nj_nju_basicLabBing_third_floor_2":
                map = new LabThirdFloorTwo(screenWidth,screenHeight);
                break;
            case "cn_js_nj_nju_basicLabBing_third_floor_3":
                map = new LabThirdFloorThree(screenWidth,screenHeight);
                break;
            default:
                map = null;
        }
        return map;
    }
}
