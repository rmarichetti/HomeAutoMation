package com.aarnamahal.homeautomation;

import android.app.AlertDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
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
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

//import static com.aarnamahal.homeautomation.MainActivity.timerInc;

public class backGroundActivity  extends AsyncTask<String,Void,String> {
    Context context;
    String sResult;
    AlertDialog alertDialog;
    String sType;
    String HomeUrl = "http://192.168.0.6/"; // "http://210.18.139.72/";
    String[] fcst;
    String[] fcstImgUrl;
    String[] swStatuses;
    String sURL="";
    boolean bSiteReachable ;
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
        String post_data="";
        if(sType.equals("getWeather")) {
//Motor
            bSiteReachable = isServerReachable(context, "http://rss.accuweather.com/rss/liveweather_rss.asp?metric=1&locCode=VOMM");
//            if (sURL != "http://192.168.0.11/getFiles.php")
//                bSiteReachable = isServerReachable(context, sURL);
            if (bSiteReachable) {
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

        }
        else{
            try {
                if(sType.equals("gpio")){
                    sURL = HomeUrl + "AppHomeGPIOset.php";
                    String sGpioNo = params[1];
                    String sOnF = params[2];
                    post_data = URLEncoder.encode("gpio","UTF-8")+"="+URLEncoder.encode(sGpioNo,"UTF-8")+"&"
                            +URLEncoder.encode("on","UTF-8")+"="+URLEncoder.encode(sOnF,"UTF-8");
                }
                else if(sType.equals("openMainDoor")){
                    sURL = HomeUrl + "mainDoorOpen.php";
                    post_data = URLEncoder.encode("device","UTF-8")+"="+URLEncoder.encode(MainActivity.device,"UTF-8");
                }
                else if(sType.equals("mos_qt")){
                    sURL = HomeUrl + "mos_qt.php";
                    String sTop = params[1];
                    String sM = params[2];
                    post_data = URLEncoder.encode("device","UTF-8")+"="+URLEncoder.encode(MainActivity.device,"UTF-8")+"&"
                            +URLEncoder.encode("top","UTF-8")+"="+URLEncoder.encode(sTop,"UTF-8")+"&"
                            +URLEncoder.encode("m","UTF-8")+"="+URLEncoder.encode(sM,"UTF-8");
                }
                else if(sType.equals("execUrl")) {
                    sURL =HomeUrl +params[1];
                }
                else if(sType.equals("execUrlfull")) {
                    sURL =params[1];
                }
                else if(sType.equals("getImgFiles")) {
                    sURL ="http://192.168.0.11/getFiles.php";
                }
                else if(sType.equals("getSwitchStatus")) {
                    sURL =HomeUrl + "getSwitchStatuses.php";
                }
                else if(sType.equals("showkWh")) {
                    sURL =HomeUrl + "showkWh.php?app=1";
                }
                else if(sType.equals("showFns")) {
                    sURL =HomeUrl + "showFns.php?app=1";
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
                            sURL =HomeUrl + "epsolar/mInvOn.php";
                            break;
                        case "BedInv:Off":
                            sURL =HomeUrl + "epsolar/mInvOff.php";
                            break;
                    }
                }
                else if(sType.equals("showSolar")) {
                    sURL =HomeUrl + "showSolarApp.php";
                }
                else if(sType.equals("getLogs")) {
                    sURL =HomeUrl + "showLogApp.php";
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            MainActivity.scrSiteReachable = isServerReachable(context, "http://192.168.0.11/getFiles.php");
            if (sURL != "http://192.168.0.11/getFiles.php")
                bSiteReachable = isServerReachable(context, sURL);
            else
                bSiteReachable = MainActivity.scrSiteReachable;
            if(bSiteReachable){
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

        }


        return null;
    }
    static public boolean isServerReachable(Context context,String URLlink) {
        ConnectivityManager connMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connMan.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            try {
                URL urlServer = new URL(URLlink);
                HttpURLConnection urlConn = (HttpURLConnection) urlServer.openConnection();
                urlConn.setConnectTimeout(1000); //<- 1Second  Timeout
                urlConn.connect();
                if (urlConn.getResponseCode() == 200) {
                    return true;
                } else {
                    return false;
                }
            } catch (MalformedURLException e1) {
                return false;
            } catch (IOException e) {
                return false;
            }
        }
        return false;
    }
    static public boolean isServerReachablestr(String URLlink) {

        try {
            InetAddress.getByName(URLlink).isReachable(2000); //Replace with your name
            return true;
        } catch (Exception e)
        {
            return false;
        }
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


         if(sType != "getWeather" && !bSiteReachable){
            Toast.makeText(context, sURL + "Site not Available. Type:" +sType, Toast.LENGTH_LONG).show();
            return;
        }
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
                //Toast.makeText(context, sResult, Toast.LENGTH_LONG).show();
            }
        }
        else if(sType == "getSwitchStatus"){
            if (sResult.indexOf("Error") > 0)
                Toast.makeText(context, "Error getting statuses", Toast.LENGTH_LONG).show();
            else {
                swStatuses = sResult.split(";");
                //MainDoorOk;GeyserStatus;MBACStatus;LiftOnStatus;Alerts;OnTime;OffTime;DisabledFlag;invMainSt;invBedSt;switchStatuses::...;SolarWaterTemp
                if(swStatuses[0].contains("0"))
                    MainActivity.btnMainDoor.setBackgroundResource(R.drawable.button_selector_red);
                else
                    MainActivity.btnMainDoor.setBackgroundResource(R.drawable.button_selector);
                if (MainActivity.device.equals(context.getResources().getStringArray(R.array.Intercoms)[3]) ||
                        MainActivity.device.equals(context.getResources().getStringArray(R.array.Intercoms)[1])) {
                    MainActivity.btnMainDoor.setTextSize(20);
                    MainActivity.btnMainDoor.setPadding(20,20,20,20);
                }
                else{
                    MainActivity.btnMainDoor.setTextSize(40);
                    MainActivity.btnMainDoor.setPadding(40,40,40,40);
                }
                //MainActivity.btnMainDoor.setPadding(10,10,10,10);
                if(swStatuses[1].contains("0"))
                    MainActivity.tbGeyser.setChecked(true);
                else
                    MainActivity.tbGeyser.setChecked(false);

                if (swStatuses[2].contains("1"))
                    MainActivity.tbMBAC.setChecked(true);
                else
                    MainActivity.tbMBAC.setChecked(false);

                if (swStatuses[3].contains("1"))
                    MainActivity.tbMotor.setChecked(true);
                else
                    MainActivity.tbMotor.setChecked(false);

                MainActivity.sAlert = swStatuses[4];
                String sAlertShow="";
                if (MainActivity.sAlert.length()> 0) {
                    String [] sAlertArr = MainActivity.sAlert.split(":");
                    sAlertShow = sAlertArr[1];//.sAlert.substring(2);
                    if(Integer.parseInt(sAlertArr[0])<1)// When 0 or less than 1 it means disable flag is off.. means announce
                        MainActivity.sAlert = sAlertShow;
                    else
                        MainActivity.sAlert ="";
                }
                //sAlertShow = "Testing";
                //MainActivity.sAlert ="Testing now";
                MainActivity.tvAlerts.setText(sAlertShow);

                if (MainActivity.timerInc%10 == 0){
                    if (swStatuses[5].length()>0)
                        MainActivity.autoStTm.setText(swStatuses[5].substring(0,5));
                    if (swStatuses[6].length()>0)
                        MainActivity.autoEdTm.setText(swStatuses[6].substring(0,5));
                }
                MainActivity.timerInc++;
                if (MainActivity.timerInc> 60000) MainActivity.timerInc = 0;

                if (swStatuses[7].contains("0"))
                    MainActivity.swMBACAuto.setChecked(true);
                else
                    MainActivity.swMBACAuto.setChecked(false);

                if (swStatuses[8].contains("Off"))
                    MainActivity.tbMain.setChecked(false);
                else
                    MainActivity.tbMain.setChecked(true);

                if (swStatuses[9].contains("Off"))
                    MainActivity.tbBed.setChecked(false);
                else
                    MainActivity.tbBed.setChecked(true);

                //btnLiftInd, btnDinAC1, btnDinAC2, btnHallAC1, btnHallAC2, btnHallAC3
                String[] sMhSw;


                if (swStatuses[10].contains("Dl")){
                    sMhSw = swStatuses[10].split(":");
                    MainActivity.btnDinAC1.setText(sMhSw[0]);
                    if (sMhSw[1].contains("1"))
                        MainActivity.btnDinAC1.setBackgroundColor(context.getResources().getColor(R.color.colorTorchRed));
                    else
                        MainActivity.btnDinAC1.setBackgroundColor(context.getResources().getColor(R.color.colorGreen));
                    MainActivity.sDinAC1 = sMhSw[2];
                }
                if (swStatuses[11].contains("Dr")){
                    sMhSw = swStatuses[11].split(":");
                    MainActivity.btnDinAC2.setText(sMhSw[0]);
                    if (sMhSw[1].contains("1"))
                        MainActivity.btnDinAC2.setBackgroundColor(context.getResources().getColor(R.color.colorTorchRed));
                    else
                        MainActivity.btnDinAC2.setBackgroundColor(context.getResources().getColor(R.color.colorGreen));
                    MainActivity.sDinAC2 = sMhSw[2];
                }
                if (swStatuses[12].contains("Lift")){
                    sMhSw = swStatuses[12].split(":");
                    MainActivity.btnLiftInd.setText(sMhSw[0]);
                    if (sMhSw[1].contains("1"))
                        MainActivity.btnLiftInd.setBackgroundColor(context.getResources().getColor(R.color.colorTorchRed));
                    else
                        MainActivity.btnLiftInd.setBackgroundColor(context.getResources().getColor(R.color.colorGreen));
                    MainActivity.sLiftInd = sMhSw[2];
                }
                if (swStatuses[13].contains("MH1")){
                    sMhSw = swStatuses[13].split(":");
                    MainActivity.btnHallAC1.setText(sMhSw[0]);
                    if (sMhSw[1].contains("1"))
                        MainActivity.btnHallAC1.setBackgroundColor(context.getResources().getColor(R.color.colorTorchRed));
                    else
                        MainActivity.btnHallAC1.setBackgroundColor(context.getResources().getColor(R.color.colorGreen));
                    MainActivity.sHallAC1 = sMhSw[2];
                }
                if (swStatuses[14].contains("MH2")){
                    sMhSw = swStatuses[14].split(":");
                    MainActivity.btnHallAC2.setText(sMhSw[0]);
                    if (sMhSw[1].contains("1"))
                        MainActivity.btnHallAC2.setBackgroundColor(context.getResources().getColor(R.color.colorTorchRed));
                    else
                        MainActivity.btnHallAC2.setBackgroundColor(context.getResources().getColor(R.color.colorGreen));
                    MainActivity.sHallAC2 = sMhSw[2];
                }
                if (swStatuses[15].contains("MH3")){
                    sMhSw = swStatuses[15].split(":");
                    MainActivity.btnHallAC3.setText(sMhSw[0]);
                    if (sMhSw[1].contains("1"))
                        MainActivity.btnHallAC3.setBackgroundColor(context.getResources().getColor(R.color.colorTorchRed));
                    else
                        MainActivity.btnHallAC3.setBackgroundColor(context.getResources().getColor(R.color.colorGreen));
                    MainActivity.sHallAC3 = sMhSw[2];
                }
                String sTemp= swStatuses[16];
                if (sTemp.length()> 0) {
                    //String[] sTemDt = sTemp.split(":");
                    MainActivity.tbGeyser.setTextOff(sTemp +"º Geyser Off");
                    MainActivity.tbGeyser.setTextOn(sTemp +"º Geyser On");
                    float fTemp =Float.parseFloat(sTemp);
                    if (fTemp>40){
                        MainActivity.tbGeyser.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.toggle_selector));
                    }
                    else if (fTemp>30 && fTemp<=40){
                        MainActivity.tbGeyser.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.toggle_selector_warning));
                    }
                    if (fTemp<=30){
                        MainActivity.tbGeyser.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.toggle_selector_critical_warning));
                    }
                }
                sTemp= swStatuses[17];
                if (sTemp.length()> 0) {
                    //String[] sTemDt = sTemp.split(":");
                    MainActivity.tbMotor.setTextOff(sTemp +" Motor Off");
                    MainActivity.tbMotor.setTextOn(sTemp +" Motor On");
                    float fTemp =Float.parseFloat(sTemp);
                    if (fTemp>2){
                        MainActivity.tbMotor.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.toggle_selector_warning));
                    }
                    else if (fTemp>1 && fTemp<=2){
                        MainActivity.tbMotor.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.toggle_selector));
                    }
                    if (fTemp<=1){
                        MainActivity.tbMotor.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.toggle_selector_critical_warning));
                    }
                }
                if (swStatuses[18].length()> 0){
                    String[] sTchSwSt = swStatuses[18].split("~");
                    String[] sTchSw;
                    boolean bSwOn;
                    for(int sw = 0; sw<sTchSwSt.length; sw++) {
                        sTchSw = sTchSwSt[sw].split(":");
                        if(sTchSw[2].contains("1")) bSwOn=true;
                        else bSwOn = false;
                        if (sTchSw[0].contains("/home/tchSwOffice/OnCmd")) {
                            if( sTchSw[1].contains("L")) MainActivity.tbOffLgt.setChecked(bSwOn);
                            else if(sTchSw[1].contains("F")) MainActivity.tbOffFan.setChecked(bSwOn);
                            else if(sTchSw[1].contains("A")) MainActivity.tbOffAC.setChecked(bSwOn);
                        }
                        if (sTchSw[0].contains("/home/tchSwliv/OnCmd")) {
                            if( sTchSw[1].contains("E")) MainActivity.tbLivLgtE.setChecked(bSwOn);
                            else if(sTchSw[1].contains("W")) MainActivity.tbLivLgtW.setChecked(bSwOn);
                            else if(sTchSw[1].contains("F")) MainActivity.tbLivFan.setChecked(bSwOn);
                        }
                    }
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
        else if(sType == "showFns"){
            if (sResult.contains("<br>") )
                sResult = sResult.replace("<br>","\n");
            if (sResult.contains("<bt>") )
                sResult = sResult.replace("<bt>","\t");
            MainActivity.tvFNs.setText(sResult);
        }
        else if(sType == "showSolar"){
            if (sResult.contains("<bt>") )
                sResult = sResult.replace("<bt>","\t");
            if (sResult.contains("<br>") ) {
                MainActivity.tvSolarMain.setText(sResult.substring(0,sResult.indexOf("<br>")));
                MainActivity.tvSolarBed.setText(sResult.substring(sResult.indexOf("<br>")+4,sResult.length()-4));
            }
        }
        else if(sType == "getImgFiles") {
            //String[] imgFiles;
            MainActivity.imgFiles = sResult.split(";");
            MainActivity.imgMaxNo = MainActivity.imgFiles.length;
            //int i = 4;
        }
        else if(sType == "getWeather"){
            for (int fcstNo=0;fcstNo<3; fcstNo++){
                switch (fcstNo){
                    case 0:
                        fcst[fcstNo]=fcst[fcstNo].replace("Currently in Chennai International Airport, IN:", "");
                        fcst[fcstNo] = fcst[fcstNo].substring(0,fcst[fcstNo].indexOf("\n"));
                        MainActivity.tvCurrWtr.setText( fcst[fcstNo] );
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
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }

    public backGroundActivity() {
        super();
    }

}

