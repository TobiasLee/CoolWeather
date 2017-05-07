package top.tobiaslee.coolweather.gson;

/**
 * Created by tobiaslee on 2017/5/7.
 */

public class AQI {
    public AQICity city;

    public class AQICity {
        public String aqi;
        public String pm25;
    }
}
