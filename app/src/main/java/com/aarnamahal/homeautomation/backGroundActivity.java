package com.aarnamahal.homeautomation;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.widget.ImageView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class backGroundActivity  extends AsyncTask<String,Void,String> {
    Context context;
    String sResult;
    AlertDialog alertDialog;
    String sType;

    String[] fcst;
    String[] fcstImgUrl;

    backGroundActivity (Context ctx){
        context = ctx;
    }

    @Override
    protected void onCancelled(String s) {
        super.onCancelled(s);
    }

    @Override
    protected String doInBackground(String... params){
        sType = params[0];
        String sURL="";
        String post_data="";
        if(sType.equals("getWeather")) {
            fcst = new String[3];
            fcstImgUrl = new String[3];
            int getUntil = 1;
            String forecastFull;
            try
            {
                URL url = new URL("http://rss.accuweather.com/rss/liveweather_rss.asp?metric=1&locCode=VOMM");
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(false);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(getInputStream(url), "UTF_8");

                String testTitle="", testDesc = "";
                boolean insideItem = false;
                int fcstNo = 0;
                // Returns the type of current event: START_TAG, END_TAG, START_DOCUMENT, END_DOCUMENT etc..
                int eventType = xpp.getEventType(); //loop control variable
                while (eventType != XmlPullParser.END_DOCUMENT)
                {
                    //if we are at a START_TAG (opening tag)
                    if (eventType == XmlPullParser.START_TAG)
                    {
                        //if the tag is called "item"
                        if (xpp.getName().equalsIgnoreCase("item"))
                        {
                            insideItem = true;
                        }
                        //if the tag is called "title"
                        else if (xpp.getName().equalsIgnoreCase("title"))
                        {
                            if (insideItem)
                            {
                                // extract the text between <title> and </title>
                                testTitle = testTitle + xpp.nextText();
                            }
                        }
                        //if the tag is called "link"
                        else if (xpp.getName().equalsIgnoreCase("description"))
                        {
                            if (insideItem)
                            {

                                if (fcstNo<3){
                                    forecastFull = xpp.nextText();
                                    fcst[fcstNo] = forecastFull.substring(0,forecastFull.indexOf("<"));

                                    //extract the image from it.
                                    if (fcstNo> 0) {
                                        getUntil = 2;
                                        fcst[fcstNo]=fcst[fcstNo].replace("High:", "");
                                        fcst[fcstNo]=fcst[fcstNo].replace("Low:", "");
                                        fcst[fcstNo]=fcst[fcstNo].replace(" C", "°C");
                                        if (fcstNo==1)
                                            fcst[fcstNo]="   Tomm:"+ fcst[fcstNo];
                                        else if (fcstNo==2)
                                            fcst[fcstNo]="   DayAfter:"+ fcst[fcstNo];

                                    }
                                    else {
                                        fcst[fcstNo]="Today:"+fcst[fcstNo].replace(" °C", "°C");
                                    }
                                    fcstImgUrl[fcstNo] =forecastFull.substring(forecastFull.indexOf("img src=")+9,forecastFull.indexOf(">")-getUntil);

                                }
                                fcstNo++;
                            }
                        }
                    }
                    //if we are at an END_TAG and the END_TAG is called "item"
                    else if (eventType == XmlPullParser.END_TAG && xpp.getName().equalsIgnoreCase("item"))
                    {
                        insideItem = false;
                    }

                    eventType = xpp.next(); //move to next element
                }


            }
            catch (MalformedURLException e)
            {
                e.printStackTrace();
            }
            catch (XmlPullParserException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        else{
            try {
                if(sType.equals("gpio")){
                    sURL = "http://210.18.139.72/AppHomeGPIOset.php";
                    String sGpioNo = params[1];
                    String sOnF = params[2];
                    post_data = URLEncoder.encode("gpio","UTF-8")+"="+URLEncoder.encode(sGpioNo,"UTF-8")+"&"
                            +URLEncoder.encode("on","UTF-8")+"="+URLEncoder.encode(sOnF,"UTF-8");
                }
                else if(sType.equals("getSwitchStatus")) {
                    sURL ="http://210.18.139.72/getSwitchStatuses.php";
                }
                else if(sType.equals("showkWh")) {
                    sURL ="http://210.18.139.72/showkWh.php?app=1";
                }
                else if(sType.equals("Inv")) {
                    String sInvNameAndOnF = params[1];
                    switch (sInvNameAndOnF){
                        case "mainInv:On":
                            sURL ="http://192.168.0.7/epsolar/mInvOn.php";
                            break;
                        case "mainInv:Off":
                            sURL ="http://192.168.0.7/epsolar/mInvOff.php";
                            break;
                        case "BedInv:On":
                            sURL ="http://210.18.139.72/epsolar/mInvOn.php";
                            break;
                        case "BedInv:Off":
                            sURL ="http://210.18.139.72/epsolar/mInvOff.php";
                            break;
                    }
                }
                else if(sType.equals("showSolar")) {
                    sURL ="http://210.18.139.72/showSolarApp.php";
                }
                else if(sType.equals("getLogs")) {
                    sURL ="http://210.18.139.72/showLogApp.php";
                }
                else if(sType.equals("setGPIOschedule")) {
                    sURL ="http://192.168.0.6/setGPIOonoffTime.php";
                    String sGpioNo = params[1];
                    String sOnOrOffstring = params[2];//"OffTime" or "OnTime"
                    String sTime = params[3];
                    post_data = URLEncoder.encode("gpio","UTF-8")+"="+URLEncoder.encode(sGpioNo,"UTF-8")+"&"
                            +URLEncoder.encode("on","UTF-8")+"="+URLEncoder.encode(sOnOrOffstring,"UTF-8")+"&"
                            +URLEncoder.encode("on","UTF-8")+"="+URLEncoder.encode(sTime,"UTF-8");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                URL url = new URL(sURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);
                OutputStream outputStream= httpURLConnection.getOutputStream();
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream,"UTF-8"));
                bufferedWriter.write(post_data);
                bufferedWriter.flush();
                bufferedWriter.close();
                outputStream.close();

                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream,"iso-8859-1"));
                sResult="";
                String sLine="";
                while ((sLine = bufferedReader.readLine())!=null){
                    sResult += sLine;
                }
                bufferedReader.close();
                inputStream.close();
                httpURLConnection.disconnect();
                return sResult;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        return null;
    }
    public InputStream getInputStream(URL url)
    {
        try
        {
            //openConnection() returns instance that represents a connection to the remote object referred to by the URL
            //getInputStream() returns a stream that reads from the open connection
            return url.openConnection().getInputStream();
        }
        catch (IOException e)
        {
            return null;
        }
    }
    @Override
    protected void onPreExecute() {
        alertDialog  = new AlertDialog.Builder(context).create();
        alertDialog.setTitle("LoginStatus");
    }


    @Override
    protected void onPostExecute(String sResult) {

        if(sType != "getWeather" && sResult == null){
            Toast.makeText(context, "Result seems to be null.", Toast.LENGTH_LONG).show();
            return;
        }

        if (sType == "gpio"){
            if(sResult == null)
            {
                Toast.makeText(context, "Error", Toast.LENGTH_LONG).show();
            }
            else if(sResult.contains("changed gpio successfully")){
                Toast.makeText(context, sResult, Toast.LENGTH_LONG).show();
            }
        }
        else if(sType == "getSwitchStatus"){
            if (sResult.indexOf("Error") > 0)
                Toast.makeText(context, "Error getting statuses", Toast.LENGTH_LONG).show();
            else {
                String sAlertShow, geySt, MBACSt, LiftSt, St, Ed, AutoD;
                if (sResult.contains("G:") && sResult.contains("M:")){
                    geySt = sResult.substring(sResult.indexOf("G:")+2,sResult.indexOf("M:"));
                    if (geySt.contains("0"))
                        MainActivity.tbGeyser.setChecked(true);
                    else
                        MainActivity.tbGeyser.setChecked(false);
                }
                if (sResult.contains("L:") && sResult.contains("M:")){
                    MBACSt = sResult.substring(sResult.indexOf("M:")+2,sResult.indexOf("L:"));
                    if (MBACSt.contains("1"))
                        MainActivity.tbMBAC.setChecked(true);
                    else
                        MainActivity.tbMBAC.setChecked(false);
                }
                if (sResult.contains("L:") && sResult.contains("A:")){
                    LiftSt = sResult.substring(sResult.indexOf("L:")+2,sResult.indexOf("A:"));
                    if (LiftSt.contains("1"))
                        MainActivity.tbLift.setChecked(true);
                    else
                        MainActivity.tbLift.setChecked(true);
                }
                if (sResult.contains("S:") && sResult.contains("A:")){
                    MainActivity.sAlert = sResult.substring(sResult.indexOf("A:")+2,sResult.indexOf("S:"));

                    //MainActivity.sAlert = "0 Testing Back Door Open";

                    sAlertShow="";
                    if (MainActivity.sAlert.length()> 0) {
                        sAlertShow = MainActivity.sAlert.substring(2);
                        if(MainActivity.sAlert.substring(0,1).equals("0"))// When 0 it means disable flag is off.. means announce
                            MainActivity.sAlert = sAlertShow;
                        else
                            MainActivity.sAlert ="";
                    }
                    MainActivity.tvAlerts.setText(sAlertShow);

                }
                if (sResult.contains("S:") && sResult.contains("E:")){
                    St = sResult.substring(sResult.indexOf("S:")+2,sResult.indexOf("E:"));
                    MainActivity.autoStTm.setText(St.substring(0,5));
                }
                if (sResult.contains("D:") && sResult.contains("E:")){
                    Ed = sResult.substring(sResult.indexOf("E:")+2,sResult.indexOf("D:"));
                    MainActivity.autoEdTm.setText(Ed.substring(0,5));
                }
                if (sResult.contains("D:") ){
                    AutoD = sResult.substring(sResult.indexOf("D:")+2,sResult.length());
                    if (AutoD.contains("0"))
                        MainActivity.swMBACAuto.setChecked(true);
                    else
                        MainActivity.swMBACAuto.setChecked(false);
                }
            }
        }
        else if(sType == "getLogs"){
            if (sResult.contains("<br>") )
                sResult = sResult.replace("<br>","\n");
            MainActivity.tvLogs.setText(sResult);
        }
        else if(sType == "showkWh"){
            if (sResult.contains("<br>") )
                sResult = sResult.replace("<br>","\n");
            if (sResult.contains("<bt>") )
                sResult = sResult.replace("<bt>","\t");
            MainActivity.tvCT.setText(sResult);
        }
        else if(sType == "showSolar"){
            if (sResult.contains("<bt>") )
                sResult = sResult.replace("<bt>","\t");
            if (sResult.contains("<br>") ) {
                MainActivity.tvSolarMain.setText(sResult.substring(0,sResult.indexOf("<br>")));
                MainActivity.tvSolarBed.setText(sResult.substring(sResult.indexOf("<br>")+4,sResult.length()-4));
            }
        }
        else if(sType == "getWeather"){
            for (int fcstNo=0;fcstNo<3; fcstNo++){
                switch (fcstNo){
                    case 0:
                        fcst[fcstNo]=fcst[fcstNo].replace("Currently in Chennai International Airport, IN:", "");
                        fcst[fcstNo] = fcst[fcstNo].substring(0,fcst[fcstNo].indexOf("\n"));
                        MainActivity.tvCurrWtr.setText(MainActivity.device.concat(fcst[fcstNo]));
                        new ImageLoadTask(fcstImgUrl[fcstNo], MainActivity.ivCurrWtr).execute();
                        break;
                    case 1:
                        MainActivity.tvFd1Wtr.setText(fcst[fcstNo]);
                        new ImageLoadTask(fcstImgUrl[fcstNo], MainActivity.ivFd1Wtr).execute();
                        break;
                    case 2:
                        MainActivity.tvFd2Wtr.setText(fcst[fcstNo]);
                        new ImageLoadTask(fcstImgUrl[fcstNo], MainActivity.ivFd2Wtr).execute();
                        break;
                }
            }
        }
        else {
            if(sResult == null)
            {
                //registerClient.etName.setText("testing");
                // do what you want to do
            } else if(sResult.contains("Registration Success"))
            {
                Toast.makeText(context, sResult, Toast.LENGTH_LONG).show();
                //registerClient.sClient = sResult.substring(sResult.indexOf(":") + 1);
            }
            else if(sResult.contains("Login Success")) // msg you get from success like "Login Success"
            {
                alertDialog.setMessage(sResult);
                alertDialog.show();

            }
            else if(sResult!=null)
            {
                alertDialog.setMessage(sResult);
                alertDialog.show();

            }

        }

    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }

    public backGroundActivity() {
        super();
    }

}

