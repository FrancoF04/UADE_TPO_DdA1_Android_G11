package com.example.androidapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.activity.EdgeToEdge;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String HARDCODED_JSON = "{"
            + "\"error\": false,"
            + "\"msg\": \"all countries and population 1961 - 2018\","
            + "\"data\": ["
            + "  { \"country\": \"Argentina\", \"code\": \"ARG\", \"iso3\": \"ARG\","
            + "    \"populationCounts\": ["
            + "      { \"year\": 2016, \"value\": 43590368 },"
            + "      { \"year\": 2017, \"value\": 44044811 },"
            + "      { \"year\": 2018, \"value\": 44494502 }"
            + "    ]"
            + "  },"
            + "  { \"country\": \"Brazil\", \"code\": \"BRA\", \"iso3\": \"BRA\","
            + "    \"populationCounts\": ["
            + "      { \"year\": 2016, \"value\": 207652865 },"
            + "      { \"year\": 2017, \"value\": 209288278 },"
            + "      { \"year\": 2018, \"value\": 209469333 }"
            + "    ]"
            + "  },"
            + "  { \"country\": \"Colombia\", \"code\": \"COL\", \"iso3\": \"COL\","
            + "    \"populationCounts\": ["
            + "      { \"year\": 2016, \"value\": 48228704 },"
            + "      { \"year\": 2017, \"value\": 49065615 },"
            + "      { \"year\": 2018, \"value\": 49661048 }"
            + "    ]"
            + "  },"
            + "  { \"country\": \"Mexico\", \"code\": \"MEX\", \"iso3\": \"MEX\","
            + "    \"populationCounts\": ["
            + "      { \"year\": 2016, \"value\": 127540423 },"
            + "      { \"year\": 2017, \"value\": 129163276 },"
            + "      { \"year\": 2018, \"value\": 130759074 }"
            + "    ]"
            + "  },"
            + "  { \"country\": \"Peru\", \"code\": \"PER\", \"iso3\": \"PER\","
            + "    \"populationCounts\": ["
            + "      { \"year\": 2016, \"value\": 31488625 },"
            + "      { \"year\": 2017, \"value\": 32165485 },"
            + "      { \"year\": 2018, \"value\": 32551815 }"
            + "    ]"
            + "  }"
            + "]"
            + "}";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ListView listView = findViewById(R.id.listViewCountries);
        Button btnHolaMundo = findViewById(R.id.btnHolaMundo);

        listView.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                parseCountries()));

        btnHolaMundo.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Mensaje")
                .setMessage("Hola Mundo")
                .setPositiveButton("OK", null)
                .show());
    }

    private List<String> parseCountries() {
        List<String> items = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(HARDCODED_JSON);
            JSONArray data = root.getJSONArray("data");
            for (int i = 0; i < data.length(); i++) {
                JSONObject entry = data.getJSONObject(i);
                String country = entry.getString("country");
                JSONArray counts = entry.getJSONArray("populationCounts");
                JSONObject latest = counts.getJSONObject(counts.length() - 1);
                int year = latest.getInt("year");
                long population = latest.getLong("value");
                items.add(country + " (" + year + "): " + String.format("%,d", population));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

}
