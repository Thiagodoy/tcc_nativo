package com.example.thiago.tcc_nativo;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created by thiago on 9/19/17.
 */

public class C {

    static final String prefs = "Prefs";

    static final int defaultIntervalRead = 1000;
    static final String readThread = "readThread";

    static final String actionSetIconRecord = "actionSetIconRecord";
    static final String intervalRead = "intervalRead";



    static final String [] palavras10 = new String[]{"Secretário","Morango","Selote","Preparo","Irritação","Charuto","Massa","Lerdaço","Potável","Anchova"};
    static final String[] Palavras100 = new String[]{"Morango","Selo","Pijamas","Irritação","Charuto","Massa","Ler","Potável","Anchova",
            "Espectador","Caçarola","Preopinar","Labátia","Rabalha","Gonalgia","Abelhina","Poimento","Intelligibilidade", "Fundo","Montar",
            "Quinchoso","Aura", "Condonatário", "Ponderar","Tombante","Rijeira", "Siagonagra","Piratiningano","Loireiro","Relaxismo","Unguento",
            "Uval", "Wiedmânnia", "Ximbaúva", "Xiphódymo", "Zoofitologia", "Zaborreira", "Quartola", "Quartzoso", "Relance", "Conducente","Hispanista",
            "Hidróstato","Hysteria","Joeireiro","Justiçar","Optação","Opticógrapho","Desvaler","Desveladamente","Andadoria","Anelytro","Bago","Balça","Cália",
            "Cânhamo","Derrocar","Deschristianizar","Entumecer","Efluxão","Faringoscopia","Falsífico","Giesta","Glossálgia","Hérnico","Homográfico","Immaturidade",
            "Incompreensível","Jeroglífica","Jesuiticamente","Képi","Kneipista","Lagrimal","Lento","Meã","Medullar","Nefrotómico","Nequícia","Octobóthrio",
            "Offenbachesco","Propinação","Pyrrhonicamente","Quotidianamente","Quinquilharia","Reaparição","Recensão","Sachador","Sacalinha","Tangomau","Tarefa",
            "Umbráculo","Uretére","Valsar","Vassoiro","Watsónia","Xeringosa","Xystrópodes","Yorkino","Zabumba","Zooscópico"};


    static void gravarLog(List<Experimento> list, String nameFile,String etapa){

        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/ExperimentosNativo");
        dir.mkdir();
        File file = new File(dir,nameFile);
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write("experimento,execucao,tempo,inicio,fim,time_inicio,time_fim,quantidade,tipo,etapa,plataforma\n");

            Iterator<Experimento> it = list.iterator();

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
                log.append("," + etapa  );
                log.append(",ANDROID\n");
                writer.write(log.toString());

            }

            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private static String getDate(Date d) {
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
