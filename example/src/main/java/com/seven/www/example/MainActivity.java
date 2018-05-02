package com.seven.www.example;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

import com.seven.www.excelview.ExcelAdapter;
import com.seven.www.excelview.ExcelView;

public class MainActivity extends AppCompatActivity {

    private int[] colors = {Color.BLACK, Color.RED, Color.BLUE, Color.GREEN};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ExcelView excelView = (ExcelView) findViewById(R.id.excelView);
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

                int x = CellPosition.getX(position);
                int y = CellPosition.getY(position);

                if (x == 0 && y == 0 ||
                    x == 2 && y == 2) {
                    return 0;
                } else if (x <= 1 && y <= 1 ||
                        (x >= 2 && x <= 3
                        && y >= 2 && y <= 3)) {
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
                    return Cell.createEmptyCell(-1);
                }

                int m = 1;
                if (cellType == 0) {
                    m = 2;
                }

                TextView textView = new TextView(MainActivity.this);
                return new Cell(textView, -1, m, m);
            }

            @Override
            public int getParentCell(int position) {

                int x = CellPosition.getX(position);
                int y = CellPosition.getY(position);

                if (x <= 1 || y <= 1) {
                    return CellPosition.create(0, 0);
                } else if (x <= 1 && y <= 1 ||
                        (x >= 2 && x <= 3
                                && y >= 2 && y <= 3)) {
                    return CellPosition.create(2, 2);
                }
                return -1;
            }

            @Override
            public void onBindCell(Cell cell, int position) {
                int x = CellPosition.getX(position);
                int y = CellPosition.getY(position);
                TextView t = (TextView) cell.getView();
                t.setText(y + " " + x);

                t.setBackgroundColor(colors[(y + x) % colors.length]);
            }
        });
    }
}
