package com.seven.www.example;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

import com.seven.www.excelview.ExcelAdapter;
import com.seven.www.excelview.ExcelView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ExcelView excelView = (ExcelView) findViewById(R.id.excelView);
        android.util.Log.d("Seven", "fffff");

        excelView.setExcelAdapter(new ExcelAdapter() {
            @Override
            public int getRowCount() {
                return 20;
            }

            @Override
            public int getColumnCount() {
                return 20;
            }

            @Override
            public int getCellType(CellPosition position) {
                if (position.x == 0 && position.y == 0) {
                    return 0;
                } else if (position.x <= 1 && position.y <= 1) {
                    return ExcelAdapter.CELL_TYPE_EMPTY;
                }
                return 1;
            }

            @Override
            public int getCellTypeCount() {
                return 3;
            }

            @Override
            public int getCellWidth(int column) {
                return 100;
            }

            @Override
            public int getCellHeight(int row) {
                return 100;
            }

            @Override
            public Cell onCreateCell(ViewGroup parent, int cellType) {
                if (cellType == ExcelAdapter.CELL_TYPE_EMPTY) {
                    return Cell.createEmptyCell(null);
                }

                int m = 1;
                if (cellType == 0) {
                    m = 2;
                }

                TextView textView = new TextView(MainActivity.this);
                return new Cell(textView, null, m, m);
            }

            @Override
            public void onBindCell(Cell cell, CellPosition cellPosition) {
                TextView t = (TextView) cell.getView();
                t.setText(cellPosition.y + " " + cellPosition.x);
            }
        });
    }
}