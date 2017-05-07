package top.tobiaslee.coolweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by tobiaslee on 2017/5/7.
 */

public class Now {

    @SerializedName("tmp")
    public String temperature;

    @SerializedName("cond")
    public More more;

    public class More {
        @SerializedName("txt")
        public String info;
    }
}
