package org.navegadorwebdozero.preview;

import android.app.*;
import android.content.*;
import android.hardware.display.*;
import android.media.MediaRecorder;
import android.media.projection.*;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.widget.Toast;
import java.io.File;
import android.os.ParcelFileDescriptor;
import java.util.UUID;

public class ScreenRecordingService extends Service {
    public static final String ACTION_START="nwz.record.START", ACTION_STOP="nwz.record.STOP";
    public static final String EXTRA_RESULT_CODE="resultCode", EXTRA_RESULT_DATA="resultData";
    private static final String CHANNEL="screen_recording";
    private MediaProjection projection; private MediaRecorder recorder; private VirtualDisplay display; private File output; private Uri outputUri; private ParcelFileDescriptor outputPfd; private String outputName;

    @Override public void onCreate(){super.onCreate();if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel(CHANNEL,"Gravação de tela",NotificationManager.IMPORTANCE_LOW);getSystemService(NotificationManager.class).createNotificationChannel(ch);}}
    @Override public int onStartCommand(Intent intent,int flags,int startId){if(intent==null)return START_NOT_STICKY;if(ACTION_STOP.equals(intent.getAction())){stopRecording();stopSelf();return START_NOT_STICKY;}if(ACTION_START.equals(intent.getAction())){startForeground(41,new Notification.Builder(this,Build.VERSION.SDK_INT>=26?CHANNEL:"").setContentTitle("Navegador Web Flash Zero").setContentText("Gravando a tela").setSmallIcon(android.R.drawable.presence_video_online).setOngoing(true).build());int rc=intent.getIntExtra(EXTRA_RESULT_CODE,Activity.RESULT_CANCELED);Intent data=Build.VERSION.SDK_INT>=33?intent.getParcelableExtra(EXTRA_RESULT_DATA,Intent.class):intent.getParcelableExtra(EXTRA_RESULT_DATA);startRecording(rc,data);}return START_NOT_STICKY;}

    private void startRecording(int resultCode,Intent data){
        if(data==null||resultCode!=Activity.RESULT_OK){stopSelf();return;}
        try{
            MediaProjectionManager pm=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);projection=pm.getMediaProjection(resultCode,data);
            DisplayMetrics dm=getResources().getDisplayMetrics();boolean portrait=dm.heightPixels>=dm.widthPixels;int width=portrait?720:1600,height=portrait?1600:720;
            outputName="NWZFZ-"+UUID.randomUUID()+".mp4";
            recorder=new MediaRecorder();
            boolean audioGranted=checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)==android.content.pm.PackageManager.PERMISSION_GRANTED;
            if(audioGranted)recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);if(audioGranted){recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);recorder.setAudioSamplingRate(44100);recorder.setAudioChannels(1);recorder.setAudioEncodingBitRate(128000);}
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);recorder.setVideoSize(width,height);recorder.setVideoFrameRate(24);recorder.setVideoEncodingBitRate(3_500_000);
            if(Build.VERSION.SDK_INT>=29){ContentValues cv=new ContentValues();cv.put(MediaStore.MediaColumns.DISPLAY_NAME,outputName);cv.put(MediaStore.MediaColumns.MIME_TYPE,"video/mp4");cv.put(MediaStore.MediaColumns.RELATIVE_PATH,"Download/Navegador Web Flash Zero");cv.put(MediaStore.MediaColumns.IS_PENDING,1);outputUri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,cv);if(outputUri==null)throw new IllegalStateException("MediaStore não criou a gravação");outputPfd=getContentResolver().openFileDescriptor(outputUri,"w");if(outputPfd==null)throw new IllegalStateException("Arquivo de saída indisponível");recorder.setOutputFile(outputPfd.getFileDescriptor());}
            else{File dir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);File appDir=new File(dir,"Navegador Web Flash Zero");if(!appDir.exists()&&!appDir.mkdirs())throw new IllegalStateException("Não foi possível criar Downloads");output=new File(appDir,outputName);recorder.setOutputFile(output.getAbsolutePath());}
            recorder.prepare();
            Surface surface=recorder.getSurface();display=projection.createVirtualDisplay("NWZFZ-Screen",width,height,dm.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,surface,null,null);recorder.start();
        }catch(Throwable t){if(outputUri!=null)try{getContentResolver().delete(outputUri,null,null);}catch(Throwable ignored){}if(output!=null&&output.exists())output.delete();Toast.makeText(this,"Não foi possível iniciar a gravação: "+t.getClass().getSimpleName(),Toast.LENGTH_LONG).show();stopRecording();stopSelf();}
    }
    private void stopRecording(){try{if(recorder!=null)recorder.stop();}catch(Throwable ignored){}try{if(recorder!=null)recorder.release();}catch(Throwable ignored){}recorder=null;try{if(display!=null)display.release();}catch(Throwable ignored){}display=null;try{if(projection!=null)projection.stop();}catch(Throwable ignored){}projection=null;try{if(outputPfd!=null)outputPfd.close();}catch(Throwable ignored){}outputPfd=null;if(Build.VERSION.SDK_INT>=29&&outputUri!=null){try{ContentValues cv=new ContentValues();cv.put(MediaStore.MediaColumns.IS_PENDING,0);getContentResolver().update(outputUri,cv,null,null);}catch(Throwable ignored){}}if(outputUri!=null|| (output!=null&&output.exists()&&output.length()>0))Toast.makeText(this,"Gravação salva em Downloads/Navegador Web Flash Zero: "+outputName,Toast.LENGTH_LONG).show();stopForeground(STOP_FOREGROUND_REMOVE);}
    @Override public void onDestroy(){stopRecording();super.onDestroy();}
    @Override public android.os.IBinder onBind(Intent intent){return null;}
}
