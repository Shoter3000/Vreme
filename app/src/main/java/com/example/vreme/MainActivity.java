package com.example.vreme;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.text.Html;
import android.text.method.LinkMovementMethod;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


public class MainActivity extends AppCompatActivity {
    private TextView cityNameText, temperatureText, humidityText, descriptionText, windText;
    private ImageView weatherIcon;
    private Button refreshButton;
    private EditText cityNameInput;

    private TextView feelsLikeText, pressureText, sunriseText, sunsetText;
    private static final String API_KEY = "228991c3f9f24ebf2d10c974b16b9b9a";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView sourceTextView = findViewById(R.id.dataSourceText);
        sourceTextView.setText(Html.fromHtml(getString(R.string.openweather_credit), Html.FROM_HTML_MODE_LEGACY));
        sourceTextView.setMovementMethod(LinkMovementMethod.getInstance());


        cityNameText = findViewById(R.id.cityNameText);
        temperatureText = findViewById(R.id.temperatureText);
        humidityText = findViewById(R.id.humidityText);
        windText= findViewById(R.id.windText);
        descriptionText = findViewById(R.id.descriptionText);
        weatherIcon = findViewById(R.id.weatherIcon);
        refreshButton  = findViewById(R.id.fetchWeatherButton);
        cityNameInput = findViewById(R.id.cityNameInput);
        feelsLikeText = findViewById(R.id.feelsLikeText);
        pressureText = findViewById(R.id.pressureText);
        sunriseText = findViewById(R.id.sunriseText);
        sunsetText = findViewById(R.id.sunsetText);



        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String cityName = cityNameInput.getText().toString();
                if(!cityName.isEmpty())
                {
                    FetchWeatherData(cityName);
                }
                else
                {
                    cityNameInput.setError("Vnesi ime kraja");
                }
            }
        });



        FetchWeatherData("Nova Gorica");
    }

    private void FetchWeatherData(String cityName) {
        String url = "https://api.openweathermap.org/data/2.5/weather?q=" + cityName + "&appid=" + API_KEY + "&units=metric&lang=sl";
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();
            try {
                Response response = client.newCall(request).execute();
                String result = response.body().string();

                if (response.isSuccessful()) {
                    runOnUiThread(() -> updateUI(result));
                } else {
                    runOnUiThread(() -> cityNameInput.setError("Vneseno ime kraja ni pravilno."));
                }

            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> cityNameInput.setError("Napaka pri povezavi."));
            }
        });
    }


    private void updateUI(String result)
    {
        if(result != null)
        {
            try {
                JSONObject jsonObject = new JSONObject(result);
                JSONObject main =  jsonObject.getJSONObject("main");
                double temperature = main.getDouble("temp");
                double humidity = main.getDouble("humidity");
                double windSpeed = jsonObject.getJSONObject("wind").getDouble("speed");

                String description = jsonObject.getJSONArray("weather").getJSONObject(0).getString("description");
                String iconCode = jsonObject.getJSONArray("weather").getJSONObject(0).getString("icon");
                String resourceName = "_" + iconCode;
                int resId = getResources().getIdentifier(resourceName, "drawable", getPackageName());
                weatherIcon.setImageResource(resId);

                cityNameText.setText(jsonObject.getString("name"));
                temperatureText.setText(String.format("%.0f°", temperature));
                humidityText.setText(String.format("%.0f%%", humidity));
                windText.setText(String.format("%.0f km/h", windSpeed));
                descriptionText.setText(description);

                double feelsLike = main.getDouble("feels_like");
                double pressure = main.getDouble("pressure");

                long sunrise = jsonObject.getJSONObject("sys").getLong("sunrise");
                long sunset = jsonObject.getJSONObject("sys").getLong("sunset");

                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                sdf.setTimeZone(TimeZone.getDefault());

                String sunriseTime = sdf.format(new Date(sunrise * 1000L));
                String sunsetTime = sdf.format(new Date(sunset * 1000L));

                feelsLikeText.setText(String.format("%.0f°", feelsLike));
                pressureText.setText(String.format("%.0f hPa", pressure));
                sunriseText.setText("Vzhod: " + sunriseTime);
                sunsetText.setText("Zahod: " + sunsetTime);

            } catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
    }
}