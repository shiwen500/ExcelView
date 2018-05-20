package com.seven.www.example;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.seven.www.excelview.ExcelAdapter;
import com.seven.www.excelview.ExcelView;

public class MainActivity extends AppCompatActivity {

    private int[] colors = {Color.BLACK, Color.RED, Color.BLUE, Color.GREEN};

    ExcelView excelView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        excelView = (ExcelView) findViewById(R.id.excelView);
        android.util.Log.d("Seven", "fffff");

        excelView.setExcelAdapter(new ExcelAdapter() {
            @Override
            public int getRowCount() {
                return 200;
            }

            @Override
            public int getColumnCount() {
                return 200;
            }

            @Override
            public int getCellType(int position) {


                return 1;
            }

            @Override
            public int getCellTypeCount() {
                return 3;
            }

            @Override
            public int getCellWidth(int column) {
                return 200;
            }

            @Override
            public int getCellHeight(int row) {
                return 200;
            }

            @Override
            public Cell onCreateCell(ViewGroup parent, int cellType) {

                View item = LayoutInflater.from(MainActivity.this).inflate(R.layout.excel_item, parent, false);

                return new Cell(item);
            }

            @Override
            public int getParentCell(int position) {

                return -1;
            }

            @Override
            public void onBindCell(Cell cell, int position) {
                int x = CellPosition.getX(position);
                int y = CellPosition.getY(position);
                TextView t = (TextView) cell.getView().findViewById(R.id.text1);
                t.setText(y + " " + x);

                t.setBackgroundColor(colors[(y + x) % colors.length]);
            }
        });
    }

    public void onTest(View view) {
        excelView.setFixedXAndY(1, 1);
    }
}
