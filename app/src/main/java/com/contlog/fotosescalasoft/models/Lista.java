package com.contlog.fotosescalasoft.models;

import java.util.ArrayList;
import java.util.List;

public class Lista {
    public List<Anexo> anexos = new ArrayList<>();

    public List<Anexo> addLista(Anexo anexo){
        anexos.add(anexo);
        return anexos;
    }

}
