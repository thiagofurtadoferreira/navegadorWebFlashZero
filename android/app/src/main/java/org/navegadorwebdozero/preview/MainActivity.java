package org.navegadorwebdozero.preview;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.provider.MediaStore;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.webkit.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int REQ_CAPTURE = 7001;
    private static final String HOME = "https://duckduckgo.com/";

    private final ArrayList<String> tabs = new ArrayList<>();
    private final ArrayList<String> suggestions = new ArrayList<>();
    private WebView web;
    private AutoCompleteTextView address;
    private LinearLayout root, chrome;
    private TextView tabCount, plusButton;
    private SharedPreferences prefs;
    private boolean darkMode, blueFilter, autoHide, showVideoTools, backgroundPlayback, saveCredentials, historyEnabled, cookiesEnabled, recording, recordAreaMode, mediaPlaying;
    private int barPosition, currentTab;
    private Uri lastRecordingTapUri; private long lastRecordingTapMs;
    private float touchStartY;

    private static final String[] BLOCKED = {
        "doubleclick.net","googlesyndication.com","googleadservices.com","adservice.google.com",
        "securepubads.g.doubleclick.net","googletagmanager.com","google-analytics.com","facebook.net",
        "scorecardresearch.com","taboola.com","outbrain.com"
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        prefs = getSharedPreferences("nwz_settings", MODE_PRIVATE);
        barPosition = prefs.getInt("barPosition", 0);
        autoHide = prefs.getBoolean("autoHide", false);
        darkMode = prefs.getBoolean("darkMode", true);
        blueFilter = prefs.getBoolean("blueFilter", false);
        showVideoTools = prefs.getBoolean("showVideoTools", true);
        backgroundPlayback = prefs.getBoolean("backgroundPlayback", false);
        saveCredentials = prefs.getBoolean("saveCredentials", false);
        historyEnabled = prefs.getBoolean("historyEnabled", true);
        cookiesEnabled = prefs.getBoolean("cookiesEnabled", true);
        buildUi();
        applyPrivacySettings();
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) { requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 7002); }
        String restoreUrl = historyEnabled ? prefs.getString("lastUrl", HOME) : HOME;
        if (restoreUrl == null || restoreUrl.trim().isEmpty()) restoreUrl = HOME;
        tabs.add(restoreUrl);
        currentTab = 0;
        updateTabCount();
        navigate(restoreUrl);
    }

    private TextView icon(String text) {
        TextView v = new TextView(this);
        v.setText(text); v.setTextSize(26); v.setGravity(Gravity.CENTER);
        v.setPadding(dp(10),dp(4),dp(10),dp(4)); v.setMinWidth(dp(44));
        v.setOnClickListener(this::handleClick);
        return v;
    }

    private void buildUi() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        chrome = new LinearLayout(this); chrome.setOrientation(LinearLayout.HORIZONTAL); chrome.setGravity(Gravity.CENTER_VERTICAL);
        TextView home=icon("⌂"); home.setId(1); chrome.addView(home);
        address = new AutoCompleteTextView(this);
        address.setSingleLine(true); address.setSelectAllOnFocus(true); address.setThreshold(1);
        address.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        address.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_GO);
        address.setHint("Pesquisar ou digitar endereço");
        suggestions.addAll(Arrays.asList("duckduckgo.com","google.com","youtube.com","wikipedia.org","github.com","joguebingou.com.br","ilhadomarajo.org","cbn.globo.com","dol.com.br","tudogostoso.com.br"));
        refreshSuggestionAdapter();
        address.setOnItemClickListener((p,v,pos,id)->{ Object item=p.getItemAtPosition(pos); if(item!=null){address.setText(item.toString()); hideKeyboard(); navigate(item.toString());}});
        address.setOnEditorActionListener((v,a,e)->{hideKeyboard();navigate(address.getText().toString());return true;});
        chrome.addView(address,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView go=icon("➜"); go.setId(2); chrome.addView(go);
        plusButton=icon("+"); plusButton.setId(3); chrome.addView(plusButton);
        tabCount=icon("1"); tabCount.setId(4); chrome.addView(tabCount);
        TextView menu=icon("⋮"); menu.setId(5); chrome.addView(menu);

        web = new WebView(this);
        WebSettings ws=web.getSettings(); ws.setJavaScriptEnabled(true); ws.setDomStorageEnabled(true); ws.setAllowFileAccess(false); ws.setAllowContentAccess(false); ws.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW); ws.setMediaPlaybackRequiresUserGesture(false);
        if (Build.VERSION.SDK_INT >= 26) web.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false);
        web.setBackgroundColor(darkMode?0xff121212:Color.WHITE);
        web.setWebViewClient(new BrowserClient());
        web.setOnScrollChangeListener((v,x,y,ox,oy)->{if(autoHide&&!recording&&Math.abs(y-oy)>6)chrome.setVisibility(View.GONE);hideKeyboard();});
        web.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN)touchStartY=e.getY();if(e.getAction()==MotionEvent.ACTION_MOVE&&autoHide){float dy=e.getY()-touchStartY;if(barPosition==0&&!web.canScrollVertically(-1)&&dy>dp(28))chrome.setVisibility(View.VISIBLE);if(barPosition==1&&!web.canScrollVertically(1)&&dy<-dp(28))chrome.setVisibility(View.VISIBLE);}if(e.getAction()==MotionEvent.ACTION_UP)hideKeyboard();return false;});
        applyChromeTheme(); rebuildRoot(); setContentView(root);
    }

    private void rebuildRoot(){root.removeAllViews();if(barPosition==0)root.addView(chrome,new LinearLayout.LayoutParams(-1,dp(52)));root.addView(web,new LinearLayout.LayoutParams(-1,0,1));if(barPosition==1)root.addView(chrome,new LinearLayout.LayoutParams(-1,dp(52)));}

    private void refreshSuggestionAdapter(){
        ArrayAdapter<String> ad=new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line,suggestions){@Override public View getView(int p,View c,ViewGroup parent){TextView t=(TextView)super.getView(p,c,parent);styleSuggestion(t);return t;}@Override public View getDropDownView(int p,View c,ViewGroup parent){TextView t=(TextView)super.getDropDownView(p,c,parent);styleSuggestion(t);return t;}};
        address.setAdapter(ad); address.setDropDownBackgroundDrawable(new ColorDrawable(darkMode?0xff25272a:Color.WHITE));
    }
    private void styleSuggestion(TextView t){t.setTextColor(darkMode?Color.WHITE:Color.BLACK);t.setBackgroundColor(darkMode?0xff25272a:Color.WHITE);t.setPadding(dp(16),dp(14),dp(16),dp(14));}

    private void handleClick(View v){switch(v.getId()){case 1:navigate(HOME);break;case 2:hideKeyboard();navigate(address.getText().toString());break;case 3:if(recording){stopScreenRecording();}else{saveCurrentTab();tabs.add(HOME);currentTab=tabs.size()-1;updateTabCount();navigate(HOME);}break;case 4:showTabs();break;case 5:showMenu(v);break;}}

    private void showMenu(View anchor){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);int bg=darkMode?0xff202226:Color.WHITE;box.setBackgroundColor(bg);box.setPadding(dp(8),dp(8),dp(8),dp(8));
        PopupWindow pw=new PopupWindow(box,dp(280),WindowManager.LayoutParams.WRAP_CONTENT,true);pw.setBackgroundDrawable(new ColorDrawable(bg));pw.setOutsideTouchable(true);pw.setElevation(dp(8));
        addMenu(box,"←  Voltar",()->{if(web.canGoBack())web.goBack();pw.dismiss();});
        addMenu(box,"→  Avançar",()->{if(web.canGoForward())web.goForward();pw.dismiss();});
        addMenu(box,"↻  Recarregar",()->{web.reload();pw.dismiss();});
        addMenu(box,"⏺  Gravar tela inteira",()->{recordAreaMode=false;requestScreenRecording();pw.dismiss();});
        addMenu(box,"▣  Gravar área selecionada",()->{recordAreaMode=true;chrome.setVisibility(View.GONE);getWindow().getDecorView().setSystemUiVisibility(5894);web.evaluateJavascript(AREA_SELECT_JS,null);pw.dismiss();Toast.makeText(this,"Ajuste X1, X2, Y1 e Y2 e toque em Iniciar gravação",Toast.LENGTH_SHORT).show();});
        addMenu(box,"■  Parar gravação",()->{stopScreenRecording();pw.dismiss();});
        addMenu(box,"▤  Gravações",()->{pw.dismiss();showRecordings();});
        addMenu(box,"⚙  Configurações",()->{pw.dismiss();showSettings();});
        pw.showAsDropDown(anchor,-dp(240),0);
    }
    private void addMenu(LinearLayout box,String label,Runnable r){TextView t=new TextView(this);t.setText(label);t.setTextSize(17);t.setPadding(dp(16),dp(14),dp(16),dp(14));t.setTextColor(darkMode?Color.WHITE:Color.BLACK);t.setOnClickListener(v->r.run());box.addView(t);}

    private void showSettings(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(12),dp(18),dp(12));box.setBackgroundColor(darkMode?0xff1b1d20:Color.WHITE);
        TextView title=new TextView(this);title.setText("Configurações");title.setTextSize(22);title.setTextColor(darkMode?Color.WHITE:Color.BLACK);title.setPadding(0,0,0,dp(10));box.addView(title);
        Switch top=switchRow("Barra superior",barPosition==0),bottom=switchRow("Barra inferior",barPosition==1),ah=switchRow("Auto-ocultar barra",autoHide),dm=switchRow("Modo escuro",darkMode),bf=switchRow("Filtro de luz azul",blueFilter),vt=switchRow("Exibir botões de vídeo",showVideoTools),bg=switchRow("Reprodução em segundo plano",backgroundPlayback),sc=switchRow("Salvar credenciais",saveCredentials),hi=switchRow("Histórico",historyEnabled),ck=switchRow("Cookies",cookiesEnabled);
        box.addView(top);box.addView(bottom);box.addView(ah);box.addView(dm);box.addView(bf);box.addView(vt);box.addView(bg);box.addView(sc);box.addView(hi);box.addView(ck);
        AlertDialog dlg=new AlertDialog.Builder(this).setView(box).setNegativeButton("Fechar",null).create();
        top.setOnCheckedChangeListener((b,c)->{if(c){barPosition=0;bottom.setChecked(false);savePrefs();rebuildRoot();}});
        bottom.setOnCheckedChangeListener((b,c)->{if(c){barPosition=1;top.setChecked(false);savePrefs();rebuildRoot();}});
        ah.setOnCheckedChangeListener((b,c)->{autoHide=c;savePrefs();if(!c)chrome.setVisibility(View.VISIBLE);});
        dm.setOnCheckedChangeListener((b,c)->{darkMode=c;savePrefs();applyChromeTheme();refreshSuggestionAdapter();applyVisualFilters();box.setBackgroundColor(darkMode?0xff1b1d20:Color.WHITE);title.setTextColor(darkMode?Color.WHITE:Color.BLACK);});
        bf.setOnCheckedChangeListener((b,c)->{blueFilter=c;savePrefs();applyVisualFilters();});
        vt.setOnCheckedChangeListener((b,c)->{showVideoTools=c;savePrefs();injectVideoTools();});
        bg.setOnCheckedChangeListener((b,c)->{backgroundPlayback=c;savePrefs();applyBackgroundPlayback();});
        sc.setOnCheckedChangeListener((b,c)->{saveCredentials=c;savePrefs();applyPrivacySettings();});
        hi.setOnCheckedChangeListener((b,c)->{historyEnabled=c;savePrefs();applyPrivacySettings();});
        ck.setOnCheckedChangeListener((b,c)->{cookiesEnabled=c;savePrefs();applyPrivacySettings();}); dlg.show();
    }
    private Switch switchRow(String text,boolean checked){Switch s=new Switch(this);s.setText(text);s.setTextSize(17);s.setChecked(checked);s.setPadding(dp(4),dp(10),dp(4),dp(10));s.setTextColor(darkMode?Color.WHITE:Color.BLACK);return s;}

    private void showTabs(){
        saveCurrentTab();
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(12),dp(12),dp(12),dp(12));box.setBackgroundColor(darkMode?0xff1b1d20:Color.WHITE);
        TextView h=new TextView(this);h.setText("Abas abertas");h.setTextSize(22);h.setTextColor(darkMode?Color.WHITE:Color.BLACK);h.setPadding(dp(8),dp(8),dp(8),dp(12));box.addView(h);
        AlertDialog dlg=new AlertDialog.Builder(this).setView(box).create();
        for(int i=0;i<tabs.size();i++) addTabRow(box,dlg,i);
        dlg.show();
    }

    private void addTabRow(LinearLayout box,AlertDialog dlg,int index){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(4),dp(3),dp(4),dp(3));
        TextView label=new TextView(this);label.setText((index==currentTab?"●  ":"○  ")+tabs.get(index));label.setTextSize(16);label.setTextColor(darkMode?Color.WHITE:Color.BLACK);label.setPadding(dp(12),dp(14),dp(8),dp(14));
        TextView close=new TextView(this);close.setText("✕");close.setTextSize(24);close.setGravity(Gravity.CENTER);close.setTextColor(darkMode?0xffe8eaed:0xff303134);close.setPadding(dp(14),dp(10),dp(14),dp(10));
        final float[] sx={0f};
        label.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN){sx[0]=e.getX();return false;}if(e.getAction()==MotionEvent.ACTION_UP&&Math.abs(e.getX()-sx[0])>dp(72)){closeTab(index);dlg.dismiss();showTabs();return true;}return false;});
        label.setOnClickListener(v->{if(index<tabs.size()){currentTab=index;updateTabCount();navigate(tabs.get(index));dlg.dismiss();}});
        close.setOnClickListener(v->{closeTab(index);dlg.dismiss();showTabs();});
        row.addView(label,new LinearLayout.LayoutParams(0,WindowManager.LayoutParams.WRAP_CONTENT,1));row.addView(close,new LinearLayout.LayoutParams(dp(54),WindowManager.LayoutParams.WRAP_CONTENT));box.addView(row);
    }

    private void closeTab(int index){
        if(index<0||index>=tabs.size())return;
        if(tabs.size()==1){tabs.set(0,HOME);currentTab=0;navigate(HOME);updateTabCount();return;}
        tabs.remove(index);
        if(index==currentTab)currentTab=Math.min(index,tabs.size()-1);else if(index<currentTab)currentTab--;
        updateTabCount();navigate(tabs.get(currentTab));
    }

    private void navigate(String raw){String s=raw==null?"":raw.trim();if(s.isEmpty())return;if(!s.startsWith("http://")&&!s.startsWith("https://")){if(s.contains(".")&&!s.contains(" "))s="https://"+s;else s="https://duckduckgo.com/?q="+Uri.encode(s);}address.setText(s);web.loadUrl(s);saveCurrentTab();}
    private void saveCurrentTab(){if(currentTab>=0&&currentTab<tabs.size()){String u=web==null?null:web.getUrl();if(u!=null&&!u.startsWith("data:"))tabs.set(currentTab,u);}}
    private void updateTabCount(){if(tabCount!=null)tabCount.setText(String.valueOf(tabs.size()));}


    private void showRecordings(){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(18),dp(12),dp(18),dp(12)); box.setBackgroundColor(darkMode?0xff1b1d20:Color.WHITE);
        TextView title=new TextView(this); title.setText("Gravações — Downloads/Navegador Web Flash Zero"); title.setTextSize(20); title.setTextColor(darkMode?Color.WHITE:Color.BLACK); title.setPadding(0,0,0,dp(12)); box.addView(title);
        if(Build.VERSION.SDK_INT>=29){
            Uri files=MediaStore.Files.getContentUri("external");
            String[] projection={MediaStore.Files.FileColumns._ID,MediaStore.Files.FileColumns.DISPLAY_NAME,MediaStore.Files.FileColumns.MIME_TYPE};
            String selection=MediaStore.Files.FileColumns.RELATIVE_PATH+"=?";
            String[] args={"Download/Navegador Web Flash Zero/"};
            try(Cursor c=getContentResolver().query(files,projection,selection,args,MediaStore.Files.FileColumns.DATE_ADDED+" DESC")){
                if(c!=null){int idCol=c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID), nameCol=c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME); while(c.moveToNext()){long id=c.getLong(idCol);String name=c.getString(nameCol);Uri uri=ContentUris.withAppendedId(files,id); addRecordingRow(box,name,uri);}}
            }
        }
        if(box.getChildCount()==1){TextView empty=new TextView(this);empty.setText("Nenhuma gravação salva ainda.");empty.setTextColor(darkMode?0xffc9cdd2:0xff555555);empty.setPadding(0,dp(16),0,dp(16));box.addView(empty);}
        new AlertDialog.Builder(this).setView(box).setNegativeButton("Fechar",null).show();
    }
    private void addRecordingRow(LinearLayout box,String name,Uri uri){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
        TextView label=new TextView(this);label.setText(name);label.setTextSize(16);label.setTextColor(darkMode?Color.WHITE:Color.BLACK);label.setPadding(dp(4),dp(12),dp(8),dp(12));
        label.setOnClickListener(v->{long now=SystemClock.elapsedRealtime();if(uri.equals(lastRecordingTapUri)&&now-lastRecordingTapMs<550){lastRecordingTapUri=null;lastRecordingTapMs=0;try{Intent play=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(play);}catch(Exception e){Toast.makeText(this,"Não foi possível abrir a gravação",Toast.LENGTH_SHORT).show();}}else{lastRecordingTapUri=uri;lastRecordingTapMs=now;Toast.makeText(this,"Toque novamente para reproduzir",Toast.LENGTH_SHORT).show();}});row.addView(label,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
        TextView close=new TextView(this);close.setText("✕");close.setTextSize(22);close.setTextColor(darkMode?0xffff8a80:0xffb00020);close.setPadding(dp(14),dp(10),dp(14),dp(10));close.setOnClickListener(v->{try{getContentResolver().delete(uri,null,null);}catch(Exception ignored){}box.removeView(row);});row.addView(close);box.addView(row);
    }
    private void applyChromeTheme(){int bg=darkMode?0xff2b2d31:0xfff7f7fa,fg=darkMode?Color.WHITE:0xff28282b;chrome.setBackgroundColor(bg);for(int i=0;i<chrome.getChildCount();i++){View v=chrome.getChildAt(i);if(v instanceof TextView){((TextView)v).setTextColor(fg);v.setBackgroundColor(bg);}}address.setHintTextColor(darkMode?0xffaeb4bc:0xff6f7278);}
    private void applyVisualFilters(){String mode=darkMode?(blueFilter?"darkblue":"dark"):(blueFilter?"blue":"off");web.evaluateJavascript(DARK_JS.replace("__MODE__",mode),null);}
    private void injectVideoTools(){if(!showVideoTools){web.evaluateJavascript("window.__nwzVideoToolsEnabled=false;if(window.__nwzVideoObserver){try{window.__nwzVideoObserver.disconnect()}catch(e){}window.__nwzVideoObserver=null}document.querySelectorAll('.nwz-video-tools').forEach(e=>e.remove());document.querySelectorAll('video[data-nwz]').forEach(e=>e.removeAttribute('data-nwz'))",null);return;}web.evaluateJavascript("window.__nwzVideoToolsEnabled=true;"+VIDEO_JS,null);}
    private void applyPrivacySettings(){
        if(web==null)return;
        WebSettings ws=web.getSettings();
        ws.setSaveFormData(saveCredentials);
        CookieManager cm=CookieManager.getInstance();
        cm.setAcceptCookie(cookiesEnabled);
        if(Build.VERSION.SDK_INT>=21)cm.setAcceptThirdPartyCookies(web,cookiesEnabled);
        if(!cookiesEnabled)cm.removeAllCookies(null);
        cm.flush();
        if(!historyEnabled)web.clearHistory();
    }

    private void savePrefs(){prefs.edit().putInt("barPosition",barPosition).putBoolean("autoHide",autoHide).putBoolean("darkMode",darkMode).putBoolean("blueFilter",blueFilter).putBoolean("showVideoTools",showVideoTools).putBoolean("backgroundPlayback",backgroundPlayback).putBoolean("saveCredentials",saveCredentials).putBoolean("historyEnabled",historyEnabled).putBoolean("cookiesEnabled",cookiesEnabled).apply();}
    private void hideKeyboard(){try{address.clearFocus();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(address.getWindowToken(),0);}catch(Exception ignored){}}
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}

    private void requestScreenRecording(){MediaProjectionManager pm=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);startActivityForResult(pm.createScreenCaptureIntent(),REQ_CAPTURE);}
    private void stopScreenRecording(){Intent i=new Intent(this,ScreenRecordingService.class).setAction(ScreenRecordingService.ACTION_STOP);startService(i);recording=false;if(plusButton!=null)plusButton.setText("+");web.evaluateJavascript(AREA_CLEAR_JS,null);chrome.setVisibility(View.VISIBLE);getWindow().getDecorView().setSystemUiVisibility(0);recordAreaMode=false;}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req!=REQ_CAPTURE)return;if(result!=RESULT_OK||data==null){web.evaluateJavascript(AREA_CLEAR_JS,null);chrome.setVisibility(View.VISIBLE);getWindow().getDecorView().setSystemUiVisibility(0);recordAreaMode=false;return;}if(recordAreaMode)getWindow().getDecorView().setSystemUiVisibility(5894);Intent i=new Intent(this,ScreenRecordingService.class).setAction(ScreenRecordingService.ACTION_START).putExtra(ScreenRecordingService.EXTRA_RESULT_CODE,result).putExtra(ScreenRecordingService.EXTRA_RESULT_DATA,data);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);recording=true;chrome.setVisibility(View.VISIBLE);if(plusButton!=null)plusButton.setText("■");Toast.makeText(this,recordAreaMode?"Gravação da área iniciada":"Gravação de tela iniciada",Toast.LENGTH_SHORT).show();}

    private void applyBackgroundPlayback(){
        if(web==null)return;
        if(backgroundPlayback){
            web.onResume();
            web.resumeTimers();
            web.evaluateJavascript(BACKGROUND_PLAYBACK_JS,null);
        } else {
            web.evaluateJavascript("window.__nwzBackgroundPlayback=false",null);
            stopService(new Intent(this, BackgroundPlaybackService.class));
        }
    }

    @Override protected void onPause(){
        if(web!=null){
            String u=web.getUrl();
            if(u!=null&&!u.startsWith("data:"))prefs.edit().putString("lastUrl",u).apply();
            if(backgroundPlayback){
                web.resumeTimers();
                web.onResume();
                web.evaluateJavascript(BACKGROUND_PLAYBACK_JS+";setTimeout(()=>document.querySelectorAll('video,audio').forEach(m=>{if(m.dataset.nwzWasPlaying==='1')m.play().catch(()=>{})}),120)",null);
                Intent i=new Intent(this, BackgroundPlaybackService.class);
                if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);
            }else{
                web.onPause();
                web.pauseTimers();
            }
        }
        super.onPause();
    }

    @Override protected void onResume(){
        super.onResume();
        if(web!=null){web.resumeTimers();web.onResume();if(backgroundPlayback)web.evaluateJavascript(BACKGROUND_PLAYBACK_JS,null);}
        stopService(new Intent(this, BackgroundPlaybackService.class));
    }

    @Override protected void onUserLeaveHint(){
        // Do not force Picture-in-Picture. Some Android/MIUI builds recreate/fail the Activity.
        super.onUserLeaveHint();
    }

    @Override protected void onDestroy(){
        stopService(new Intent(this, BackgroundPlaybackService.class));
        if(web!=null)web.destroy();
        super.onDestroy();
    }

    private class BrowserClient extends WebViewClient {
        @Override public boolean shouldOverrideUrlLoading(WebView v,String u){if(u.startsWith("nwzmedia:1")){mediaPlaying=true;return true;}if(u.startsWith("nwzmedia:0")){mediaPlaying=false;return true;}if(u.startsWith("nwzrecordarea:start")){chrome.setVisibility(View.GONE);requestScreenRecording();return true;}if(u.startsWith("nwzrecordarea:cancel")){chrome.setVisibility(View.VISIBLE);getWindow().getDecorView().setSystemUiVisibility(0);recordAreaMode=false;return true;}return false;}
        @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){return intercept(r.getUrl().toString());}
        @Override public WebResourceResponse shouldInterceptRequest(WebView v,String u){return intercept(u);}
        private WebResourceResponse intercept(String u){String l=u.toLowerCase(Locale.ROOT);for(String d:BLOCKED)if(l.contains(d))return new WebResourceResponse("text/html","UTF-8",new java.io.ByteArrayInputStream("<html></html>".getBytes()));return null;}
        @Override public void onPageStarted(WebView v,String u,android.graphics.Bitmap b){if(darkMode){v.setBackgroundColor(0xff121212);v.setVisibility(View.INVISIBLE);}}
        @Override public void onPageFinished(WebView v,String u){address.setText(u);String h=Uri.parse(u).getHost();if(h!=null&&!suggestions.contains(h)){suggestions.add(h);refreshSuggestionAdapter();}applyVisualFilters();v.evaluateJavascript(COSMETIC_JS,null);injectVideoTools();if(backgroundPlayback)v.evaluateJavascript(BACKGROUND_PLAYBACK_JS,null);v.setVisibility(View.VISIBLE);saveCurrentTab();if(u!=null&&!u.startsWith("data:"))prefs.edit().putString("lastUrl",u).apply();}
    }

    private static final String BACKGROUND_PLAYBACK_JS="(function(){try{window.__nwzBackgroundPlayback=true;var __nwzNativeHidden=(function(){try{var d=Object.getOwnPropertyDescriptor(Document.prototype,'hidden');return d&&d.get?function(){return !!d.get.call(document)}:function(){return !!document.hidden}}catch(e){return function(){return !!document.hidden}}})();function state(){let q=document.querySelectorAll('video,audio'),on=false;for(let m of q){if(!m.paused&&!m.ended){on=true;break}}if(window.__nwzMediaPlaying!==on){window.__nwzMediaPlaying=on}}function bind(){document.querySelectorAll('video,audio').forEach(m=>{if(m.dataset.nwzBgBound)return;m.dataset.nwzBgBound='1';m.addEventListener('play',()=>{m.dataset.nwzWasPlaying='1';state()},true);m.addEventListener('pause',()=>{if(!__nwzNativeHidden())m.dataset.nwzWasPlaying='0';state()},true);m.addEventListener('ended',state,true);if(!m.paused&&!m.ended)m.dataset.nwzWasPlaying='1'});state()}function keep(){if(!window.__nwzBackgroundPlayback)return;document.querySelectorAll('video,audio').forEach(m=>{if(m.dataset.nwzWasPlaying==='1'&&m.paused&&!m.ended)m.play().catch(()=>{})});state()}try{Object.defineProperty(document,'hidden',{configurable:true,get:()=>false});Object.defineProperty(document,'visibilityState',{configurable:true,get:()=>'visible'});document.hasFocus=()=>true}catch(e){}bind();if(!window.__nwzBgObserver&&window.MutationObserver){window.__nwzBgObserver=new MutationObserver(bind);window.__nwzBgObserver.observe(document.documentElement||document.body,{childList:true,subtree:true})}if(!window.__nwzBgVisibility){window.__nwzBgVisibility=true;['visibilitychange','pagehide','freeze'].forEach(ev=>document.addEventListener(ev,e=>{if(window.__nwzBackgroundPlayback){try{e.stopImmediatePropagation()}catch(x){}setTimeout(keep,20);setTimeout(keep,180);setTimeout(keep,700)}},true));window.addEventListener('blur',()=>{if(window.__nwzBackgroundPlayback){setTimeout(keep,40);setTimeout(keep,300)}},true)}setInterval(()=>{if(window.__nwzBackgroundPlayback)keep()},1200)}catch(e){}})()";
    private static final String AREA_SELECT_JS="(function(){setTimeout(function(){try{\n var old=document.getElementById('nwz-area-layer');if(old&&old.parentNode)old.parentNode.removeChild(old);
 var vw=Math.max(1,window.innerWidth||document.documentElement.clientWidth||360);
 var vh=Math.max(1,window.innerHeight||document.documentElement.clientHeight||640);
 var minW=60,minH=60;
 var x1=Math.round(vw*.10),x2=Math.round(vw*.90),y1=Math.round(vh*.12),y2=Math.round(vh*.72);
 var host=document.body||document.documentElement;
 var layer=document.createElement('div');layer.id='nwz-area-layer';
 layer.style.cssText='position:fixed;left:0;top:0;width:100vw;height:100vh;z-index:2147483647;pointer-events:none;font-family:sans-serif;overflow:visible;';host.appendChild(layer);
 var box=document.createElement('div');box.id='nwz-area-box';
 box.style.cssText='position:absolute;border:3px solid #18a0fb;background:rgba(24,160,251,.035);box-shadow:0 0 0 9999px rgba(0,0,0,.20),0 0 0 1px rgba(255,255,255,.9) inset;box-sizing:border-box;pointer-events:none;';layer.appendChild(box);
 var info=document.createElement('div');info.style.cssText='position:absolute;left:6px;top:6px;padding:4px 7px;background:rgba(0,0,0,.80);color:#fff;border:1px solid #18a0fb;border-radius:7px;font:700 11px sans-serif;pointer-events:none;';box.appendChild(info);
 var moveXY=document.createElement('div');moveXY.setAttribute('data-handle','move');moveXY.textContent='Mover área';moveXY.style.cssText='position:absolute;right:6px;top:6px;padding:6px 9px;background:#25272a;color:#fff;border:2px solid #18a0fb;border-radius:8px;font:700 11px sans-serif;pointer-events:auto;touch-action:none;';box.appendChild(moveXY);
 function edge(name,label,css){var e=document.createElement('div');e.setAttribute('data-handle',name);e.textContent=label;e.style.cssText='position:absolute;min-width:34px;height:28px;padding:0 7px;display:flex;align-items:center;justify-content:center;background:#1769e0;color:#fff;border:2px solid #fff;border-radius:10px;z-index:8;pointer-events:auto;touch-action:none;font:700 11px sans-serif;box-sizing:border-box;'+css;box.appendChild(e)}\n edge('left','X1','left:-20px;top:50%;transform:translateY(-50%);');edge('right','X2','right:-20px;top:50%;transform:translateY(-50%);');edge('top','Y1','top:-17px;left:50%;transform:translateX(-50%);');edge('bottom','Y2','bottom:-17px;left:50%;transform:translateX(-50%);');
 function corner(pos){var e=document.createElement('div');e.setAttribute('data-handle',pos);e.style.cssText='position:absolute;width:24px;height:24px;background:#fff;border:3px solid #18a0fb;border-radius:50%;z-index:9;pointer-events:auto;touch-action:none;'+(pos.indexOf('l')>=0?'left:-13px;':'right:-13px;')+(pos.indexOf('t')>=0?'top:-13px;':'bottom:-13px;');box.appendChild(e)}\n ['lt','rt','lb','rb'].forEach(corner);
 var controls=document.createElement('div');controls.id='nwz-area-controls';controls.style.cssText='position:fixed;left:6px;right:6px;bottom:8px;display:flex;flex-wrap:wrap;gap:5px;justify-content:center;align-items:center;z-index:2147483647;pointer-events:auto;padding:7px;background:rgba(20,20,22,.90);border-radius:12px;';layer.appendChild(controls);
 function btn(txt,fn,primary){var b=document.createElement('button');b.type='button';b.textContent=txt;b.style.cssText='min-height:36px;padding:7px 8px;border:'+(primary?'0':'1px solid #777')+';border-radius:8px;background:'+(primary?'#1769e0':'#2b2d30')+';color:#fff;font-weight:700;font-size:12px;';b.onclick=function(e){e.preventDefault();e.stopPropagation();fn()};controls.appendChild(b);return b}\n function num(label,get,set){var wrap=document.createElement('label');wrap.style.cssText='display:flex;align-items:center;gap:3px;color:#fff;font:700 11px sans-serif;background:#24262a;border:1px solid #555;border-radius:8px;padding:3px 5px;';var sp=document.createElement('span');sp.textContent=label;var inp=document.createElement('input');inp.type='number';inp.inputMode='numeric';inp.style.cssText='width:56px;height:28px;border:0;border-radius:6px;background:#111;color:#fff;padding:0 5px;font:700 12px sans-serif;';inp.addEventListener('change',function(){var v=parseInt(inp.value,10);if(!isNaN(v)){set(v);draw()}});wrap.appendChild(sp);wrap.appendChild(inp);controls.appendChild(wrap);return inp}\n var ix1=num('X1',function(){return x1},function(v){x1=v});var ix2=num('X2',function(){return x2},function(v){x2=v});var iy1=num('Y1',function(){return y1},function(v){y1=v});var iy2=num('Y2',function(){return y2},function(v){y2=v});
 function clamp(){vw=Math.max(1,window.innerWidth||document.documentElement.clientWidth||vw);vh=Math.max(1,window.innerHeight||document.documentElement.clientHeight||vh);x1=Math.max(0,Math.min(vw-minW,x1));x2=Math.max(x1+minW,Math.min(vw,x2));y1=Math.max(0,Math.min(vh-minH,y1));y2=Math.max(y1+minH,Math.min(vh,y2))}\n function draw(){clamp();box.style.left=Math.round(x1)+'px';box.style.top=Math.round(y1)+'px';box.style.width=Math.round(x2-x1)+'px';box.style.height=Math.round(y2-y1)+'px';info.textContent='X1 '+Math.round(x1)+'  X2 '+Math.round(x2)+'  Y1 '+Math.round(y1)+'  Y2 '+Math.round(y2)+'  '+Math.round(x2-x1)+'×'+Math.round(y2-y1);ix1.value=Math.round(x1);ix2.value=Math.round(x2);iy1.value=Math.round(y1);iy2.value=Math.round(y2)}\n draw();
 btn('X1 ←',function(){x1-=5;draw()});btn('X1 →',function(){x1+=5;draw()});btn('X2 ←',function(){x2-=5;draw()});btn('X2 →',function(){x2+=5;draw()});
 btn('Y1 ↑',function(){y1-=5;draw()});btn('Y1 ↓',function(){y1+=5;draw()});btn('Y2 ↑',function(){y2-=5;draw()});btn('Y2 ↓',function(){y2+=5;draw()});
 btn('Cancelar',function(){if(layer.parentNode)layer.parentNode.removeChild(layer);location.href='nwzrecordarea:cancel'});
 btn('Iniciar gravação',function(){window.__nwzAreaRect={x:Math.round(x1),y:Math.round(y1),w:Math.round(x2-x1),h:Math.round(y2-y1)};if(layer.parentNode)layer.parentNode.removeChild(layer);location.href='nwzrecordarea:start?x='+window.__nwzAreaRect.x+'&y='+window.__nwzAreaRect.y+'&w='+window.__nwzAreaRect.w+'&h='+window.__nwzAreaRect.h},true);
 var mode='',sx=0,sy=0,ox1=0,ox2=0,oy1=0,oy2=0;
 function point(e){var t=(e.touches&&e.touches.length)?e.touches[0]:e;return{x:t.clientX,y:t.clientY}}\n function down(e){var p=point(e);mode=e.currentTarget.getAttribute('data-handle')||'';sx=p.x;sy=p.y;ox1=x1;ox2=x2;oy1=y1;oy2=y2;e.preventDefault();e.stopPropagation()}\n function move(e){if(!mode)return;var p=point(e),dx=p.x-sx,dy=p.y-sy;if(mode==='move'){var ww=ox2-ox1,hh=oy2-oy1;x1=ox1+dx;x2=x1+ww;y1=oy1+dy;y2=y1+hh;if(x1<0){x2-=x1;x1=0}if(x2>vw){x1-=x2-vw;x2=vw}if(y1<0){y2-=y1;y1=0}if(y2>vh){y1-=y2-vh;y2=vh}}else{\n   if(mode==='left'||mode==='lt'||mode==='lb')x1=Math.max(0,Math.min(ox2-minW,ox1+dx));
   if(mode==='right'||mode==='rt'||mode==='rb')x2=Math.min(vw,Math.max(ox1+minW,ox2+dx));
   if(mode==='top'||mode==='lt'||mode==='rt')y1=Math.max(0,Math.min(oy2-minH,oy1+dy));
   if(mode==='bottom'||mode==='lb'||mode==='rb')y2=Math.min(vh,Math.max(oy1+minH,oy2+dy));
 }draw();e.preventDefault();e.stopPropagation()}\n function up(){mode=''}\n var handles=box.querySelectorAll('[data-handle]');for(var i=0;i<handles.length;i++){handles[i].addEventListener('touchstart',down,{passive:false});handles[i].addEventListener('mousedown',down)}\n document.addEventListener('touchmove',move,{passive:false});document.addEventListener('touchend',up,{passive:false});document.addEventListener('mousemove',move);document.addEventListener('mouseup',up);window.addEventListener('resize',draw);
}catch(e){try{var d=document.createElement('div');d.textContent='Falha ao abrir seleção: '+e;d.style.cssText='position:fixed;left:10px;right:10px;top:70px;z-index:2147483647;background:#b00020;color:white;padding:12px;font:14px sans-serif';(document.body||document.documentElement).appendChild(d)}catch(x){}}},80)})()";
    private static final String AREA_CLEAR_JS="(function(){try{var e=document.getElementById('nwz-area-layer');if(e&&e.parentNode)e.parentNode.removeChild(e)}catch(x){}})()"\n    private static final String COSMETIC_JS="(function(){const q=['iframe[src*=doubleclick]','ins.adsbygoogle','[id*=google_ads]','[class*=ad-slot]','[data-ad-slot]','[data-google-query-id]'];function s(){q.forEach(x=>document.querySelectorAll(x).forEach(e=>{let p=e;for(let i=0;i<5&&p;i++,p=p.parentElement){if((p.innerText||'').trim()===''&&!p.querySelector('img,video,form')){p.style.setProperty('display','none','important');break}}}))}s();new MutationObserver(s).observe(document.documentElement,{subtree:true,childList:true})})()";
    private static final String DARK_JS="(function(){let m='__MODE__',d=m.indexOf('dark')===0,b=m.indexOf('blue')>0||m==='blue';let st=document.getElementById('nwz-theme');if(!st){st=document.createElement('style');st.id='nwz-theme';document.head.appendChild(st)}st.textContent=d?'html,body{background:#101214!important;color:#eef0f2!important;color-scheme:dark!important}body *{border-color:#3d4147!important}input,textarea,select{background:#26292d!important;color:#eef0f2!important}':'';document.documentElement.style.filter=b?'sepia(.16) saturate(.9) brightness(.96)':'';if(d){document.querySelectorAll('body,body *').forEach(e=>{if(/^(IMG|VIDEO|SVG|CANVAS|PICTURE|IFRAME)$/.test(e.tagName))return;let c=getComputedStyle(e),bg=c.backgroundColor.match(/\\d+/g);if(bg&&bg.length>=3){let L=(+bg[0]+ +bg[1]+ +bg[2])/3;if(L>205)e.style.setProperty('background-color','#181a1b','important')}let fg=c.color.match(/\\d+/g);if(fg&&fg.length>=3){let L=(+fg[0]+ +fg[1]+ +fg[2])/3;if(L<170)e.style.setProperty('color','#eef0f2','important')}})}})()";
    private static final String VIDEO_JS="(function(){window.__nwzVideoToolsEnabled=true;function attach(v){if(!window.__nwzVideoToolsEnabled||v.dataset.nwz)return;v.dataset.nwz=1;let w=document.createElement('div');w.className='nwz-video-tools';w.style='display:flex;gap:8px;flex-wrap:wrap;margin:8px 0 14px;position:relative;clear:both;z-index:2';let b=document.createElement('button');b.textContent='↓ Salvar vídeo';b.onclick=()=>{let u=v.currentSrc||v.src;if(u&&/^https?:/.test(u))location.href=u};w.appendChild(b);let a=document.createElement('button');a.textContent='Auditar proteção';a.onclick=()=>alert(v.mediaKeys?'EME/DRM detectado':'EME/DRM não detectado');w.appendChild(a);let p=v.parentElement;if(p&&p.parentElement)p.parentElement.insertBefore(w,p.nextSibling)}function scan(){if(!window.__nwzVideoToolsEnabled)return;document.querySelectorAll('video').forEach(attach)}scan();if(!window.__nwzVideoObserver&&window.MutationObserver){window.__nwzVideoObserver=new MutationObserver(scan);window.__nwzVideoObserver.observe(document.documentElement||document.body,{childList:true,subtree:true})}})()";
}
