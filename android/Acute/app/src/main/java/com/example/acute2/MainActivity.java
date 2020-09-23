package com.example.acute2;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.acute2.Entity.Parameters;
import com.example.acute2.location.GPSLocationListener;
import com.example.acute2.location.GPSLocationManager;
import com.example.acute2.location.GPSProviderStatus;
import com.example.acute2.play.AudioPlayer;
import com.example.acute2.record.AudioRecorder;
import com.example.acute2.screen.ScreenReceiver;
import com.example.acute2.utils.HandleUtil;
import com.example.acute2.utils.Kalman;
import com.example.acute2.utils.TransferUtil;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.jtransforms.fft.DoubleFFT_1D;

import io.socket.client.IO;
import io.socket.client.Socket;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;

import java.lang.reflect.Parameter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MainActivity extends AppCompatActivity implements AudioRecorder.RecordingCallback, View.OnClickListener, GPSLocationListener {

    private TextView temperatureView;
    private ImageButton startBtn;
    private ImageButton switchBtn;
    private ImageButton editButton;

    Parameters parameters;
    private boolean isCalibrating = false;

    private LineChart lineChart;
    private Description description;
    private LinkedList yvalues;
    private LinkedList fftValues;
    private LinkedList tempValues;
    private int dataCount = 0;
    private LineDataSet lineDataSet;
    private LineData lineData;
    private XAxis xAxis ;
    private LimitLine limitLine;
    private Socket socket;
    private Location location;
    private boolean hasConnected = false;

    private double temperatureForShow;

    private int calibrateCount = 0;
    private double calibratedD = 0;
    //0代表的peek location曲线 1代表的是fft的结果
    private int curCurve = 0;
    private int curStatus = 1; //1标识播放

    private String TAG = "MainActivity";
    private AudioRecorder recorder;
    private AudioPlayer player;

    private Queue<Double> peekFreqQueue;
    private List<Double> peekFreqKalmanQueue;

    private String androidID;

    private DoubleFFT_1D fft;
    private Kalman kalman;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    private static String[] PERMISSIONS = {android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.RECORD_AUDIO,
    android.Manifest.permission.ACCESS_COARSE_LOCATION,
    android.Manifest.permission.ACCESS_FINE_LOCATION,
    android.Manifest.permission.INTERNET};
    private static int REQUEST_PERMISSION_CODE = 3;

    private BroadcastReceiver mReceiver = null;
    private GPSLocationManager gpsLocationManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //keep screen light
        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        verifyPermissions(this);
        init();
    }


    private void init(){
        kalman = new Kalman();
        initView();
        initParams();
    }

    @Override
    protected void onPause() {
        // when the screen is about to turn off
        if (ScreenReceiver.wasScreenOn) {
            // this is the case when onPause() is called by the system due to a screen state change
            if(isProcessThreadAlive){
                processEnd();
            }

        } else {
            // this is when onPause() is called when the screen state has not changed
        }
        super.onPause();
        gpsLocationManager.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //断开连接
        socket.off();
        socket.disconnect();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_btn: {
                if (curStatus == 1) {
                    processStart();
                } else {
                    processEnd();
                }
                break;
            }

            case R.id.switchButton:{
                if(curCurve == 0){
                    curCurve = 1;
                }else if(curCurve == 1){
                    curCurve = 2;
                }else if(curCurve == 2){
                    curCurve = 0;
                }
                break;
            }

            case R.id.edit_btn:{
                showPopupMenue(editButton);
                break;
            }
        }
    }

    @Override
    public void UpdateLocation(Location location) {

        this.location = location;
        Log.e(TAG,"获得定位");
        if (location != null) {
            Toast.makeText(MainActivity.this, location.getLongitude() +","+location.getLatitude(), Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void UpdateStatus(String provider, int status, Bundle extras) {
        Toast.makeText(MainActivity.this, "定位类型：" + provider, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void UpdateGPSProviderStatus(int gpsStatus) {
    }

    private void initInternet(){

        String uri = "http://"+parameters.getIp()+":"+parameters.getPort();

        try {
            socket = IO.socket(uri);
        } catch (URISyntaxException e) {
            Toast.makeText(MainActivity.this,
                    "connect fail", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        socket.connect();
        hasConnected = true;
        Toast.makeText(MainActivity.this,
                "connect ok", Toast.LENGTH_LONG).show();
    }

    /**
     * check permission
     * @param activity
     */
    private static void verifyPermissions(Activity activity){

        ArrayList<String> permissionsNotPassed = new ArrayList<String>();
        for (int i = 0; i < PERMISSIONS.length; i++) {
            if (ContextCompat.checkSelfPermission(activity, PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) {
                permissionsNotPassed.add(PERMISSIONS[i]);//添加还未授予的权限
            }
        }

        if (permissionsNotPassed.size() > 0) {
            //有权限没有通过，需要申请
            String[] request = new String[permissionsNotPassed.size()];
            for(int i=0;i<permissionsNotPassed.size();i++){
                request[i] = permissionsNotPassed.get(i);
            }
            ActivityCompat.requestPermissions(activity, request, 5);
        }
    }
    /**
     * init view fow show
     */
    private void initView() {
        temperatureView = findViewById(R.id.temperature);
        startBtn = findViewById(R.id.start_btn);
        switchBtn = findViewById(R.id.switchButton);
        editButton = findViewById(R.id.edit_btn);

        startBtn.setOnClickListener(this);
        switchBtn.setOnClickListener(this);
        editButton.setOnClickListener(this);

        //开启gps定位
        gpsLocationManager = GPSLocationManager.getInstances(MainActivity.this);
        gpsLocationManager.start(this);

        peekFreqQueue = new LinkedList<Double>();
        peekFreqKalmanQueue = new LinkedList<>();

        lineChart=findViewById(R.id.linechart);

        description = new Description();
        yvalues = new LinkedList();
        fftValues = new LinkedList<Entry>();
        tempValues = new LinkedList();

        lineChart.setNoDataText("Waiting for data...");
        xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

    }
    /**
     * init parameters
     */
    private void initParams(){
        parameters = new Parameters();
        androidID = getAndroidId(MainActivity.this);
        File f = new File("/data/data/com.example.acute/shared_prefs/params.xml");
        if (f.exists()) {
            SharedPreferences myPreference=getSharedPreferences("params", Context.MODE_PRIVATE);
            parameters.setD(myPreference.getFloat("d",0.15f));
            parameters.setDuration(myPreference.getFloat("duration",0.1f));
            parameters.setFmin(myPreference.getInt("fmin",1000));
            parameters.setFmax(myPreference.getInt("fmax",21000));
            parameters.setFftLength(myPreference.getInt("fftLength",524288));
            parameters.setSampleRate(myPreference.getInt("sampleRate",48000));
            parameters.setIp(myPreference.getString("ip","192.168.28.7"));
            parameters.setPort(myPreference.getString("port","8080"));
            parameters.setTemperatureForCalibration(myPreference.getFloat("tempForCal",30));

        } else{
            parameters.setD(0.15f);
            parameters.setDuration(0.1f);
            parameters.setFmin(1000);
            parameters.setFmax(21000);
            parameters.setSampleRate(48000);
            parameters.setFftLength(524288);
            parameters.setIp("192.168.28.7");
            parameters.setPort("8080");
            parameters.setTemperatureForCalibration(30);

        }
    }
    /**
     * init audio
     */
    private void initAudioWidget() {
        recorder = new AudioRecorder(parameters.getSampleRate(),parameters.getFmin(),parameters.getFmax(),parameters.getDuration());
        player = new AudioPlayer(parameters.getSampleRate(), parameters.getDuration(), parameters.getFmin(), parameters.getFmax());
        recorder.registerCallback(this);
        fft = new DoubleFFT_1D(parameters.getFftLength());
    }

    /**
     * destroy audio
     */
    private void destroyAudioWidget() {
        recorder = null;
        player = null;
        kalman.setFreqP(1);
        kalman.setFreqX(-60);
    }


    /**
     * do when started
     */
    private void processStart(){
        startBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_stop));
        initAudioWidget();
        recorder.startRecord();
        player.startPlay();
        startProcess();
        curStatus = 0;
    }


    /**
     * do when stopped
     */
    private void processEnd(){
        startBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_start));
        recorder.finishRecord();
        if (player.isPlaying()) {
            player.finishPlay();
        }
        destroyAudioWidget();
        isProcessThreadAlive = false;
        curStatus = 1;
    }

    /**
     * calibrate
     */
    private void calibrate(){

        isCalibrating = true;
        if(isProcessThreadAlive){
            startBtn.performClick();
        }
        startBtn.performClick();
    }

    /**
     * menu view
     * @param view
     */
    @SuppressLint("RestrictedApi")
    private void showPopupMenue(View view){
        PopupMenu popupMenu = new PopupMenu(MainActivity.this, view);
        popupMenu.getMenuInflater().inflate(R.menu.main, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){

                    case R.id.menu_item1:
                        showSettingsDialog();
                        break;

                    case R.id.menu_item2:
                        showCalibrationDailog();
                        break;

                    case R.id.menu_item3:
                        showHelpDialog();
                        break;

                    case R.id.menu_item4:
                        showNetDialog();
                        break;
                }
                return true;
            }
        });

        try {
            Field field = popupMenu.getClass().getDeclaredField("mPopup");
            field.setAccessible(true);
            MenuPopupHelper helper = (MenuPopupHelper) field.get(popupMenu);
            helper.setForceShowIcon(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        popupMenu.show();

    }

    /**
     * set net view
     */
    private void showNetDialog() {
        LayoutInflater factory = LayoutInflater.from(this);
        final View internetView = factory.inflate(R.layout.internet, null);
        EditText ipView = (EditText) internetView.findViewById(R.id.ip);
        EditText portView = (EditText) internetView.findViewById(R.id.port);
        ipView.setText(parameters.getIp());
        portView.setText(parameters.getPort());

        AlertDialog.Builder ad1 = new AlertDialog.Builder(this);
        ad1.setTitle("internet");
        ad1.setView(internetView);

        ad1.setPositiveButton("connect", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                Toast.makeText(MainActivity.this,
                        "connect...", Toast.LENGTH_LONG).show();
                parameters.setIp(ipView.getText().toString());
                parameters.setPort(portView.getText().toString());
                SharedPreferences myPreference = getSharedPreferences("params", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = myPreference.edit();
                editor.putString("ip",parameters.getIp());
                editor.putString("port",parameters.getPort());
                editor.commit();
                initInternet();
            }
        });

        ad1.show();
    }

    /**
     * calibrate view
     */
    private void showCalibrationDailog(){
        LayoutInflater factory = LayoutInflater.from(this);
        final View calibrationView = factory.inflate(R.layout.calibration,null);
        EditText curTempView = (EditText) calibrationView.findViewById(R.id.curTemp);
        curTempView.setText(parameters.getTemperatureForCalibration()+"");
        AlertDialog.Builder ad1 = new AlertDialog.Builder(this);
        ad1.setTitle("calibration");
        ad1.setView(calibrationView);

        ad1.setPositiveButton("calibrate", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                Toast.makeText(MainActivity.this,
                        "begin calibrating",Toast.LENGTH_LONG).show();
                parameters.setTemperatureForCalibration(Float.parseFloat(curTempView.getText().toString()));

                SharedPreferences myPreference = getSharedPreferences("params", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = myPreference.edit();
                editor.putFloat("tempForCal", (float) parameters.getTemperatureForCalibration());
                editor.commit();
                calibrate();
            }
        });

        ad1.show();
    }

    /**
     * help view
     */
    private void showHelpDialog(){
        LayoutInflater factory = LayoutInflater.from(this);
        final View helpView = factory.inflate(R.layout.help,null);
        AlertDialog.Builder ad1 = new AlertDialog.Builder(this);
        ad1.setView(helpView);
        ad1.setPositiveButton("get it", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {

            }
        });
        ad1.show();
    }


    /**
     * set parameter view
     */
    private void showSettingsDialog(){
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.dialog, null);
        //定位控件

        EditText dParam = (EditText) textEntryView.findViewById(R.id.d);
        EditText fsParam = (EditText) textEntryView.findViewById(R.id.fsParam);
        EditText durationParam = (EditText) textEntryView.findViewById(R.id.durationParam);
        EditText fminParam = (EditText) textEntryView.findViewById(R.id.fminParam);
        EditText fmaxParam = (EditText) textEntryView.findViewById(R.id.fmaxParam);
        EditText fftParam = (EditText)textEntryView.findViewById(R.id.fftPoints);

        //show current value
        dParam.setText(""+parameters.getD());
        fsParam.setText(parameters.getSampleRate()+"");
        durationParam.setText(parameters.getDuration()+"");
        fminParam.setText(parameters.getFmin()+"");
        fmaxParam.setText(parameters.getFmax()+"");
        fftParam.setText(parameters.getFftLength()+"");

        AlertDialog.Builder ad1 = new AlertDialog.Builder(this);
        ad1.setTitle("settings");
        ad1.setView(textEntryView);
        ad1.setPositiveButton("save", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                //保存
                parameters.setD(Float.parseFloat(dParam.getText().toString()));
                parameters.setSampleRate(Integer.parseInt(fsParam.getText().toString()));
                parameters.setDuration(Float.parseFloat(durationParam.getText().toString()));
                parameters.setFmin(Integer.parseInt(fminParam.getText().toString()));
                parameters.setFmax(Integer.parseInt(fmaxParam.getText().toString()));
                parameters.setFftLength(Integer.parseInt(fftParam.getText().toString()));

                SharedPreferences myPreference=getSharedPreferences("params", Context.MODE_PRIVATE);
//像SharedPreference中写入数据需要使用Editor
                SharedPreferences.Editor editor = myPreference.edit();
                editor.putFloat("d",parameters.getD());
                editor.putInt("sampleRate",parameters.getSampleRate());
                editor.putFloat("duration",parameters.getDuration());
                editor.putInt("fmin",parameters.getFmin());
                editor.putInt("fmax",parameters.getFmax());
                editor.putInt("fftLength",parameters.getFftLength());
                editor.commit();

            }
        });
        ad1.show();
        //ad1.create().getWindow().setLayout(WRAP_CONTENT,WRAP_CONTENT);// 显示对话框
    }


    /**
     * callback of recorder
     * @param data
     * @param offset
     */
    @Override
    public void onDataReady(short[] data, int offset) {

        short[] left = new short[data.length/2];
        short[] right = new short[data.length/2];

        for(int i=0;i<data.length/2;i++){
            left[i] = data[2*i];
            right[i] = data[2*i+1];
        }

        short[] alignedLeft = new short[left.length/2];
        short[] alignedRight = new short[left.length/2];

        for(int i=0;i<left.length/2;i++){
            if(i+offset >= left.length){
                alignedLeft[i] = left[(i+offset)%left.length];
                alignedRight[i] = right[(i+offset)%left.length];
            }else {
                alignedLeft[i] = left[i + offset];
                alignedRight[i] = right[i + offset];
            }
        }
        double[] normalizedData = new double[parameters.getFftLength()];
        double[] curPeek = compute(alignedLeft,alignedRight,normalizedData);
        drawCurVe(curPeek,normalizedData);
    }

    private volatile boolean isProcessThreadAlive = false;

    private void startProcess() {
        isProcessThreadAlive = true;
    }

    /**
     * fft and find the peek of result
     * @param left
     * @param right
     * @param normalizedData
     * @return
     */
    private double[] compute(short[] left,short[] right,double[] normalizedData) {

        int[] mixedData = HandleUtil.mixData(left, right);
        for (int i = 0; i < mixedData.length; i++) {
            normalizedData[i] = (double) mixedData[i] / 2000 / 2000;
        }
        //normalizedData contains the result of fft。the first half is Real part and the second half is imaginary part
        fft.realForward(normalizedData);
        double[] curPeek = HandleUtil.findPeek(normalizedData,isCalibrating, parameters);
        return curPeek;
    }

    /**
     * draw curve via choice
     * @param curPeek
     * @param normalizedData
     */
    private void drawCurVe(double[] curPeek,double[] normalizedData){

        double resolution = parameters.getResolution();

        if(curPeek[0] > 0){
            //show calibrate curve
            if(isCalibrating){
                int startIndex = 0;
                int endIndex = (int) (200 / resolution);
                int peekIndex = (int) curPeek[2];
                if (!fftValues.isEmpty()) {
                    fftValues.removeAll(fftValues);
                }
                for (int i = startIndex; i < endIndex; i += 10) {
                    double mag = Math.sqrt(normalizedData[2 * i] * normalizedData[2 * i] + normalizedData[2 * i + 1] * normalizedData[2 * i + 1]);
                    fftValues.addLast(new Entry((float) (i * resolution), (float) mag));
                }
                lineDataSet = new LineDataSet(fftValues, "fft");
                xAxis.removeAllLimitLines();
                //绘制限制线
                limitLine = new LimitLine((float) (peekIndex * resolution), "");
                limitLine.enableDashedLine(1f, 4f, 0f);//虚线
                limitLine.setLineColor(Color.parseColor("#d91111"));
                limitLine.setLineWidth(1f);
                limitLine.setTextColor(Color.parseColor("#d91111"));
                xAxis.addLimitLine(limitLine);
                xAxis.setDrawGridLines(false);
                lineDataSet.setDrawCircles(false);
                lineDataSet.setDrawValues(false);
                lineData = new LineData(lineDataSet);
                description.setText("fft");
                lineChart.setDescription(description);

                if (calibrateCount > 5) {
                    calibratedD += TransferUtil.freqTod(curPeek[0],parameters);
                }
                calibrateCount++;

            }else {

                //show most 150 points at once
                if (peekFreqQueue.size() == 150) {
                    peekFreqQueue.remove();
                    peekFreqQueue.add(curPeek[0]);
                    peekFreqKalmanQueue.remove(0);
                    peekFreqKalmanQueue.add(curPeek[0]);

                } else {
                    peekFreqQueue.add(curPeek[0]);
                    peekFreqKalmanQueue.add(curPeek[0]);
                }

                //kalman filter
                kalman.kalmanFilter(peekFreqKalmanQueue, 1e-6, 4e-4);

                Double[] peekFreqArr = peekFreqQueue.toArray(new Double[0]);
                Double[] peekFreqKalmanArr = peekFreqKalmanQueue.toArray(new Double[0]);

                double[] freqArr = new double[peekFreqArr.length];
                double[] freqKalmanArr = new double[peekFreqKalmanArr.length];
                for (int i = 0; i < peekFreqArr.length; i++) {
                    freqArr[i] = peekFreqArr[i].doubleValue();
                    freqKalmanArr[i] = peekFreqKalmanArr[i].doubleValue();
                }

                temperatureForShow = TransferUtil.freqToTemperature(freqKalmanArr[freqKalmanArr.length - 1],parameters);

                //产生peek location的画图数据
                dataCount += 1;
                float y = (float) (freqKalmanArr[freqKalmanArr.length - 1]);
                yvalues.addLast(new Entry(dataCount, y));
                tempValues.addLast(new Entry(dataCount, (float) TransferUtil.freqToTemperature(y,parameters)));
                if (yvalues.size() > 20) {
                    yvalues.removeFirst();
                    tempValues.removeFirst();
                }

                //显示周围0-200hz的点
                int startIndex = 0;
                int endIndex = (int) (200 / resolution);

                int peekIndex = (int) curPeek[2];
                Log.d(TAG, "peekindex" + peekIndex);


                if (!fftValues.isEmpty()) {
                    fftValues.removeAll(fftValues);
                }
                for (int i = startIndex; i < endIndex; i += 10) {
                    double mag = Math.sqrt(normalizedData[2 * i] * normalizedData[2 * i] + normalizedData[2 * i + 1] * normalizedData[2 * i + 1]);
                    fftValues.addLast(new Entry((float) (i * resolution), (float) mag));
                }


                //show different curve via choice
                //temperature curve
                if (curCurve == 0) {
                    lineDataSet = new LineDataSet(tempValues, "Temperature /℃");
                    lineDataSet.setColor(Color.BLUE);
                    lineData = new LineData(lineDataSet);
                    description.setText("Temperature");
                    lineChart.setDescription(description);
                }

                // peek of fft curve
                else if (curCurve == 1) {
                    lineDataSet = new LineDataSet(yvalues, "Peek Location /Hz");
                    lineDataSet.setColor(Color.BLUE);
                    lineData = new LineData(lineDataSet);
                    description.setText("Peek Location");
                    lineChart.setDescription(description);
                }
                //fft curve
                else if (curCurve == 2) {
                    lineDataSet = new LineDataSet(fftValues, "fft");
                    xAxis.removeAllLimitLines();
                    //draw limit line
                    limitLine = new LimitLine((float) (peekIndex * resolution), "");
                    limitLine.enableDashedLine(1f, 4f, 0f);//虚线
                    limitLine.setLineColor(Color.parseColor("#d91111"));
                    limitLine.setLineWidth(1f);
                    limitLine.setTextColor(Color.parseColor("#d91111"));
                    xAxis.addLimitLine(limitLine);
                    xAxis.setDrawGridLines(false);
                    lineDataSet.setDrawCircles(false);
                    lineDataSet.setDrawValues(false);
                    lineData = new LineData(lineDataSet);
                    description.setText("fft");
                    lineChart.setDescription(description);
                }
            }


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isCalibrating) {
                        if (location != null && socket != null) {
                            socket.emit("data", location.getLongitude(), location.getLatitude(), temperatureForShow, androidID);
                        }
                        temperatureView.setText(String.format("%.2f", temperatureForShow));
                    }else {
                        if (calibrateCount == 35) {
                            calibratedD = calibratedD / 29;
                            SharedPreferences myPreference = getSharedPreferences("params", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = myPreference.edit();
                            editor.putFloat("d", (float) calibratedD);
                            editor.commit();
                            parameters.setD((float) calibratedD);
                            calibrateCount = 0;
                            isCalibrating = false;
                            calibratedD = 0;
                            Toast.makeText(MainActivity.this,
                                    "finish", Toast.LENGTH_SHORT).show();
                            startBtn.performClick();
                        }
                    }
                    lineChart.setData(lineData);
                    lineChart.invalidate();
                }
            });
        }
    }

    public static String getAndroidId (Context context) {
        String ANDROID_ID = Settings.System.getString(context.getContentResolver(), Settings.System.ANDROID_ID);
        return ANDROID_ID;
    }
}


