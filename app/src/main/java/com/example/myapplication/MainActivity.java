package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvUserInfo;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Protección de ruta: valida si hay token
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String token = prefs.getString("auth_token", null);

        if (token == null || token.isEmpty()) {
            redirigirALogin();
            return;
        }

        setContentView(R.layout.activity_main);

        tvUserInfo = findViewById(R.id.tvUserInfo);
        btnLogout = findViewById(R.id.btnLogout);

        String rol = prefs.getString("user_role", "Sin Rol");
        int userId = prefs.getInt("user_id", 0);

        tvUserInfo.setText("ID Usuario: " + userId + "\nRol asignado: " + rol);

        btnLogout.setOnClickListener(v -> cerrarSesion());
    }

    private void cerrarSesion() {
        // Criterio 3: Limpieza profunda de memoria
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();

        // Criterios 1 y 2: Matar pila y volver al login
        redirigirALogin();
    }

    private void redirigirALogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}