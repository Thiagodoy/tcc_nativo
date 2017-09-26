package com.example.thiago.tcc_nativo;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class DataBase extends AppCompatActivity {


    List<Experimento> experimentos = new ArrayList<>();
    LinkedList<Experimento> resultado = new LinkedList<>();
    SqliteProvider sqliteProvider;
    ProgressBar progressBar;
    ListView lv;
    ArrayAdapter<String> arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);


        progressBar = (ProgressBar)findViewById(R.id.progressBar_cyclic);

        Button btn = (Button) findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                teste();
            }
        });


        experimentos.add(new Experimento(500,"escrita","y2"));
        experimentos.add(new Experimento(500,"leitura","y1"));
        experimentos.add(new Experimento(5000,"escrita","y4"));
        experimentos.add(new Experimento(5000,"leitura","y3"));

        this.sqliteProvider = new SqliteProvider(getBaseContext());



        lv = (ListView) findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        lv.setAdapter(arrayAdapter);

    }


    public void teste(){

        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);

        for (Experimento experimento: this.experimentos) {

            if(experimento.getType().equals("leitura")){

                this.sqliteProvider.truncateTable("person");
                for (int y = 0; y < experimento.getQtd(); y++){
                    ContentValues values = new ContentValues();
                    values.put("name","Lorem " + y);
                    values.put("last_name","Lorem " + y);
                    values.put("age", y);
                    this.sqliteProvider.insert("person",values);
                }

                for (int i = 0; i< 10;i++){
                    Experimento exp = new Experimento(experimento.getQtd(),experimento.getType(),experimento.getExp());
                    exp.setExec( i + 1);
                    exp.setInit(new Date());
                    this.sqliteProvider.select("person");
                    exp.setEnd(new Date());
                    resultado.add(exp);
                }

            }else{
                for (int i = 0; i< 10;i++){
                    this.sqliteProvider.truncateTable("person");
                    Experimento exp = new Experimento(experimento.getQtd(),experimento.getType(),experimento.getExp());
                    exp.setExec( i + 1);
                    exp.setInit(new Date());
                    for (int y = 0; y< experimento.getQtd(); y++){
                        ContentValues values = new ContentValues();
                        values.put("name","Lorem " + i);
                        values.put("last_name","Lorem " + i);
                        values.put("age",i);
                        this.sqliteProvider.insert("person",values);
                    }
                    exp.setEnd(new Date());
                    arrayAdapter.add(exp.toString());
                    resultado.add(exp);
                }
            }

        }
        progressBar.setVisibility(View.VISIBLE);
        this.gravarlog();

    }
    private void gravarlog(){


        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/ExperimentosNativo");
        dir.mkdir();
        File file = new File(dir,"EtapaSqlite.csv");
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write("experimento,execucao,tempo,inicio,fim,time_inicio,time_fim,quantidade,tipo,etapa,plataforma\n");

            Iterator<Experimento> it = this.resultado.iterator();

            while (it.hasNext()){
                StringBuilder log = new StringBuilder();
                Experimento exp = it.next();

                log.append("" + exp.getExp());
                log.append("," + exp.getExec());
                log.append("," + (exp.getEnd().getTime() - exp.getInit().getTime()));
                log.append("," + getDate(exp.getInit()));
                log.append("," + getDate(exp.getEnd()));
                log.append("," + exp.getInit().getTime());
                log.append("," + exp.getEnd().getTime());
                log.append("," + exp.getQtd());
                log.append("," + exp.getType());
                log.append(",SQLITE" );
                log.append(",ANDROID\n");
                writer.write(log.toString());

            }

            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String getDate(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        DecimalFormat df = new DecimalFormat("00");
        DecimalFormat dff = new DecimalFormat("000");


        return new StringBuilder()
                .append(df.format(c.get(Calendar.YEAR))).append("-")
                .append(df.format(c.get(Calendar.MONTH) + 1)).append("-")
                .append(df.format(c.get(Calendar.DATE))).append(" ")
                .append(df.format(c.get(Calendar.HOUR_OF_DAY))).append(":")
                .append(df.format(c.get(Calendar.MINUTE))).append(":")
                .append(df.format(c.get(Calendar.SECOND))).append(".")
                .append(dff.format(c.get(Calendar.MILLISECOND))).append("").toString();

    }
}
