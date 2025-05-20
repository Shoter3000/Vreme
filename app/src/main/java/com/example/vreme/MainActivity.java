package com.example.vreme;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.text.Html;
import android.text.method.LinkMovementMethod;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONArray;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.ArrayList;

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
        feelsLikeText = findViewById(R.id.feelsLikeText);
        pressureText = findViewById(R.id.pressureText);
        sunriseText = findViewById(R.id.sunriseText);
        sunsetText = findViewById(R.id.sunsetText);
        LinearLayout forecastLayout = findViewById(R.id.forecastLayout);


        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Inflate custom layout
                LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                View dialogView = inflater.inflate(R.layout.dialog_city_input, null);

                EditText input = dialogView.findViewById(R.id.inputCity);

                // Uporabi MaterialAlertDialog z želenim slogom
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.CustomAlertDialog);

                builder.setView(dialogView);
                builder.setPositiveButton("Poišči", (dialog, which) -> {
                    String cityName = input.getText().toString().trim();
                    if (!cityName.isEmpty()) {
                        FetchWeatherData(cityName);
                    }
                });

                builder.setNegativeButton("Prekliči", (dialog, which) -> dialog.cancel());

                // Ustvari dialog in nastavi transparentno ozadje
                AlertDialog dialog = builder.create();
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });








        FetchWeatherData("Nova Gorica");
    }

    private void FetchWeatherData(String cityName) {

        String url = "https://api.openweathermap.org/data/2.5/weather?q=" + cityName +
                "&appid=" + API_KEY + "&units=metric&lang=sl";

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String result = response.body().string();
                    runOnUiThread(() -> updateUI(result));
                    fetchForecastData(cityName);
                } else {
                    runOnUiThread(() ->
                            showCustomDialog("Vneseno ime kraja ni pravilno.")
                    );
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        showCustomDialog("Napaka pri povezavi.")
                );
            }
        });
    }

    private void fetchForecastData(String cityName) {
        String url = "https://api.openweathermap.org/data/2.5/forecast?q=" + cityName +
                "&appid=" + API_KEY + "&units=metric&lang=sl";

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String result = response.body().string();
                    runOnUiThread(() -> updateForecastUI(result));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    private void updateForecastUI(String json) {
        try {
            JSONObject forecastObject = new JSONObject(json);
            JSONArray list = forecastObject.getJSONArray("list");

            LinearLayout forecastLayout = findViewById(R.id.forecastLayout);
            forecastLayout.removeAllViews();

            Map<String, List<JSONObject>> dailyForecasts = new LinkedHashMap<>();
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("sl"));
            SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

            for (int i = 0; i < list.length(); i++) {
                JSONObject item = list.getJSONObject(i);
                Date date = inputFormat.parse(item.getString("dt_txt"));
                String day = dayFormat.format(date);

                dailyForecasts.computeIfAbsent(day, k -> new ArrayList<>()).add(item);
            }

            int daysShown = 0;

            for (Map.Entry<String, List<JSONObject>> entry : dailyForecasts.entrySet()) {
                if (daysShown >= 3) break;

                // Inflate kartico za en dan
                View cardView = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
                TextView dayTitle = cardView.findViewById(R.id.dayTitle);
                LinearLayout hourlyContainer = cardView.findViewById(R.id.hourlyContainer);

                dayTitle.setText(entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1));

                for (JSONObject item : entry.getValue()) {
                    JSONObject main = item.getJSONObject("main");
                    double temp = main.getDouble("temp");

                    String description = item.getJSONArray("weather").getJSONObject(0).getString("description");
                    String iconCode = item.getJSONArray("weather").getJSONObject(0).getString("icon");

                    Date date = inputFormat.parse(item.getString("dt_txt"));
                    String time = hourFormat.format(date);
                    int resId = getResources().getIdentifier("_" + iconCode, "drawable", getPackageName());

                    TextView forecastText = new TextView(this);
                    forecastText.setText(time + "  •  " + String.format("%.0f°", temp) + "  •  " + description);
                    forecastText.setTextColor(Color.WHITE);
                    forecastText.setTextSize(15);
                    forecastText.setCompoundDrawablesWithIntrinsicBounds(resId, 0, 0, 0);
                    forecastText.setCompoundDrawablePadding(16);
                    forecastText.setPadding(0, 6, 0, 6);

                    hourlyContainer.addView(forecastText);
                }

                forecastLayout.addView(cardView);
                daysShown++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private void showCustomDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.custom_dialog, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        Button dialogButton = dialogView.findViewById(R.id.dialogButton);

        dialogMessage.setText(message);
        dialogButton.setOnClickListener(v -> dialog.dismiss());

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

}