package com.contlog.fotosescalasoft;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText usuario;
    private EditText password;
    private String basicEncode;
    private TextView errorLogin;
    private ProgressBar progressBar;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button enviar = findViewById(R.id.buttonLogar);
        progressBar = (ProgressBar)findViewById(R.id.progressBarLogin);
        usuario = findViewById(R.id.editTextTextPersonName);
        password = findViewById(R.id.editTextTextPassword);

        errorLogin = findViewById(R.id.errorLogin);

        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.CAMERA
            },100);
        }

        enviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (usuario.getText().length() == 0) {
                    Toast.makeText(
                            view.getContext(),
                            "Campo Usuário não pode ser vazio",
                            Toast.LENGTH_SHORT)
                            .show();
                    return;
                }
                if (password.getText().length() == 0) {
                    Toast.makeText(
                            view.getContext(),
                            "Campo Senha não pode ser vazio",
                            Toast.LENGTH_SHORT)
                            .show();
                    return;
                }
                progressBar.setVisibility(View.VISIBLE);
                String encode = usuario.getText() + ":" + password.getText();
                basicEncode = Base64.getEncoder().encodeToString(encode.getBytes());
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        StringBuffer buffer = logar();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String token = extrairToken(buffer);
                                if (token == null) {
                                    errorLogin.setVisibility(View.VISIBLE);
                                    errorLogin.setText(R.string.ErrorLogin);
                                    progressBar.setVisibility(View.INVISIBLE);
                                    return;
                                }
                                Intent intent = new Intent(getApplicationContext(), MainTask.class);
                                intent.putExtra("token", token);
                                startActivity(intent);
                                finish();
                            }
                        });

                    }
                });
            }
        });
    }

    private String extrairToken(StringBuffer buffer) {
        String response = buffer.toString();
        int initToken = response.indexOf("token");
        if (initToken == -1) {
            return null;
        }
        String iniTokenString = response.substring(initToken);
        String token = iniTokenString.substring(8, iniTokenString.lastIndexOf("\""));
        return token;
    }

    private StringBuffer logar() {
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        StringBuffer buffer = new StringBuffer();

        try {
            URL url = new URL("http://192.168.0.228:42510/authorization");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Basic " + basicEncode);

            if (connection.getResponseCode() == 200) {
                inputStream = connection.getInputStream();
                inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String linha = "";
                while ((linha = reader.readLine()) != null) {
                    buffer.append(linha);
                }
            }else{
                buffer.append("Erro ao logar no WebService");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }
}