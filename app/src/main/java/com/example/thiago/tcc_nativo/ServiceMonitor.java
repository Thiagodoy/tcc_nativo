package com.example.thiago.tcc_nativo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.support.annotation.Nullable;

import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by thiago on 9/19/17.
 */

public class    ServiceMonitor extends Service {


    private boolean /*threadSuspended, */recording=true, firstRead = true, topRow = true;
    private int memTotal, pId, intervalRead, maxSamples = 2000;
    private long workT, totalT, workAMT, total, totalBefore, work, workBefore, workAM, workAMBefore;
    private String s;
    private String[] sa;
    private List<Float> cpuTotal, cpuAM;
    private List<Integer> memoryAM;

    private List<String> memUsed, memAvailable, memFree, cached, threshold;
    private ActivityManager am;
    private Debug.MemoryInfo[] amMI;
    private ActivityManager.MemoryInfo mi;

    private BufferedReader reader;
    private BufferedWriter mW;
    private File mFile;
    private SharedPreferences mPrefs;
    private Runnable readRunnable = new Runnable() {
        @Override
        public void run() {

            Thread thisThread = Thread.currentThread();
            while (readThread == thisThread) {
                read();
                try {
                    Thread.sleep(150);

                } catch (InterruptedException e) {
                    break;
                }


            }
        }



    };
    private volatile Thread readThread = new Thread(readRunnable, C.readThread);






    class ServiceMonitorDataBinder extends Binder {
        ServiceMonitor getService() {
            return ServiceMonitor.this;
        }
    }

    @Override
    public void onCreate() {
        cpuTotal = new ArrayList<Float>(maxSamples);
        cpuAM = new ArrayList<Float>(maxSamples);
        memoryAM = new ArrayList<Integer>(maxSamples);
        memUsed = new ArrayList<String>(maxSamples);
        memAvailable = new ArrayList<String>(maxSamples);
        memFree = new ArrayList<String>(maxSamples);
        cached = new ArrayList<String>(maxSamples);
        threshold = new ArrayList<String>(maxSamples);

        pId = Process.myPid();

        am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        amMI = am.getProcessMemoryInfo(new int[]{ pId });
        mi = new ActivityManager.MemoryInfo();

        mPrefs = getSharedPreferences(getString(R.string.app_name) + C.prefs, MODE_PRIVATE);
        intervalRead = mPrefs.getInt(C.intervalRead, C.defaultIntervalRead);


        readThread.start();


    }





    @Override
    public void onDestroy() {
        if (recording)
            stopRecord();


        try {
            readThread.interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
        synchronized (this) {
            readThread = null;
            notify();
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return new ServiceMonitorDataBinder();
    }


    @SuppressLint("NewApi")
    @SuppressWarnings("unchecked")
    private void read() {
        try {
            reader = new BufferedReader(new FileReader("/proc/meminfo"));
            s = reader.readLine();
            while (s != null) {
                // Memory is limited as far as we know
                while (memFree.size() >= maxSamples) {
                    cpuTotal.remove(cpuTotal.size() - 1);
                    cpuAM.remove(cpuAM.size() - 1);
                    memoryAM.remove(memoryAM.size() - 1);

                    memUsed.remove(memUsed.size() - 1);
                    memAvailable.remove(memAvailable.size() - 1);
                    memFree.remove(memFree.size() - 1);
                    cached.remove(cached.size() - 1);
                    threshold.remove(threshold.size() - 1);
                }


                // Memory values. Percentages are calculated in the ActivityMain class.
                if (firstRead && s.startsWith("MemTotal:")) {
                    memTotal = Integer.parseInt(s.split("[ ]+", 3)[1]);
                    firstRead = false;
                } else if (s.startsWith("MemFree:"))
                    memFree.add(0, s.split("[ ]+", 3)[1]);
                else if (s.startsWith("Cached:"))
                    cached.add(0, s.split("[ ]+", 3)[1]);

                s = reader.readLine();
            }
            reader.close();

            am.getMemoryInfo(mi);
            if (mi == null) { // Sometimes mi is null
                memUsed.add(0, String.valueOf(0));
                memAvailable.add(0, String.valueOf(0));
                threshold.add(0, String.valueOf(0));
            } else {
                memUsed.add(0, String.valueOf(memTotal - mi.availMem/1024));
                memAvailable.add(0, String.valueOf(mi.availMem/1024));
                threshold.add(0, String.valueOf(mi.threshold/1024));
            }

            memoryAM.add(amMI[0].getTotalPrivateDirty());
			Log.d("TotalPrivateDirty", String.valueOf(amMI[0].getTotalPrivateDirty()));
			/*Log.d("TotalPrivateClean", String.valueOf(amMI[0].getTotalPrivateClean()));
			Log.d("TotalPss", String.valueOf(amMI[0].getTotalPss()));
			Log.d("TotalSharedDirty", String.valueOf(amMI[0].getTotalSharedDirty()));
            Log.d("MemoryStats", String.valueOf(amMI[0].getMemoryStats()));

            Debug.MemoryInfo memInfo = new Debug.MemoryInfo();
            Debug.getMemoryInfo(memInfo);
            long res = memInfo.getTotalPrivateDirty();

            Log.d("getTotalPrivateDirt22", "" + (res * 1024L));

            Log.d("Runtime", "###########");
            Log.d("Runtime total memory", "" + Runtime.getRuntime().totalMemory());
            Log.d("Runtime free memory", "" + Runtime.getRuntime().freeMemory());
            Log.d("Runtime alocate memory", "" + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
            Log.d("Runtime percentual","" + (((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) * 100)/Runtime.getRuntime().totalMemory())/100d+" %");
            */

            if (Build.VERSION.SDK_INT < 26) {
                reader = new BufferedReader(new FileReader("/proc/stat"));
                sa = reader.readLine().split("[ ]+", 9);
                work = Long.parseLong(sa[1]) + Long.parseLong(sa[2]) + Long.parseLong(sa[3]);
                total = work + Long.parseLong(sa[4]) + Long.parseLong(sa[5]) + Long.parseLong(sa[6]) + Long.parseLong(sa[7]);
                reader.close();
            }

            reader = new BufferedReader(new FileReader("/proc/" + pId + "/stat"));
            sa = reader.readLine().split("[ ]+", 18);
            workAM = Long.parseLong(sa[13]) + Long.parseLong(sa[14]) + Long.parseLong(sa[15]) + Long.parseLong(sa[16]);
            reader.close();

            if (totalBefore != 0) {
                totalT = total - totalBefore;
                workT = work - workBefore;
                workAMT = workAM - workAMBefore;

                cpuTotal.add(0, restrictPercentage(workT * 100 / (float) totalT));
                cpuAM.add(0, restrictPercentage(workAMT * 100 / (float) totalT));

            }

            totalBefore = total;
            workBefore = work;
            workAMBefore = workAM;

            reader.close();

            if (recording)
                record();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private float restrictPercentage(float percentage) {
        if (percentage > 100)
            return 100;
        else if (percentage < 0)
            return 0;
        else return percentage;
    }


    @SuppressWarnings("unchecked")
    private void record() {


        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, getString(R.string.storage_permission), Toast.LENGTH_LONG).show();
            return;
        }



        if (mW == null) {
            File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/TccMonitor");
            dir.mkdirs();
            mFile = new File( dir,"variaveis_resposta_nativo.csv");

            try {
                mW = new BufferedWriter(new FileWriter(mFile));
            } catch (IOException e) {
                notifyError(e);
                return;
            }
        }

        try {
            if (topRow) {



                StringBuilder sb = new StringBuilder()
                        .append("Inicio:,")
                        .append(getDate())
                        .append(",Intervalo:,")
                        .append(150)
                        .append(",Memória Total (Mb),")
                        .append(Runtime.getRuntime().totalMemory()/2048)
                        .append("\ntime,data,cpu_usada_percentual, memoria_total_mb, memoria_livre_Mb,memoria_alocada_percentual,plataforma");


                mW.write(sb.toString());
                topRow = false;
            }

            StringBuilder sb = new StringBuilder()
                    .append("\n").append(new Date().getTime()) //time
                    .append(",").append(getDate())//data
                    .append(",").append(cpuAM.get(0))// processador processo
                    //.append(",").append(Runtime.getRuntime().totalMemory()/2048)//memoria do processo
                    .append(",").append(memoryAM.get(0)/2048)
                    .append(",").append(Runtime.getRuntime().freeMemory()/2048)
                    .append(",").append((((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) * 100)/Runtime.getRuntime().totalMemory())/100d)
                    .append(",").append("ANDROID");

            mW.write(sb.toString());
        } catch (IOException e) {
            notifyError(e);
        }
    }

    void startRecord() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, getString(R.string.storage_permission), Toast.LENGTH_LONG).show();
            return;
        }
        recording = true;
        sendBroadcast(new Intent(C.actionSetIconRecord));
    }

    void stopRecord() {
        recording = false;
        sendBroadcast(new Intent(C.actionSetIconRecord));
        try {
            mW.flush();
            mW.close();
            mW = null;;
        } catch (Exception e) {
            e.printStackTrace();
        }
        topRow = true;

    }

    boolean isRecording() {
        return recording;
    }





    void notifyError(final IOException e) {
        e.printStackTrace();
        if (mW != null)
            stopRecord();
        else {
            recording = false;
        }
    }


    private String getDate() {
        Calendar c = Calendar.getInstance();
        DecimalFormat df = new DecimalFormat("00");


        return new StringBuilder()
                .append(df.format(c.get(Calendar.YEAR))).append("-")
                .append(df.format(c.get(Calendar.MONTH) + 1)).append("-")
                .append(df.format(c.get(Calendar.DATE))).append(" ")
                .append(df.format(c.get(Calendar.HOUR_OF_DAY))).append(":")
                .append(df.format(c.get(Calendar.MINUTE))).append(":")
                .append(df.format(c.get(Calendar.SECOND))).append(".")
                .append(df.format(c.get(Calendar.MILLISECOND))).append("").toString();

    }

}
