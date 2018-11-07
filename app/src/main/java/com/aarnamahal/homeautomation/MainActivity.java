package com.aarnamahal.homeautomation;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.santalu.maskedittext.MaskEditText;
import com.squareup.picasso.Picasso;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static final long DISCONNECT_TIMEOUT = 30000; //300000; // 5 min = 5 * 60 * 1000 ms
    public static final long CHANGE_SCR_SAVER = 30000; //300000; // 5 min = 5 * 60 * 1000 ms
    private Handler disconnectHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            // todo
            return true;
        }
    });

    private Runnable disconnectCallback = new Runnable() {
        @Override
        public void run() {
            // Perform any required operation on disconnect
            //tvDebug.setText("idle now");
            if(tbScrSvr.isChecked()){
                changeImgScrSvr();
                if (!dialogScrSvr.isShowing()) {
                    dialogScrSvr.show();
                }
            }
        }
    };
    public void changeImgScrSvr(){
        imgNo++;
        if (imgNo>=imgMaxNo) imgNo=0;
        //Picasso.get().load(imgFiles[imgNo]).resize(1152, 864).rotate(0).into(imageScrSvr);
        //imageScrSvr.setImageResource("");
        new ImageLoadTask(imgFiles[imgNo], imageScrSvr).execute();
        dialogScrSvr.setContentView(imageScrSvr);

        disconnectHandler.postDelayed(disconnectCallback, CHANGE_SCR_SAVER);
    }
    public void resetDisconnectTimer(){
        //tvDebug.setText("Back now");
        disconnectHandler.removeCallbacks(disconnectCallback);
        disconnectHandler.postDelayed(disconnectCallback, DISCONNECT_TIMEOUT);
    }

    public void stopDisconnectTimer(){
        disconnectHandler.removeCallbacks(disconnectCallback);
    }

    @Override
    public void onUserInteraction(){
        resetDisconnectTimer();
    }

    @Override
    public void onResume() {
        super.onResume();
        resetDisconnectTimer();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopDisconnectTimer();
    }


    private final int REQ_CODE_SPEECH_INPUT = 100;

    public static String device ;

    String[] imgUrl;
    Dialog dialogScrSvr;
    ImageView imageScrSvr;
    int imgNo,imgMaxNo;
    public static String[] imgFiles;
    public static int timerInc = 0;

    private MqttAndroidClient client;
    public static Switch swMBACAuto;
    public static ToggleButton tbMBAC, tbGeyser, tbLift, tbMain, tbBed, tbAlarm, tbScrSvr;
    public static TextView tvCurrWtr, tvFd1Wtr, tvFd2Wtr, tvAlerts, tvLogs, tvCT, tvSolarMain, tvSolarBed, tvCurrTm, tvDebug;
    public static ImageView ivCurrWtr, ivFd1Wtr,ivFd2Wtr;
    public static Spinner spIntercom;
    public static String sIntercomSelected;
    public static MaskEditText autoStTm, autoEdTm;
    public static Button btnMainDoor;
    private LinearLayout llMainButtons;
    public static TextToSpeech tts;
    public static String sAlert;
    private WebView wvContent;
    //for refreshing screen.
    private int iAlertSpeakTime = 0;
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            refreshNow(); //refresh screen for statuses and logs
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        device = Build.MANUFACTURER.concat(Build.MODEL);
        device = device.replace(" ","");
        device = device.replace("-","");

        initAllcomponents();
        initMQTT();
        initIntercomSpinner();
        initScreenSaver();

        InitButtonSizesAsPerDevices();

        InitTextToSpeech();
        initAnimationForAlarms();

        initAutoScheduleTimes();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);// hide keyboard for better visibility.

        timerHandler.postDelayed(timerRunnable, 2000);
    }
    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void initAnimationForAlarms(){
        Animation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(500); //You can manage the blinking time with this parameter
        anim.setStartOffset(20);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        tvAlerts.startAnimation(anim);
        tvAlerts.setTextColor(getResources().getColor(R.color.colorAccent));
    }

    private void initAllcomponents(){

        tvCurrWtr = (TextView) findViewById(R.id.tvCurrWtr);
        tvFd1Wtr = (TextView) findViewById(R.id.tvFd1Wtr);
        tvFd2Wtr = (TextView) findViewById(R.id.tvFd2Wtr);
        ivCurrWtr = (ImageView) findViewById(R.id.imCurr);
        ivFd1Wtr = (ImageView) findViewById(R.id.imFd1);
        ivFd2Wtr = (ImageView) findViewById(R.id.imFd2);

        autoStTm = (MaskEditText) findViewById(R.id.etStTime);
        autoEdTm = (MaskEditText) findViewById(R.id.etEdTime);

        btnMainDoor = (Button) findViewById(R.id.btnMainDoor);
        tbMBAC =  findViewById(R.id.tbMBAC);
        tbLift =  findViewById(R.id.tbLift);
        tbGeyser =  findViewById(R.id.tbGeyser);
        tbMain = findViewById(R.id.tbMain);
        tbBed = findViewById(R.id.tbBed);
        tbAlarm = findViewById(R.id.tbAlarm);
        tbScrSvr = findViewById(R.id.tbScrSvr);

        swMBACAuto = (Switch) findViewById(R.id.swMBACauto);
        tvAlerts = (TextView) findViewById(R.id.tvAlerts);
        llMainButtons = (LinearLayout) findViewById(R.id.llMainButtons);
        tvLogs = (TextView) findViewById(R.id.tvLogs);
        tvCT = (TextView) findViewById(R.id.tvCT);
        tvSolarMain = (TextView) findViewById(R.id.tvSolarMain);
        tvSolarBed = (TextView) findViewById(R.id.tvSolarBed);
        tvCurrTm = (TextView) findViewById(R.id.tvCurrTm);
    }


    public void InitButtonSizesAsPerDevices(){

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        int newSize = 20;
        tvDebug = (TextView)findViewById(R.id.tvDebug);
        //if(BuildConfig.DEBUG)
        //    tvDebug.setText("Ht:"+height+"  Wdt:"+width +"  den:"+displayMetrics.density+"  denDpi:"+displayMetrics.densityDpi+"  scDen:"+displayMetrics.scaledDensity +
        //            "  xdpi:"+displayMetrics.xdpi +"  ydpi:"+displayMetrics.ydpi+"  DM:"+displayMetrics.toString()+" swMBAC:" + tbMBAC.getTextSize());
        //else
            //tvDebug.setVisibility(View.INVISIBLE);
        if (device.equals(getResources().getStringArray(R.array.Intercoms)[3]) ||
                device.equals(getResources().getStringArray(R.array.Intercoms)[1])) {
            tbMBAC.setTextSize(newSize);
            tbMBAC.setPadding(newSize/2,newSize/2,newSize/2,newSize/2);
            tbGeyser.setTextSize(newSize);
            tbGeyser.setPadding(newSize/2,newSize/2,newSize/2,newSize/2);
            tbLift.setTextSize(newSize);
            tbLift.setPadding(newSize/2,newSize/2,newSize/2,newSize/2);
            swMBACAuto.setTextSize(newSize);

            btnMainDoor.setTextSize(newSize);
            btnMainDoor.setPadding(newSize,newSize,newSize,newSize);
            tvAlerts.setTextSize(newSize);
            tvLogs.setTextSize(newSize/2);

            ViewGroup.LayoutParams tvparams = tvLogs.getLayoutParams();
            if (device.equals(getResources().getStringArray(R.array.Intercoms)[3])){
                tvparams.width = getResources().getDimensionPixelSize(R.dimen.tvLogsWdPhone);
                tvLogs.setLines(30);
                tvCT.setLines(15);
            }
            else{//Mi Max 2
                tvparams.width = getResources().getDimensionPixelSize(R.dimen.tvLogsWdOfficeTab);
                tvLogs.setLines(32);
            }
            tvLogs.setLayoutParams(tvparams);
            tvCT.setTextSize(newSize/2);
            tvSolarMain.setTextSize(newSize/2);
            tvSolarBed.setTextSize(newSize/2);
            //tvCT.setLines(7);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) llMainButtons.getLayoutParams();
            params.topMargin =newSize;
            llMainButtons.setLayoutParams(params);

        }
        else if (device.equals(getResources().getStringArray(R.array.Intercoms)[2])){// Samsung hall test

            tbMBAC.setTextSize(getResources().getDimensionPixelSize(R.dimen.tbTextSize));
            tbGeyser.setTextSize(getResources().getDimensionPixelSize(R.dimen.tbTextSize));
            tbLift.setTextSize(getResources().getDimensionPixelSize(R.dimen.tbTextSize));

        }
    }
    private void initScreenSaver(){

        String sPath = "http://192.168.0.11:81/getFiles.php";
        //sPath = "http://192.168.0.6/bookings/listBookingsv2.php";
        backGroundActivity bA = new backGroundActivity(MainActivity.this);
        bA.execute("getImgFiles");

        //File dir = new File(sPath);
        //File[] files = dir.listFiles();
        //tvDebug.setText(files.length + files[0].getName());
/*
        try {
            URL url = new URL(sPath);
            URLConnection conn = url.openConnection();
            InputStream inputStream = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line = null;
            System.out.println("--- START ---");
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("--- END

            inputStream.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }*/
        imageScrSvr = new ImageView(this);
        imgMaxNo = 7;
        imgUrl = new String[imgMaxNo];
        imgNo = 0;
        imgUrl[0]= "http://192.168.0.6/slsw/sld/PictureGangaFam1.jpg";//http://www.uniwallpaper.com/static/images/6663822_orig.jpg";
        imgUrl[1] = "http://www.uniwallpaper.com/static/images/2016-bb-chevrolet-cars-seo-masthead-1480x551.jpg";
        imgUrl[2] = "http://www.uniwallpaper.com/static/images/Sydney-harbour-bei-nacht-wallpaper_1GCx7Bu.JPG";
        imgUrl[3] = "http://www.uniwallpaper.com/static/images/Spring-Colours-Wallpaper_BeOup6e.jpg";
        imgUrl[4] = "http://www.uniwallpaper.com/static/images/Autumn_Wallpaper_by_emats_R3bf4pr.jpg";
        imgUrl[5] = "http://www.uniwallpaper.com/static/images/road_along-wallpaper-1366x768_RjeqmhA.jpg";
        imgUrl[6] = "http://www.uniwallpaper.com/static/images/1712173_DxY27jV.jpg";
        Picasso.get().load(imgUrl[imgNo]).into(imageScrSvr);
        dialogScrSvr=new Dialog(this,android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialogScrSvr.setContentView(imageScrSvr);
        imageScrSvr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogScrSvr.dismiss();
                resetDisconnectTimer();
                refreshNow();
            }
        });
    }

    private void initIntercomSpinner(){
        ArrayAdapter<CharSequence> adapterStatus;
        spIntercom = (Spinner) findViewById(R.id.spIntNo);
        adapterStatus = ArrayAdapter.createFromResource(this,R.array.IntercomsDisplay,android.R.layout.simple_spinner_item);
        adapterStatus.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spIntercom.setAdapter(adapterStatus);
        spIntercom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sIntercomSelected = getResources().getStringArray(R.array.Intercoms)[position];//.getItemAtPosition(position).toString();
                tvDebug.setText(sIntercomSelected);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    private void initAutoScheduleTimes(){
        autoStTm.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i==EditorInfo.IME_ACTION_DONE){
                    Toast.makeText(MainActivity.this, "Staring to update sch", Toast.LENGTH_LONG).show();
                    //var url = "setGPIOonoffTime.php?GPIOno="+24+"&OnOff="+OffTime+"&Time='"+06:40+"'";
                    backGroundActivity bA = new backGroundActivity(MainActivity.this);
                    //bA.execute("setGPIOschedule", "24", "OnTime",autoStTm.getText().toString());//  switch on MBAC time
                    bA.execute("execUrlfull", "http://192/168.0.6/setGPIOonoffTime.php?GPIOno=24&OnOff=OnTime&Time='"+autoStTm.getText().toString()+"'");

                    Toast.makeText(MainActivity.this, "DONE", Toast.LENGTH_LONG).show();
                    //Toast.makeText(MainActivity.this, autoStTm.getRawText(), Toast.LENGTH_LONG).show();
                    //Toast.makeText(MainActivity.this, autoStTm.getText(), Toast.LENGTH_LONG).show();
                    return true;
                }
                return false;
            }
        });
        autoEdTm.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i==EditorInfo.IME_ACTION_DONE){
                    Toast.makeText(MainActivity.this, "Staring to update sch", Toast.LENGTH_LONG).show();
                    //var url = "setGPIOonoffTime.php?GPIOno="+24+"&OnOff="+OffTime+"&Time='"+06:40+"'";
                    backGroundActivity bA = new backGroundActivity(MainActivity.this);
                    //bA.execute("setGPIOschedule", "24", "OffTime",autoStTm.getText().toString());//switch off MBAC time
                    bA.execute("execUrlfull", "http://192/168.0.6/setGPIOonoffTime.php?GPIOno=24&OnOff=OffTime&Time='"+autoEdTm.getText().toString()+"'");
                    Toast.makeText(MainActivity.this, "DONE", Toast.LENGTH_LONG).show();
                    return true;
                }
                return false;
            }
        });
    }

    private void InitTextToSpeech(){
        tts = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS){
                    tts.setLanguage(Locale.US);
                }
                else{
                    Toast.makeText(MainActivity.this, "Feature is not supported in your device.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void ChangeBackGroundColor(){

        int colorFrom = getResources().getColor(R.color.colorLightBlue);
        int colorTo = getResources().getColor(R.color.colorLightYellow);
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(2500); // milliseconds
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                //llMainL.setBackgroundColor((int) animator.getAnimatedValue());
            }

        });
        colorAnimation.start();
    }

    private void refreshNow(){

        imgMaxNo = imgFiles.length;

        tvCurrTm.setText(new SimpleDateFormat("dd HH:mm").format(Calendar.getInstance().getTime()).toString());
        backGroundActivity bASS = new backGroundActivity(MainActivity.this);
        bASS.execute("getSwitchStatus");
        backGroundActivity bAwtr = new backGroundActivity(MainActivity.this);
        bAwtr.execute("getWeather");
        backGroundActivity bAlogs = new backGroundActivity(MainActivity.this);
        bAlogs.execute("getLogs");
        backGroundActivity bAkWh = new backGroundActivity(MainActivity.this);
        bAkWh.execute("showkWh");
        backGroundActivity bASolar = new backGroundActivity(MainActivity.this);
        bASolar.execute("showSolar");

        if (sAlert!= null){//Some alerts have popped up.. Need to announce it every 30 secs.
            if (sAlert.length()>0 && tbAlarm.isChecked()){
                if (iAlertSpeakTime > 5){//30 secs up.
                    MainActivity.tts.speak(sAlert, TextToSpeech.QUEUE_FLUSH, null);
                    iAlertSpeakTime = 0;
                }
                else if (iAlertSpeakTime==0)//first
                    MainActivity.tts.speak(sAlert, TextToSpeech.QUEUE_FLUSH, null);
                iAlertSpeakTime++;
            }
            if (sAlert.length()==0)
                iAlertSpeakTime = 0;
        }
        else
            iAlertSpeakTime = 0;
        timerHandler.postDelayed(timerRunnable, 5000);
    }
    public void OnIntercom (View view){
        //Publish("/intercom/samsungGT-5113/Speak", "can you come down NOW?");
        promptSpeechInput();

    }
    public void OnRefresh (View view){
        refreshNow();
    }
    public void OnGeyser (View view){
        if (tbGeyser.isChecked()){
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            //Yes button clicked
                            backGroundActivity bA = new backGroundActivity(MainActivity.this);
                            bA.execute("execUrl","geyserOnAndSchOff.php?OffAfter=20");//  switch on  Geyser and schedule off after 20 mins
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Have the Geyser Valves been Changed?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
        } else {
            backGroundActivity bA = new backGroundActivity(MainActivity.this);
            bA.execute("gpio", "26", "1");//  switch off  Geyser
        }
    }
    public void OnLift (View view){
        if (tbLift.isChecked()){
            Publish("/home/liftOnCmd", "1"); // switch on Lift manually
        } else {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            //Yes button clicked
                            Publish("/home/liftOnCmd", "0"); // switch off Lift manually
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Are you sure you want to Turn off Lift?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
        }
    }
    public void OnAutoMBAC (View view){
        String sDisabledFlag = swMBACAuto.isChecked() ? "0":"1";

        backGroundActivity bA = new backGroundActivity(MainActivity.this);
        bA.execute("execUrl", "setGPIOonoff.php?GPIOno=24&OffFlag="+sDisabledFlag);

    }
    public void OnMain (View view){
        backGroundActivity bA = new backGroundActivity(MainActivity.this);
        if (tbMain.isChecked()){
            bA.execute("Inv","mainInv:On");// switch on Main Inverter
        } else {
            bA.execute("Inv","mainInv:Off");// switch off Main Inverter
        }
    }
    public void OnBed (View view){
        backGroundActivity bA = new backGroundActivity(MainActivity.this);
        if (tbBed.isChecked()){
            bA.execute("Inv","BedInv:On");// switch on Bed Inverter
        } else {
            bA.execute("Inv","BedInv:Off");// switch off Bed Inverter
        }
    }
    public void OnMBAC (View view){
        backGroundActivity bA = new backGroundActivity(MainActivity.this);
        if (tbMBAC.isChecked()){
            bA.execute("gpio", "19", "1");// switch on MBAC
        } else {
            bA.execute("gpio", "19", "0");// switch off  MBAC
        }

    }
    public void OnOpenMainDoor (View view){
        final Animation animBlink = AnimationUtils.loadAnimation(this, R.anim.blink);
        view.startAnimation(animBlink);
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        Publish("/home/gndMainDoor/OpenCmd", "1");
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }
    private void initMQTT(){
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), "tcp://210.18.139.72:1883",
                clientId);

        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    //Log.d(TAG, "onSuccess");
                    String scsD = "/intercom/"+device+"/Speak";
                    //Toast.makeText(MainActivity.this, "OnSuccess:"+scsD, Toast.LENGTH_LONG).show();
                    //tvDebug.setText(scsD);
                    Subscribe(scsD);
                    //Subscribe("/intercom/samsungGT_5113/Speak");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Toast.makeText(MainActivity.this, "OnFailure", Toast.LENGTH_LONG).show();

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String Msg=new String (message.getPayload());
                tvDebug.setText(Msg);
                tts.speak(Msg, TextToSpeech.QUEUE_FLUSH, null);

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }
    public void Publish (String topic, String payload) {

        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }
    public void Subscribe(String topic){
        try {
            client.subscribe(topic, 0);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Speech prompt");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),"Speech not supported",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Receiving speech input
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String sRecognSpeech = result.get(0);
                    //Publish("/intercom/samsungGT-5113/Speak", "Kitchen door opening");
                    //tvDebug.setText(sIntercomSelected);
                    Publish("/intercom/"+sIntercomSelected +"/Speak", sRecognSpeech);//"can you come down NOW?");
                    //tvDebug.setText(sRecognSpeech);
                    refreshNow();
                }
                break;
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}

