package com.ciphervault.app;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        statusText = findViewById(R.id.statusText);

        checkBackendHealth();
    }

    private void checkBackendHealth() {

        statusText.setText(
                "Connecting to CipherVault backend..."
        );

        ApiClient.getApiService(this)
                .checkHealth()
                .enqueue(new Callback<Map<String, Object>>() {

                    @Override
                    public void onResponse(
                            Call<Map<String, Object>> call,
                            Response<Map<String, Object>> response
                    ) {

                        if (response.isSuccessful()
                                && response.body() != null) {

                            Map<String, Object> data =
                                    response.body();

                            String status =
                                    String.valueOf(data.get("status"));

                            String service =
                                    String.valueOf(data.get("service"));

                            statusText.setText(
                                    "✓ Backend Connected\n\n"
                                            + "Status: " + status
                                            + "\n"
                                            + "Service: " + service
                            );

                        } else {

                            statusText.setText(
                                    "✗ Backend Error\n\n"
                                            + "HTTP "
                                            + response.code()
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<Map<String, Object>> call,
                            Throwable t
                    ) {

                        statusText.setText(
                                "✗ Connection Failed\n\n"
                                        + t.getMessage()
                        );
                    }
                });
    }
}