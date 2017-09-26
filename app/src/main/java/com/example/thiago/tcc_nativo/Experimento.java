package com.example.thiago.tcc_nativo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

/**
 * Created by thiago on 9/23/17.
 */

public class Experimento extends Object implements Cloneable {

    long qtd;
    String type;
    String exp;
    Date init;
    Date end;
    long exec;

    public Experimento(long qtd, String type, String exp) {
        this.qtd = qtd;
        this.type = type;
        this.exp = exp;
    }

    @Override
    protected Experimento clone()  {

        try {
            return this.clone();
        }catch (Exception e){
            return null;
        }

    }

    public Date getInit() {
        return init;
    }

    public void setInit(Date init) {
        this.init = init;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public long getExec() {
        return exec;
    }

    public void setExec(long exec) {
        this.exec = exec;
    }

    public long getQtd() {
        return qtd;
    }

    public void setQtd(long qtd) {
        this.qtd = qtd;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExp() {
        return exp;
    }

    public void setExp(String exp) {
        this.exp = exp;
    }

    @Override
    public String toString() {
        return "" + getExec() + " " + getType() + " " + getQtd() + "\n"+
                new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").format(getInit()) + " - " + new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").format(getEnd());
    }
}
