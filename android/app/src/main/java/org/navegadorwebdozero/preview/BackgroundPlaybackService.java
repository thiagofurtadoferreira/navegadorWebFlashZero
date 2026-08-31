package org.navegadorwebdozero.preview;

import android.app.*;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class BackgroundPlaybackService extends Service {
    private static final String CHANNEL="nwz_background_playback";
    @Override public void onCreate(){
        super.onCreate();
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel ch=new NotificationChannel(CHANNEL,"Reprodução em segundo plano",NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Mantém áudio e vídeo da página ativos enquanto o navegador está em segundo plano.");
            nm.createNotificationChannel(ch);
        }
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        b.setContentTitle("Navegador Web Flash Zero")
         .setContentText("Reprodução em segundo plano ativa")
         .setSmallIcon(android.R.drawable.ic_media_play)
         .setOngoing(true)
         .setCategory(Notification.CATEGORY_SERVICE);
        startForeground(460,b.build());
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){return START_NOT_STICKY;}
    @Override public IBinder onBind(Intent intent){return null;}
}
