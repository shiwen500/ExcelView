package com.seven.www.example;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.transition.Slide;
import android.transition.Transition;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.seven.www.excelview.ExcelAdapter;
import com.seven.www.excelview.ExcelView;

/**
 * Created by chenshiwen on 18-5-31.
 */

public class SimpleFullScrollTableFragment extends BaseFragment {

    private static final int[] COLORS = {Color.BLACK, Color.RED, Color.BLUE, Color.GREEN};

    private ExcelView mExcelView;

    public SimpleFullScrollTableFragment(){}

    public static SimpleFullScrollTableFragment newInstance(String title) {
        SimpleFullScrollTableFragment simpleFullScrollTableFragment = new SimpleFullScrollTableFragment();
        Bundle args = new Bundle();
        args.putString(MainListFragment.TITLE_SUB_FRAGMENT, title);
        simpleFullScrollTableFragment.setArguments(args);
        return simpleFullScrollTableFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public ExcelAdapter onCreateAdapter() {
        return new SimpleFullScrollTableAdapter(getActivity());
    }

    public static class SimpleFullScrollTableAdapter implements ExcelAdapter {

        private Context mContext;

        public SimpleFullScrollTableAdapter(Context context) {
            mContext = context;
        }

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

            View item = LayoutInflater.from(mContext).inflate(R.layout.item_simple_full_scroll_table, parent, false);

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

            t.setBackgroundColor(COLORS[(y + x) % COLORS.length]);
        }
    }
}
