package com.contlog.fotosescalasoft;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;

import com.contlog.fotosescalasoft.models.Anexo;
import com.contlog.fotosescalasoft.models.Foto;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import java.net.URL;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainTask extends AppCompatActivity implements View.OnClickListener {

    private EditText ordem;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private CheckBox checkBoxCntrVazio;
    private CheckBox checkBoxPortaLacrada;
    private CheckBox checkBoxPortaAberta;
    private CheckBox checkBoxLacre;
    private CheckBox checkBoxChecklist;
    private CheckBox checkBoxBobina;
    private ProgressBar progressBarHorizontal;
    private ProgressBar spinnerProgress;
    private final List<Foto> list = new ArrayList<>();
    private int IdButton;
    public  String token;
    private Uri image_uri;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_task);

        Bundle dados    = getIntent().getExtras();
        token           = dados.getString("token");

        Button cntrVazio = findViewById(R.id.cntrVazioFoto);
        Button bobinas = findViewById(R.id.bobinas);
        Button cntrPortaAberta = findViewById(R.id.cntrPortaAberta);
        Button checkList = findViewById(R.id.checkList);
        Button lacre = findViewById(R.id.Lacre);
        Button cntrPortaLacrada = findViewById(R.id.cntrPortaLacrada);
        Button buttonSubmit= findViewById(R.id.buttonSubmit);
        progressBarHorizontal = (ProgressBar)findViewById(R.id.progressBar2);
        spinnerProgress = (ProgressBar)findViewById(R.id.spinner);
        ordem = findViewById(R.id.NumeroOrdem);

        checkBoxCntrVazio = findViewById(R.id.checkBoxCntrVazio);
        checkBoxPortaLacrada = findViewById(R.id.checkBoxPortaLacrada);
        checkBoxPortaAberta = findViewById(R.id.checkBoxPortaAberta);
        checkBoxLacre = findViewById(R.id.checkBoxLacre);
        checkBoxChecklist = findViewById(R.id.checkBoxChecklist);
        checkBoxBobina = findViewById(R.id.checkBoxBobinas);

        ordem.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_UP) {
                    spinnerProgress.setVisibility(View.VISIBLE);
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            StringBuffer buffer = procurarOrdem(ordem.getText().toString());
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    atualizarViewFotos(buffer);
                                    spinnerProgress.setVisibility(View.INVISIBLE);
                                }
                            });
                        }
                    });
                }
                return false;
            }
        });

        cntrVazio.setOnClickListener(this);
        bobinas.setOnClickListener(this);
        cntrPortaAberta.setOnClickListener(this);
        checkList.setOnClickListener(this);
        lacre.setOnClickListener(this);
        cntrPortaLacrada.setOnClickListener(this);
        buttonSubmit.setOnClickListener(this);
    }

    private void atualizarViewFotos(StringBuffer buffer) {
        String[] anexos = buffer.toString().replaceAll("\"", "").split(",");
        for (String s : anexos) {
            String anexo = s.replace("[^\\w\\s]+", "");
            atualizarCheckbox(anexo);
        }

    }

    private StringBuffer procurarOrdem(String ordem) {
        progressBarHorizontal.setProgress(5);
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        StringBuffer buffer = new StringBuffer();
        try {
            URL url = new URL("http://192.168.0.228:42510/api/Ordem/anexosdaordem?numeroOrdem=" + ordem);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization","Bearer "+token);
            progressBarHorizontal.setProgress(7);
            if (connection.getResponseCode() == 200) {
                inputStream = connection.getInputStream();
                inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String linha = "";
                while ((linha = reader.readLine()) != null) {
                    buffer.append(linha);
                }
            } else {
                buffer.append("server retornou erro ao consultar a ordem");
            }
            progressBarHorizontal.setProgress(10);
        } catch (IOException e) {
            e.printStackTrace();
        }
        progressBarHorizontal.setProgress(0);
        return buffer;
    }

    @Override
    public void onClick(View view) {
        if (ordem.getText().length() == 0) {
            Toast.makeText(getApplicationContext(), "Ordem não pode vazio", Toast.LENGTH_LONG).show();
            return;
        }
        if(view.getId() == R.id.buttonSubmit){
            spinnerProgress.setVisibility(View.VISIBLE);
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    enviarFotos();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            spinnerProgress.setVisibility(View.INVISIBLE);
                            Toast.makeText(getApplicationContext(),"Fotos enviadas com Sucesso",Toast.LENGTH_LONG).show();
                        }
                    });

                }
            });
            return;
        }
        //abrir camera
        dispatchTakePictureIntent();
        IdButton = view.getId();
    }

    private void enviarFotos() {
        int quantEnviado = 1;
        int maxValue = progressBarHorizontal.getMax();
        double unit =(double) list.size()/maxValue;
        for (Foto foto : list) {
            quantEnviado = quantEnviado + 1;
            int progress = (int)Math.ceil(quantEnviado * unit);
            progressBarHorizontal.setProgress(progress);
            byte[] bt = null;
            try {
                InputStream imageStream = getContentResolver().openInputStream(foto.uri);
                Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                selectedImage.compress(Bitmap.CompressFormat.PNG,80,stream);
                bt = stream.toByteArray();
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String encodeString = Base64.getEncoder().encodeToString(bt);
            Anexo anexo = new Anexo(foto.nome, "Imagem",encodeString);
            Gson gson = new Gson();
            String json = "{\"Lista\":{\"Anexo\":"+gson.toJson(anexo)+"}}";
            try {
                URL url = new URL("http://192.168.0.228:42510/api/Ordem/anexo/cadastro?numeroOrdem="+ordem.getText().toString());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Authorization","Bearer "+token);
                try(OutputStream os = connection.getOutputStream()){
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input,0,input.length);
                }catch (IOException e){
                    e.printStackTrace();
                }

                int statusCode = connection.getResponseCode();
                if(statusCode == 200){
                    System.out.println("Foto Envida");
                }else{
                    String response = connection.getResponseMessage();
                    System.out.println("Erro server: "+response);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
            Intent intent = getIntent();
            finish();
            startActivity(intent);

    }

    //232146
    private void dispatchTakePictureIntent() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE,"New Picure");
                values.put(MediaStore.Images.Media.DESCRIPTION,"From the camera");
                image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
                //intent
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            atualizarFoto(image_uri);
        }
     }

    private void atualizarFoto(Uri img_uri){
        Date date = new Date();
        String nome = String.valueOf(date.getTime());

        if(IdButton == R.id.cntrVazioFoto){
            ImageView img = findViewById(R.id.imageView11);
            img.setImageURI(img_uri);
            CheckBox checkBox = findViewById(R.id.checkBoxCntrVazio);
            checkBox.setChecked(true);
            list.add(new Foto("cntrVazioFoto"+nome+".PNG",
                    img_uri,
                    ordem.getText().toString(),
                    token));
            return;
        }
        if(IdButton == R.id.cntrPortaLacrada){
            ImageView img5 = findViewById(R.id.imageView13);
            img5.setImageURI(img_uri);
            CheckBox checkBox = findViewById(R.id.checkBoxPortaLacrada);
            checkBox.setChecked(true);
            list.add(new Foto("cntrPortaLacrada"+nome+".PNG",
                    img_uri,
                    ordem.getText().toString(),
                    token));
            return;
        }
        if(IdButton == R.id.Lacre){
            ImageView img4 = findViewById(R.id.imageView12);
            img4.setImageURI(img_uri);
            CheckBox checkBox = findViewById(R.id.checkBoxLacre);
            checkBox.setChecked(true);
            list.add(new Foto("Lacre"+nome+".PNG",
                    img_uri,
                    ordem.getText().toString(),
                    token));
            return;
        }
        if(IdButton == R.id.checkList){
            ImageView img3 = findViewById(R.id.imageView8);
            img3.setImageURI(img_uri);
            CheckBox checkBox = findViewById(R.id.checkBoxChecklist);
            checkBox.setChecked(true);
            list.add(new Foto("checkList"+nome+".PNG",
                    img_uri,
                    ordem.getText().toString(),
                    token));
            return;
        }
        if(IdButton == R.id.cntrPortaAberta){
            ImageView img2 = findViewById(R.id.imageView7);
            img2.setImageURI(img_uri);
            CheckBox checkBox = findViewById(R.id.checkBoxPortaAberta);
            checkBox.setChecked(true);
            list.add(new Foto("cntrPortaAberta"+nome+".PNG",
                    img_uri,
                    ordem.getText().toString(),
                    token));
            return;
        }
        if(IdButton == R.id.bobinas){
            ImageView img1 = findViewById(R.id.imageView5);
            img1.setImageURI(img_uri);
            CheckBox checkBox = findViewById(R.id.checkBoxBobinas);
            checkBox.setChecked(true);
            list.add(new Foto("bobinas"+nome+".PNG",
                    img_uri,
                    ordem.getText().toString(),
                    token));
        }
    }

    private void atualizarCheckbox(String anexo) {
        if(anexo.contains("erro")){
            new AlertDialog.Builder(MainTask.this)
                    .setTitle("Information")
                    .setMessage("Ordem não existe")
                    .show();
        }
        if (anexo.contains("cntrVazioFoto")) {
            checkBoxCntrVazio.setChecked(true);
            return;
        }
        if (anexo.contains("bobinas")) {
            checkBoxBobina.setChecked(true);
            return;
        }
        if (anexo.contains("cntrPortaAberta")) {
            checkBoxPortaAberta.setChecked(true);
            return;
        }
        if (anexo.contains("Lacre")) {
            checkBoxLacre.setChecked(true);
            return;
        }
        if (anexo.contains("checkList")) {
            checkBoxChecklist.setChecked(true);
            return;
        }
        if (anexo.contains("cntrPortaLacrada")) {
            checkBoxPortaLacrada.setChecked(true);
        }
    }
}