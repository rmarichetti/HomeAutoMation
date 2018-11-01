package com.aarnamahal.homeautomation;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.santalu.maskedittext.MaskEditText;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    //private ImageView imView;
    public static final long DISCONNECT_TIMEOUT = 5000; //300000; // 5 min = 5 * 60 * 1000 ms


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
            //imView.setVisibility(View.VISIBLE);
            tvDebug.setText("idle now");
        }
    };

    public void resetDisconnectTimer(){
        disconnectHandler.removeCallbacks(disconnectCallback);
        disconnectHandler.postDelayed(disconnectCallback, DISCONNECT_TIMEOUT);
    }

    public void stopDisconnectTimer(){
        disconnectHandler.removeCallbacks(disconnectCallback);
    }

    @Override
    public void onUserInteraction(){
        resetDisconnectTimer();
        tvDebug.setText("Back now");
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

    public static String device ;
    private MqttAndroidClient client;
    public static Switch swMBACAuto;
    public static ToggleButton tbMBAC, tbGeyser, tbLift, tbMain, tbBed, tbAlarm;
    public static TextView tvCurrWtr, tvFd1Wtr, tvFd2Wtr, tvAlerts;
    public static ImageView ivCurrWtr, ivFd1Wtr,ivFd2Wtr;
    public static MaskEditText autoStTm, autoEdTm;
    public static TextView tvLogs, tvCT, tvSolarMain, tvSolarBed;
    private TextView tvDebug;
    private Button btnMainDoor;
    private LinearLayout llMainButtons;
    public static TextToSpeech tts;
    public static String sAlert;
    private WebView wvContent;
    //runs without a timer by reposting this handler at the end of the runnable
    private int iAlertSpeakTime = 0;
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            timerHandler.postDelayed(this, 5000);
            refreshNow();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        device = Build.MANUFACTURER.concat(Build.MODEL);
        device = device.replace(" ","_");
        InitMQTT();

        btnMainDoor = (Button) findViewById(R.id.btnMainDoor);
        tbMBAC = (ToggleButton) findViewById(R.id.tbMBAC);
        tbLift = (ToggleButton) findViewById(R.id.tbLift);
        tbGeyser = (ToggleButton) findViewById(R.id.tbGeyser);
        tbMain = (ToggleButton) findViewById(R.id.tbMain);
        tbBed = (ToggleButton) findViewById(R.id.tbBed);
        tbAlarm = (ToggleButton) findViewById(R.id.tbAlarm);
        swMBACAuto = (Switch) findViewById(R.id.swMBACauto);
        tvAlerts = (TextView) findViewById(R.id.tvAlerts);
        llMainButtons = (LinearLayout) findViewById(R.id.llMainButtons);
        tvLogs = (TextView) findViewById(R.id.tvLogs);
        tvCT = (TextView) findViewById(R.id.tvCT);
        tvSolarMain = (TextView) findViewById(R.id.tvSolarMain);
        tvSolarBed = (TextView) findViewById(R.id.tvSolarBed);

        //imView = (ImageView) findViewById(R.id.imBkg);
        InitButtonSizesAsPerDevices();

        InitTextToSpeech();

        tvCurrWtr = (TextView) findViewById(R.id.tvCurrWtr);
        tvFd1Wtr = (TextView) findViewById(R.id.tvFd1Wtr);
        tvFd2Wtr = (TextView) findViewById(R.id.tvFd2Wtr);
        ivCurrWtr = (ImageView) findViewById(R.id.imCurr);
        ivFd1Wtr = (ImageView) findViewById(R.id.imFd1);
        ivFd2Wtr = (ImageView) findViewById(R.id.imFd2);

        Animation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(500); //You can manage the blinking time with this parameter
        anim.setStartOffset(20);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        //anim.setBackgroundColor(getResources().getColor(R.color.colorRed));
        tvAlerts.startAnimation(anim);
        tvAlerts.setTextColor(getResources().getColor(R.color.colorAccent));

        autoStTm = (MaskEditText) findViewById(R.id.etStTime);
        autoEdTm = (MaskEditText) findViewById(R.id.etEdTime);

        autoStTm.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i==EditorInfo.IME_ACTION_DONE){
                    Toast.makeText(MainActivity.this, "Staring to update sch", Toast.LENGTH_LONG).show();
                    //var url = "setGPIOonoffTime.php?GPIOno="+24+"&OnOff="+OffTime+"&Time='"+06:40+"'";
                    backGroundActivity bA = new backGroundActivity(MainActivity.this);
                    bA.execute("setGPIOschedule", "24", "OnTime",autoStTm.getText().toString());//  switch on  Geyser
                    Toast.makeText(MainActivity.this, "DONE", Toast.LENGTH_LONG).show();
                    Toast.makeText(MainActivity.this, autoStTm.getRawText(), Toast.LENGTH_LONG).show();
                    Toast.makeText(MainActivity.this, autoStTm.getText(), Toast.LENGTH_LONG).show();
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
                    bA.execute("setGPIOschedule", "24", "OnTime",autoStTm.getText().toString());//  switch on  Geyser
                    Toast.makeText(MainActivity.this, "DONE", Toast.LENGTH_LONG).show();
                    Toast.makeText(MainActivity.this, autoStTm.getRawText(), Toast.LENGTH_LONG).show();
                    Toast.makeText(MainActivity.this, autoStTm.getText(), Toast.LENGTH_LONG).show();
                    return true;
                }
                return false;
            }
        });
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);// hide keyboard for better visibility.

        timerHandler.postDelayed(timerRunnable, 2000);
    }
    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
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
        if (height == 552 || height == 1080) {
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
            if (height == 1080) {
                tvparams.width = getResources().getDimensionPixelSize(R.dimen.tvLogsWdPhone);
                tvLogs.setLines(10);
                tvCT.setLines(10);
            }
            else{
                tvparams.width = getResources().getDimensionPixelSize(R.dimen.tvLogsWdOfficeTab);
                tvLogs.setLines(32);
            }
            tvLogs.setLayoutParams(tvparams);
            tvCT.setTextSize(newSize/2);
            tvSolarMain.setTextSize(newSize/2);
            tvSolarBed.setTextSize(newSize/2);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) llMainButtons.getLayoutParams();
            params.topMargin =newSize;
            llMainButtons.setLayoutParams(params);

        }
        else if (height == 800){// Samsung hall test

            tbMBAC.setTextSize(getResources().getDimensionPixelSize(R.dimen.tbTextSize));
            tbGeyser.setTextSize(getResources().getDimensionPixelSize(R.dimen.tbTextSize));
            tbLift.setTextSize(getResources().getDimensionPixelSize(R.dimen.tbTextSize));

        }
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
                            bA.execute("gpio", "26", "0");//  switch on  Geyser
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
        backGroundActivity bA = new backGroundActivity(MainActivity.this);
/*
 var url = "setGPIOonoff.php?GPIOno="+GPIOno+"&OffFlag="+EnableFlag.toString();

        if (tbMBAC.isChecked()){
            bA.execute("gpio", "19", "1");// switch on MBAC
        } else {
            bA.execute("gpio", "19", "0");// switch off  MBAC
        }
*/
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
    private void InitMQTT(){
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), "tcp://192.168.0.6:1883",
                clientId);

        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    //Log.d(TAG, "onSuccess");
                    //Toast.makeText(MainActivity.this, "OnSuccess", Toast.LENGTH_LONG).show();
                    Subscribe("/intercom/"+device+"/Speak");
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

}

