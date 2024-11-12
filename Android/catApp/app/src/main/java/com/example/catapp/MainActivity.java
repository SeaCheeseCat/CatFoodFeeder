package com.example.catapp;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    // 已有的变量和视图声明
    private Button connectBtn, servoControlBtn, closeservoControlBtn;
    private TextView dataTextView;
    private LineChart lineChart;

    private String serverIP = "192.168.1.119";  // ESP8266 IP 地址
    private int serverPort = 8080;

    private Socket socket;
    private BufferedReader reader;
    private OutputStream writer;

    private ExecutorService executorService;
    private static final String TAG = "MainActivity";

    private ArrayList<Entry> chartDataEntries = new ArrayList<>();
    private LineDataSet dataSet;
    private LineData lineData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化视图和按钮
        connectBtn = findViewById(R.id.connectBtn);
        servoControlBtn = findViewById(R.id.servoControlBtn);
        closeservoControlBtn = findViewById(R.id.closeServoControlBtn);
        dataTextView = findViewById(R.id.dataTextView);

        // 初始化线程池
        executorService = Executors.newSingleThreadExecutor();

        // 初始化图表
        lineChart = findViewById(R.id.line_chart1);
        initLineChart();

        // 在 onCreate 方法中
        Button statsButton = findViewById(R.id.stats_button);
        statsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StatisticsActivity.class);
                startActivity(intent);
            }
        });
        connectBtn.setOnClickListener(v -> executorService.execute(new ConnectTask()));
        servoControlBtn.setOnClickListener(v -> sendBytesData(new byte[]{0x55, 0x01, 0x0B}));
        closeservoControlBtn.setOnClickListener(v -> sendBytesData(new byte[]{0x55, 0x02, 0x0B}));
    }

    private void initLineChart() {
        // 设置数据集和初始数据
        dataSet = new LineDataSet(chartDataEntries, "Weight Data");
        dataSet.setColor(getResources().getColor(R.color.purple_200)); // 设置颜色
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false); // 隐藏数据点的值

        lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // 设置X轴和Y轴的样式
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f); // 设置Y轴的最小值

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false); // 禁用右Y轴
    }

    // 连接到服务器的任务
    private class ConnectTask implements Runnable {
        @Override
        public void run() {
            try {
                socket = new Socket(serverIP, serverPort);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = socket.getOutputStream();
                Log.d(TAG, "Connection successful!");

                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connected to ESP8266", Toast.LENGTH_SHORT).show());
                executorService.execute(new ReadDataTask());
            } catch (IOException e) {
                Log.e(TAG, "Connection failed: " + e.toString());
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connection failed", Toast.LENGTH_SHORT).show());
            }
        }
    }

    private class ReadDataTask implements Runnable {
        @Override
        public void run() {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    final String data = line;
                    Log.d(TAG, "Received data: " + data);

                    runOnUiThread(() -> {
                        float weight = extractWeight(data);
                        if (weight != -1f) {
                            dataTextView.setText("Weight: " + weight + " kg");
                            addDataToChart(weight);
                        }
                    });
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading data: " + e.toString());
            } finally {
                closeSocket();
            }
        }
    }

    private void addDataToChart(float weight) {
        // 新增数据点并更新图表
        chartDataEntries.add(new Entry(chartDataEntries.size(), weight));
        dataSet.notifyDataSetChanged();
        lineData.notifyDataChanged();
        lineChart.notifyDataSetChanged();
        lineChart.invalidate(); // 刷新图表
    }

    private float extractWeight(String input) {
        String prefix = "Aight:";
        String suffix = "kg";
        int startIndex = input.indexOf(prefix);
        int endIndex = input.indexOf(suffix);

        if (startIndex != -1 && endIndex != -1) {
            String weightString = input.substring(startIndex + prefix.length(), endIndex).trim();
            return Float.parseFloat(weightString);
        }
        return -1;
    }

    private void sendBytesData(byte[] byteData) {
        new SendDataTask().execute(byteData);
    }

    private class SendDataTask extends AsyncTask<byte[], Void, String> {
        @Override
        protected String doInBackground(byte[]... params) {
            byte[] byteData = params[0];
            try {
                Socket socket = new Socket(serverIP, serverPort);
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(byteData);
                outputStream.flush();
                socket.close();
                return "Data sent successfully";
            } catch (IOException e) {
                e.printStackTrace();
                return "Error sending data: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
        }
    }

    private void closeSocket() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing socket: " + e.toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeSocket();
        executorService.shutdown();
    }
}
