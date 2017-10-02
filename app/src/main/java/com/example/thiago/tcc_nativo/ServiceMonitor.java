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
    private int memTotal, pId, intervalRead, intervalUpdate, intervalWidth, maxSamples = 2000;
    private long workT, totalT, workAMT, total, totalBefore, work, workBefore, workAM, workAMBefore;
    private String s;
    private String[] sa;
    private List<Float> cpuTotal, cpuAM;
    private List<Integer> memoryAM;
    private List<Map<String, Object>> mListSelected; // Integer		 C.pId
    // String		 C.pName
    // Integer	 C.work
    // Integer	 C.workBefore
    // List<Sring> C.finalValue
    //Bateria
    private long level = 0;
    BroadcastReceiver batteryLevelReceiver;

    private List<String> memUsed, memAvailable, memFree, cached, threshold;
    private ActivityManager am;
    private Debug.MemoryInfo[] amMI;
    private ActivityManager.MemoryInfo mi;
    //    private NotificationManager mNM;
//    private Notification mNotificationRead, mNotificationRecord;
    private BufferedReader reader;
    private BufferedWriter mW;
    private File mFile;
    private SharedPreferences mPrefs;
    private Runnable readRunnable = new Runnable() { // http://docs.oracle.com/javase/8/docs/technotes/guides/concurrency/threadPrimitiveDeprecation.html
        @Override
        public void run() {
            // The service makes use of an explicit Thread instead of a Handler because with the Threat the code is executed more synchronously.
            // However the ViewGraphic is drew with a Handler because the drawing code must be executed in the UI thread.
            Thread thisThread = Thread.currentThread();
            while (readThread == thisThread) {
                read();
                try {
                    Thread.sleep(100);
/*					synchronized (this) {
						while (readThread == thisThread && threadSuspended)
							wait();
					}*/
                } catch (InterruptedException e) {
                    break;
                }

                // The Runnable can be suspended and resumed with the below code:
//				threadSuspended = !threadSuspended;
//				if (!threadSuspended)
//					notify();
            }
        }

/*		public synchronized void stop() {
			readThread = null;
			notify();
		}*/


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
        intervalUpdate = mPrefs.getInt(C.intervalUpdate, C.defaultIntervalUpdate);
        intervalWidth = mPrefs.getInt(C.intervalWidth, C.defaultIntervalWidth);

        readThread.start();

        IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);;
        registerReceiver(batteryLevelReceiver, batteryLevelFilter);
        this.batteryLevel();

//		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(Constants.anotherMonitorEvent));
//        registerReceiver(receiverStartRecord, new IntentFilter(C.actionStartRecord));
//        registerReceiver(receiverStopRecord, new IntentFilter(C.actionStopRecord));
//        registerReceiver(receiverClose, new IntentFilter(C.actionClose));
//
//        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

//        PendingIntent contentIntent =  TaskStackBuilder.create(this)
////				.addParentStack(ActivityMain.class)
////				.addNextIntent(new Intent(this, ActivityMain.class))
//                .addNextIntentWithParentStack(new Intent(this, ActivityMain.class))
//                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
//        PendingIntent pIStartRecord = PendingIntent.getBroadcast(this, 0, new Intent(C.actionStartRecord), PendingIntent.FLAG_UPDATE_CURRENT);
//        PendingIntent pIStopRecord = PendingIntent.getBroadcast(this, 0, new Intent(C.actionStopRecord), PendingIntent.FLAG_UPDATE_CURRENT);
//        PendingIntent pIClose = PendingIntent.getBroadcast(this, 0, new Intent(C.actionClose), PendingIntent.FLAG_UPDATE_CURRENT);

//        mNotificationRead = new NotificationCompat.Builder(this)
//                .setContentTitle(getString(R.string.app_name))
//                .setContentText(getString(R.string.notify_read2))
////				.setTicker(getString(R.string.notify_read))
//                .setSmallIcon(R.drawable.icon_bw)
//                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon, null))
//                .setWhen(0) // Removes the time
//                .setOngoing(true)
//                .setContentIntent(contentIntent) // PendingIntent.getActivity(this, 0, new Intent(this, ActivityMain.class), 0)
//                .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.notify_read2)))
//                .addAction(R.drawable.icon_circle_sb, getString(R.string.menu_record), pIStartRecord)
//                .addAction(R.drawable.icon_times_ai, getString(R.string.menu_close), pIClose)
//                .build();

//        mNotificationRecord = new NotificationCompat.Builder(this)
//                .setContentTitle(getString(R.string.app_name))
//                .setContentText(getString(R.string.notify_record2))
//                .setTicker(getString(R.string.notify_record))
//                .setSmallIcon(R.drawable.icon_recording_bw)
//                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon_recording, null))
//                .setWhen(0)
//                .setOngoing(true)
//                .setContentIntent(contentIntent)
//                .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.notify_record2)))
//                .addAction(R.drawable.icon_stop_sb, getString(R.string.menu_stop_record), pIStopRecord)
//                .addAction(R.drawable.icon_times_ai, getString(R.string.menu_close), pIClose)
//                .build();

//		mNM.notify(0, mNotificationRead);
//        startForeground(10, mNotificationRead); // If not the AM service will be easily killed when a heavy-use memory app (like a browser or Google Maps) goes onto the foreground
    }





    @Override
    public void onDestroy() {
        if (recording)
            stopRecord();
//        mNM.cancelAll();

//        unregisterReceiver(receiverStartRecord);
//        unregisterReceiver(receiverStopRecord);
//        unregisterReceiver(receiverClose);

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
//                if (mListSelected != null && !mListSelected.isEmpty()) {
//                    List<Integer> l = (List<Integer>) (mListSelected.get(0)).get(C.pFinalValue);
//                    if (l != null && l.size() >= maxSamples)
//                        for (Map<String, Object> m : mListSelected) {
//                            ((List<Integer>) m.get(C.pFinalValue)).remove(l.size() - 1);
//                            ((List<Integer>) m.get(C.pTPD)).remove(((List<Integer>) m.get(C.pTPD)).size() - 1);
//                        }
//                }
//                if (mListSelected != null && !mListSelected.isEmpty()) {
//                    for (Map<String, Object> m : mListSelected) {
//                        List<Integer> l = (List<Integer>) m.get(C.pFinalValue);
//                        if (l == null)
//                            break;
//                        while (l.size() >= maxSamples)
//                            l.remove(l.size() - 1);
//                        l = (List<Integer>) m.get(C.pTPD);
//                        while (l.size() >= maxSamples)
//                            l.remove(l.size() - 1);
//                    }
//                }

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

            // http://stackoverflow.com/questions/3170691/how-to-get-current-memory-usage-in-android
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
//			Log.d("TotalPrivateDirty", String.valueOf(amMI[0].getTotalPrivateDirty()));
//			Log.d("TotalPrivateClean", String.valueOf(amMI[0].getTotalPrivateClean()));
//			Log.d("TotalPss", String.valueOf(amMI[0].getTotalPss()));
//			Log.d("TotalSharedDirty", String.valueOf(amMI[0].getTotalSharedDirty()));

//			CPU usage percents calculation. It is possible negative values or values higher than 100% may appear.
//			http://stackoverflow.com/questions/1420426
//			http://kernel.org/doc/Documentation/filesystems/proc.txt
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

            if (mListSelected != null && !mListSelected.isEmpty()) {
                int[] arrayPIds = new int[mListSelected.size()];
                synchronized (mListSelected) {
                    int n=0;
                    for (Map<String, Object> p : mListSelected) {
                        try {
                            if (p.get(C.pDead) == null) {
                                reader = new BufferedReader(new FileReader("/proc/" + p.get(C.pId) + "/stat"));
                                arrayPIds[n] = Integer.valueOf((String) p.get(C.pId));
                                ++n;
                                sa = reader.readLine().split("[ ]+", 18);
                                p.put(C.work, (float) Long.parseLong(sa[13]) + Long.parseLong(sa[14]) + Long.parseLong(sa[15]) + Long.parseLong(sa[16]));
                                reader.close();
                            }
                        } catch (FileNotFoundException e) {
                            p.put(C.pDead, Boolean.TRUE);
                            Intent intent = new Intent(C.actionDeadProcess);
                            intent.putExtra(C.process, (Serializable) p);
                            sendBroadcast(intent);
                        }
                    }
                }

                Debug.MemoryInfo[] mip = am.getProcessMemoryInfo(arrayPIds);
                int n = 0;
                for (Map<String, Object> entry : mListSelected) {
                    List<Integer> l = (List<Integer>) entry.get(C.pTPD);
                    if (l == null) {
                        l = new ArrayList<Integer>();
                        entry.put(C.pTPD, l);
                    }
                    if (entry.get(C.pDead) == null) {
//						if (mip[n].getTotalPrivateDirty() !=0
//								&& mip[n].getTotalPss() != 0
//								&& mip[n].getTotalSharedDirty() != 0) // To avoid dead processes
                        l.add(0, mip[n].getTotalPrivateDirty());
                        ++n;
                    } else l.add(0, 0);
                }
//				Log.d("MemoryInfo entries", String.valueOf(mip.length));
//				Log.d("List Selected entries", String.valueOf(mListSelected.size()));

//				Log.d("TotalSharedClean", String.valueOf(mi[0].getTotalSharedClean()));
//				Log.d("TotalSharedDirty", String.valueOf(mi[0].getTotalSharedDirty()));
//				Log.d("TotalPrivateClean", String.valueOf(mi[0].getTotalPrivateClean()));
//				Log.d("TotalPrivateDirty", String.valueOf(mi[0].getTotalPrivateDirty()));
//				Log.d("TotalPss", String.valueOf(mi[0].getTotalPss()));
//				Log.d("Pss", String.valueOf(Debug.getPss()));
//				Log.d("GlobalAllocSize", String.valueOf(Debug.getGlobalAllocSize()));
//				Log.d("NativeHeapSize", String.valueOf(Debug.getNativeHeapSize()/1024));
//				Log.d("NativeHeapAllocatedSize", String.valueOf(Debug.getNativeHeapAllocatedSize()/1024));
            }

            if (totalBefore != 0) {
                totalT = total - totalBefore;
                workT = work - workBefore;
                workAMT = workAM - workAMBefore;

                cpuTotal.add(0, restrictPercentage(workT * 100 / (float) totalT));
                cpuAM.add(0, restrictPercentage(workAMT * 100 / (float) totalT));

                if (mListSelected != null && !mListSelected.isEmpty()) {
                    int workPT = 0;
                    List<Float> l;

                    synchronized (mListSelected) {
                        for (Map<String, Object> p : mListSelected) {
                            if (p.get(C.workBefore) == null)
                                break;
                            l = (List<Float>) p.get(C.pFinalValue);
                            if (l == null) {
                                l = new ArrayList<Float>();
                                p.put(C.pFinalValue, l);
                            }
                            while (l.size() >= maxSamples)
                                l.remove(l.size() - 1);

                            workPT = (int) ((Float) p.get(C.work) - (Float) p.get(C.workBefore));
                            l.add(0, restrictPercentage(workPT * 100 / (float) totalT));
                        }
                    }
                }
            }

            totalBefore = total;
            workBefore = work;
            workAMBefore = workAM;

            if (mListSelected != null && !mListSelected.isEmpty())
                for (Map<String, Object> p : mListSelected)
                    p.put(C.workBefore, p.get(C.work));

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
                        .append("nativo")
                        .append("Inicio:,")
                        .append(getDate())
                        .append(",Intervalo:,")
                        .append(intervalRead)
                        .append(",Memória Total (kB),")
                        .append(memTotal)
                        .append("\nTime,Data,Total CPU usada (%)," + "nativo" + "(Pid ").append(Process.myPid()).append(") CPU usado (%),nativoApp memoria (kB)");
                if (mListSelected != null && !mListSelected.isEmpty())
                    for (Map<String, Object> p : mListSelected)
                        sb.append(",").append(p.get(C.pAppName)).append(" (Pid ").append(p.get(C.pId)).append(") CPU usada (%)")
                                .append(",").append(p.get(C.pAppName)).append(" Memoria (kB)");

                sb.append(",,Memória usada (kB),Memoria disponivel (MemFree+Cached) (kB),Memoria disponivel (kB),Cached (kB),Threshold (kB),Bateria,Plataforma");

                mW.write(sb.toString());
                topRow = false;
            }

            StringBuilder sb = new StringBuilder()
                    .append("\n").append(new Date().getTime())
                    .append(",").append(getDate())
                    .append(",").append(cpuTotal.get(0))
                    .append(",").append(cpuAM.get(0))
                    .append(",").append(memoryAM.get(0));
            sb.append(",")
                    .append(",").append(memUsed.get(0))
                    .append(",").append(memAvailable.get(0))
                    .append(",").append(memFree.get(0))
                    .append(",").append(cached.get(0))
                    .append(",").append(threshold.get(0))
                    .append(",").append(this.level)
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
            mW = null;

            // http://stackoverflow.com/questions/13737261/nexus-4-not-showing-files-via-mtp
//			MediaScannerConnection.scanFile(this, new String[] { mFile.getAbsolutePath() }, null, null);
            // http://stackoverflow.com/questions/5739140/mediascannerconnection-produces-android-app-serviceconnectionleaked
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).setData(Uri.fromFile(mFile)));

//            Toast.makeText(this, new StringBuilder().append(getString(R.string.app_name)).append("Record-").append(getDate()).append(".csv ")
//                    .append(getString(R.string.notify_toast_saved))
//                    .append(" " + Environment.getExternalStorageDirectory().getAbsolutePath() + "/AnotherMonitor"), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
//            Toast.makeText(this, getString(R.string.notify_toast_error) + " " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        topRow = true;
//        mNM.notify(10, mNotificationRead);
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
//            sendBroadcast(new Intent(C.actionSetIconRecord));

            // http://stackoverflow.com/questions/3875184/cant-create-handler-inside-thread-that-has-not-called-looper-prepare
//            new Handler(Looper.getMainLooper()).post(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(ServiceReader.this, getString(R.string.notify_toast_error_2) + " " + e.getMessage(), Toast.LENGTH_LONG).show();
//                }
//            });
//
//            mNM.notify(10, mNotificationRead);
        }
    }

    private void batteryLevel(){
        //Cria o broadcastreceiver
        batteryLevelReceiver = new BroadcastReceiver(){

            //Método executado sempre que o broadcast for executado
            public void onReceive(Context context, Intent  intent){
                context.unregisterReceiver(this);
                //pega nível da bateria da intent
                int rawLevel = intent.getIntExtra("level", -1);

                //pega o nível máximo da bateria
                int scale = intent.getIntExtra("scale", -1);
                level = -1;

                if(rawLevel >= 0 && scale > 0){
                    //faz o cálculo do percentual.
                    level = (rawLevel *100)/scale;
                }
            }
        };
    };




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
