package top.tobiaslee.coolweather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import top.tobiaslee.coolweather.gson.Forecast;
import top.tobiaslee.coolweather.gson.Weather;
import top.tobiaslee.coolweather.service.AutoUpdateService;
import top.tobiaslee.coolweather.util.HttpUtil;
import top.tobiaslee.coolweather.util.Utility;

public class WeatherActivity extends AppCompatActivity {
    public static final String KEY = "6f88423c8e7748498506b85c921c1f03";
    public static final String ADDRESS = "http://guolin.tech/api/weather?cityid=";
    private ScrollView weatherLayout;
    private ImageView bingPicImg;

    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;

    private LinearLayout forecastLayout;

    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;

    public SwipeRefreshLayout swipeRefresh;
    public DrawerLayout drawerLayout;
    private Button navButton;

    private static final String TAG = "RaleeWeather";
    private static final String CELSIUS = "\u2103";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);

        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);

        aqiText = (TextView) findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        sportText = (TextView) findViewById(R.id.sport_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);

        bingPicImg = (ImageView) findViewById(R.id.bing_pic_img);
        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);

        navButton = (Button) findViewById(R.id.nav_button);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navButton.setOnClickListener((v)->{
            drawerLayout.openDrawer(Gravity.START);
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String bingPic = prefs.getString("bing_pic", null);
        if(bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }

        final String weatherId;
        String weatherString = prefs.getString("weather", null);
        if(weatherString != null ) {
             // 利用缓存
            Weather weather = Utility.handleWeatherResponse(weatherString);
            weatherId = weather.basic.weatherId;
            Log.d("weather id",  weather.basic.weatherId);
            showWeatherInfo(weather);
            Log.d(TAG, weatherId);

        } else {
            weatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
            Log.d(TAG, weatherId);

        }

        swipeRefresh.setOnRefreshListener(()->{
            String newId = prefs.getString("weather_id", null);
            // 更新后如果没有选择过城市 weather_id是null 就会报错
            if(newId != null) {
                if(newId != weatherId){
                    // 解决下拉刷新回到缓存的bug
                    requestWeather(newId);
                    Log.d(TAG, "new: " + newId);
                } else {
                    requestWeather(weatherId);
                }
            } else {
                requestWeather(weatherId);
            }

        });
    }

    private void loadBingPic() {
        Log.d("LoadPic", "loadBingPic: ");
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this)
                        .edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
                runOnUiThread(()->{
                    Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                });
            }
        });
    }

    public void requestWeather(String weatherId) {
        String weatherUrl = ADDRESS + weatherId + "&key=" + KEY;
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(()-> {
                    Toast.makeText(WeatherActivity.this, "获取天气失败", Toast.LENGTH_SHORT).show();
                    swipeRefresh.setRefreshing(false);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(()->{
                    if(weather != null) {
                        // 缓存数据
                        SharedPreferences.Editor editor = PreferenceManager.
                                getDefaultSharedPreferences(WeatherActivity.this).edit();
                        editor.putString("weather", responseText);
                        Log.d(TAG, "weather_id" + weather.basic.weatherId);
                        editor.putString("weather_id", weather.basic.weatherId);
                        editor.apply();
                        showWeatherInfo(weather);
                    } else {
                        Toast.makeText(WeatherActivity.this, "获取天气失败", Toast.LENGTH_SHORT).show();
                    }
                    swipeRefresh.setRefreshing(false);
                });
            }
        });
        loadBingPic();
    }


    /**
     *  展示天气类的内容
     * @param weather
     */
    private void showWeatherInfo(Weather weather) {
        if(weather != null && "ok".equals(weather.status)) {
            Intent intent = new Intent(this, AutoUpdateService.class);
            startService(intent);
            String cityName = weather.basic.cityName;
            String updateTime = weather.basic.update.updateTime.split(" ")[1];
            String degree = weather.now.temperature + " " +CELSIUS;
            String weatherInfo = weather.now.more.info;

            titleCity.setText(cityName);
            titleUpdateTime.setText(updateTime);
            degreeText.setText(degree);
            weatherInfoText.setText(weatherInfo);

            forecastLayout.removeAllViews();
            for (Forecast forecast:
                    weather.forecastList) {
                Log.d("Forecast", "showWeatherInfo: ");
                View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,
                        forecastLayout, false);
                TextView dateText = (TextView) view.findViewById(R.id.date_text);
                TextView infoText = (TextView) view.findViewById(R.id.info_text);
                TextView maxText = (TextView) view.findViewById(R.id.max_text);
                TextView minText = (TextView) view.findViewById(R.id.min_text);

                dateText.setText(forecast.date);
                infoText.setText(forecast.more.info);
                String max = forecast.temperature.max + " " + CELSIUS;
                String min = forecast.temperature.min + " " + CELSIUS;
                maxText.setText(max);
                minText.setText(min);
                forecastLayout.addView(view);
            }
            if(weather.aqi != null ) {
                aqiText.setTextSize(40);
                pm25Text.setTextSize(40);
                aqiText.setText(weather.aqi.city.aqi);
                pm25Text.setText(weather.aqi.city.pm25);
            } else {
                aqiText.setTextSize(20);
                pm25Text.setTextSize(20);
                aqiText.setText("暂未获取");
                pm25Text.setText("暂未获取");

                Log.d(TAG, "showWeatherInfo:  no aqi");
            }
            String comfort = "舒适度: " + weather.suggestion.comfort.info;
            String carWash = "洗车指数: " + weather.suggestion.carWash.info;
            String sport = "运动建议: " + weather.suggestion.sport.info;

            comfortText.setText(comfort);
            carWashText.setText(carWash);
            sportText.setText(sport);
            weatherLayout.setVisibility(View.VISIBLE);

        } else {
            Toast.makeText(this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
        }
    }

}
