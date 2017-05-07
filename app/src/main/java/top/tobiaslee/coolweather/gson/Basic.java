package top.tobiaslee.coolweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by tobiaslee on 2017/5/7.
 */

public class Basic {

    // 用SerializedName来建立JSON字段和Java字段之间的联系
    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String weatherId;

    public Update update;

    public class Update {
        @SerializedName("loc")
        public String updateTime;
    }
}
