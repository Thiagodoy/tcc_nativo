package com.example.thiago.tcc_nativo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchFile extends AppCompatActivity {


    List<Experimento> experimentos = new ArrayList<>();
    List<Experimento> experimentosSaida = new ArrayList<>();
    List<String>palavra10 = Arrays.asList(C.palavras10);
    List<String>palavra100 = Arrays.asList(C.Palavras100);

    Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_file);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        experimentos.add(new Experimento(10,"","y1"));
        experimentos.add(new Experimento(100,"","y2"));
        btn = (Button) findViewById(R.id.button3);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initTest();
            }
        });

    }

    public void initTest(){

        long init = System.currentTimeMillis();
        int count= 0;
        for (Experimento exp: experimentos) {
            for (int i = 0; i < 10; i++){
                long start = System.currentTimeMillis();
                List<String> current = exp.getQtd() == 10 ? palavra10 : palavra100;
                try {
                    for (String palavra: current) {
                        String bf = openFile(palavra.substring(0,1).toUpperCase());

                        Pattern pattern = Pattern.compile("\\*?\\s?\\*" + palavra +"\\*",Pattern.CASE_INSENSITIVE);
                        ///String line;

                        Matcher matcher = pattern.matcher(bf);

                        if(matcher.find()){
                            Log.i("Dicionario","###############################");
                            Log.i("Dicionario","Palavra - " + palavra);
                            Log.i("Dicionario","###############################");
                        }
                        //y:
//                        while((line = bf.readLine()) != null){
//                            Matcher matcher = pattern.matcher(line);
//
//                            if(matcher.find()){
//                                count++;
//                               Log.i("Dicionario","###############################");
//                               Log.i("Dicionario","Palavra - " + line);
//                                while(!(line = bf.readLine()).isEmpty()){
//                                    Log.i("Dicionario",line);
//                                }
//                                Log.i("Dicionario","###############################");
//                                break y;
//                            }
//                        }
//
//                        bf.close();
                    }


                }catch (Exception e){

                }


                long end = System.currentTimeMillis();

                Experimento e= new Experimento(exp.getQtd(),"busca",exp.getExp());
                e.setExec(i+1);
                e.setEnd(new Date(end));
                e.setInit(new Date(start));
                experimentosSaida.add(e);
            }
        }


        C.gravarLog(experimentosSaida,"EtapaDicionario.csv","SEARCHFILE");


        Log.i("time","" + (System.currentTimeMillis() - init));
        Log.i("time count","" +  count);

    }

    private String openFile(String inicial) throws Exception{



        StringWriter writer = new StringWriter();
        IOUtils.copy(getAssets().open(inicial + ".txt"), writer, "UTF-8");



        return writer.toString();//new BufferedReader(new InputStreamReader(getAssets().open(inicial + ".txt"), "UTF-8"));
    }

}
