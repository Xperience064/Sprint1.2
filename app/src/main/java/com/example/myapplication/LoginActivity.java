package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private TextView tvError;
    private Button btnLogin;
    private ProgressBar progressBar;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        tvError = findViewById(R.id.tvError);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> intentarLogin());
    }

    private void intentarLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Escenario 3: Manejo de conectividad de red
        if (!hayConexionRed()) {
            mostrarError("Sin conexión a internet. Verifique su red.");
            return;
        }

        if (username.isEmpty() || password.isEmpty()) {
            mostrarError("Ingrese usuario y contraseña.");
            return;
        }

        tvError.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        // Llamada asíncrona al endpoint /auth/login
        executor.execute(() -> {
            boolean exito = false;
            String token = "";
            String detalleFallo = "";

            try {
                URL url = new URL("https://fakestoreapi.com/auth/login");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("username", username);
                jsonParam.put("password", password);

                byte[] input = jsonParam.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(input, 0, input.length);
                    os.flush();
                }

                int responseCode = conn.getResponseCode();

                if (responseCode == 200 || responseCode == 201) {
                    InputStream is = conn.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    br.close();

                    JSONObject resObj = new JSONObject(response.toString());
                    token = resObj.optString("token", "");
                    exito = !token.isEmpty();
                } else {
                    detalleFallo = "Código HTTP: " + responseCode;
                }
            } catch (Exception e) {
                exito = false;
                detalleFallo = e.getClass().getSimpleName();
            }

            final boolean loginOk = exito;
            final String finalToken = token;
            final String mensajeExtra = detalleFallo;

            handler.post(() -> {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);

                if (loginOk) {
                    guardarSesion(username, finalToken);
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // Escenario 2: Alerta roja
                    if (!mensajeExtra.isEmpty()) {
                        mostrarError("Usuario o contraseña inválidos (" + mensajeExtra + ")");
                    } else {
                        mostrarError("Usuario o contraseña inválidos");
                    }
                }
            });
        });
    }

    private void mostrarError(String mensaje) {
        tvError.setText(mensaje);
        tvError.setVisibility(View.VISIBLE);
    }

    private boolean hayConexionRed() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
    }

    private void guardarSesion(String username, String token) {
        // Reglas de negocio: IDs 1 y 2 = Admin, ID 3 = Auditor, resto = Clientes
        String rol = "Cliente";
        int userId = 4;

        if (username.equalsIgnoreCase("johnd")) {
            userId = 1;
            rol = "Administrador";
        } else if (username.equalsIgnoreCase("mor_2314")) {
            userId = 2;
            rol = "Administrador";
        } else if (username.equalsIgnoreCase("kevinryan")) {
            userId = 3;
            rol = "Auditor";
        }

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit()
                .putString("auth_token", token)
                .putInt("user_id", userId)
                .putString("user_role", rol)
                .apply();
    }
}