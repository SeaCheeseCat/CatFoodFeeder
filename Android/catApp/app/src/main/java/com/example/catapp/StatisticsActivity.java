package com.example.catapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;

public class StatisticsActivity extends AppCompatActivity {

    private LineChart lineChart;
    private Button resetButton;

    private List<Float> dailyFeedData = new ArrayList<>();
    private List<Float> dailyEatData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        lineChart = findViewById(R.id.line_chart);


        // 加载数据
        loadData();

        // 显示折线图
        displayLineChart();

    }

    private void loadData() {
        // 模拟读取数据，实际应从 SharedPreferences 或数据库中获取
        SharedPreferences sharedPreferences = getSharedPreferences("CatAppPrefs", MODE_PRIVATE);

        // 如果没有记录的数据，则使用模拟数据
        int days = sharedPreferences.getInt("daysRecorded", 7); // 假设记录了7天的数据
        if (days >= 0) {
            // 使用模拟数据代替 SharedPreferences
            dailyFeedData.add(100f); // 第一天的投喂量
            dailyFeedData.add(120f); // 第二天的投喂量
            dailyFeedData.add(110f); // 第三天的投喂量
            dailyFeedData.add(100f); // 第四天的投喂量
            dailyFeedData.add(100f); // 第五天的投喂量
            dailyFeedData.add(90f); // 第六天的投喂量
            dailyFeedData.add(90f); // 第七天的投喂量

            dailyEatData.add(90f); // 第一天的进食量
            dailyEatData.add(110f); // 第二天的进食量
            dailyEatData.add(95f); // 第三天的进食量
            dailyEatData.add(90f); // 第四天的进食量
            dailyEatData.add(90f); // 第五天的进食量
            dailyEatData.add(90f); // 第六天的进食量
            dailyEatData.add(90f); // 第七天的进食量

            // 更新SharedPreferences，保存模拟数据
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("daysRecorded", 7); // 更新天数记录
            for (int i = 0; i < 7; i++) {
                editor.putFloat("feedWeightDay" + i, dailyFeedData.get(i));
                editor.putFloat("eatWeightDay" + i, dailyEatData.get(i));
            }
            editor.apply();
        } else {
            // 如果有数据，则从 SharedPreferences 中读取数据
            for (int i = 0; i < days; i++) {
                dailyFeedData.add(sharedPreferences.getFloat("feedWeightDay" + i, 0f));
                dailyEatData.add(sharedPreferences.getFloat("eatWeightDay" + i, 0f));
            }
        }
    }

    private void displayLineChart() {
        List<Entry> feedEntries = new ArrayList<>();
        List<Entry> eatEntries = new ArrayList<>();

        for (int i = 0; i < dailyFeedData.size(); i++) {
            feedEntries.add(new Entry(i, dailyFeedData.get(i)));
            eatEntries.add(new Entry(i, dailyEatData.get(i)));
        }

        LineDataSet feedDataSet = new LineDataSet(feedEntries, "Feed Weight");
        feedDataSet.setColor(getResources().getColor(R.color.purple_500));
        feedDataSet.setLineWidth(2f);

        LineDataSet eatDataSet = new LineDataSet(eatEntries, "Eat Weight");
        eatDataSet.setColor(getResources().getColor(R.color.teal_200));
        eatDataSet.setLineWidth(2f);

        LineData lineData = new LineData(feedDataSet, eatDataSet);
        lineChart.setData(lineData);

        // 设置图表的样式
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getAxisRight().setEnabled(false);  // 只显示左侧的Y轴
        // 设置X轴标签格式
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setGranularity(1f);  // 强制x轴每个标签之间都有间隔
        xAxis.setLabelCount(dailyFeedData.size(), true);  // 根据数据点数量设置标签数目

        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // 假设你的x轴是基于天数，你可以在这里添加单位“天”
                int dayIndex = (int) value; // 转换为整数
                if (dayIndex < dailyFeedData.size()) {
                    return "Day " + (dayIndex + 1);  // 显示 Day 1, Day 2 等
                }
                return "";
            }
        });

        // 更新图表
        lineChart.invalidate();
    }

    private void resetDailyData() {
        // 清空数据
        dailyFeedData.clear();
        dailyEatData.clear();

        SharedPreferences sharedPreferences = getSharedPreferences("CatAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("daysRecorded", 0);
        editor.apply();

        // 更新图表
        displayLineChart();
    }
}