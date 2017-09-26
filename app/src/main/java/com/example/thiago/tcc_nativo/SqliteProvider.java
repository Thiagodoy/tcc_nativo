package com.example.thiago.tcc_nativo;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.logging.Logger;

/**
 * Created by thiago on 9/23/17.
 */

public class SqliteProvider extends SQLiteOpenHelper {


    private SQLiteDatabase sqLiteDatabase;
    public SqliteProvider(Context context) {

        super(context, "teste.db", null, 1);
        sqLiteDatabase = getWritableDatabase();



    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        StringBuilder sql = new StringBuilder();
        sql.append("create table person(name VARCHAR(99),last_name VARCHAR(99),age INTEGER)");
        sqLiteDatabase.execSQL(sql.toString());
        this.sqLiteDatabase = sqLiteDatabase;
        Log.i("SQLITE","CRIO!");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public long insert(String table, ContentValues values){
        return this.sqLiteDatabase.insert(table,null,values);
    }

    public void select(String table){
        long count = this.sqLiteDatabase.query(table,new String[]{},null,null,null,null,null).getCount();
        Log.i("TCC-SELECT"," - " + count);

    }

    public void truncateTable(String table){
        this.sqLiteDatabase.delete(table,null,null);
    }

}
