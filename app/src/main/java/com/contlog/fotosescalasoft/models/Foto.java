package com.contlog.fotosescalasoft.models;

import android.graphics.Bitmap;
import android.net.Uri;

import com.contlog.fotosescalasoft.R;

public class Foto {
    public String ordem;
    public String nome;
    public Uri uri;
    public String tokenAuthorization;

    public Foto(String nome, Uri uri,String ordem,String tokenAuthorization) {
        this.nome = nome;
        this.uri = uri;
        this.ordem = ordem;
        this.tokenAuthorization = tokenAuthorization;
    }
}
