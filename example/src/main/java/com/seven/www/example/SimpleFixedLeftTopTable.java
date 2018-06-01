package com.seven.www.example;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.seven.www.excelview.ExcelAdapter;
import com.seven.www.excelview.ExcelView;
import com.seven.www.excelview.ViewUtil;

/**
 * Created by chenshiwen on 18-6-1.
 */
public class SimpleFixedLeftTopTable extends BaseFragment {

    public SimpleFixedLeftTopTable() {}

    public static SimpleFixedLeftTopTable newInstance(String title) {
        SimpleFixedLeftTopTable simpleFixedLeftTopTable = new SimpleFixedLeftTopTable();
        Bundle args = new Bundle();
        args.putString(MainListFragment.TITLE_SUB_FRAGMENT, title);
        simpleFixedLeftTopTable.setArguments(args);
        return simpleFixedLeftTopTable;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mExcelView.setFixedXAndY(0, 0);
    }

    @Override
    public ExcelAdapter onCreateAdapter() {
        return new SimpleFixedLeftTopTableAdapter(getActivity());
    }

    public static class SimpleFixedLeftTopTableAdapter implements ExcelAdapter {

        private static final int CELL_TYPE_0 = 0;
        private static final int CELL_TYPE_TOP = 1;
        private static final int CELL_TYPE_LEFT = 2;
        private static final int CELL_TYPE_BODY = 3;

        private Context mContext;
        private int mCellWidth;
        private int mCellHeight;
        private int mLeftWidth;
        private int mTopHeight;

        public SimpleFixedLeftTopTableAdapter(Context context) {
            mContext = context;
            mCellWidth = ViewUtil.dp2px(context.getResources(), 50);
            mCellHeight = ViewUtil.dp2px(context.getResources(), 50);
            mLeftWidth = ViewUtil.dp2px(context.getResources(), 25);
            mTopHeight = ViewUtil.dp2px(context.getResources(), 25);
        }

        @Override
        public int getRowCount() {
            return 100;
        }

        @Override
        public int getColumnCount() {
            return 27;
        }

        @Override
        public int getCellType(int position) {
            int x = CellPosition.getX(position);
            int y = CellPosition.getY(position);
            if (x == 0 && y == 0) {
                return CELL_TYPE_0;
            } else if (x >= 1 && y >= 1) {
                return CELL_TYPE_BODY;
            } else if (x == 0) {
                return CELL_TYPE_LEFT;
            } else if (y == 0) {
                return CELL_TYPE_TOP;
            }
            return CELL_TYPE_0;
        }

        @Override
        public int getCellTypeCount() {
            return 4;
        }

        @Override
        public int getCellWidth(int column) {
            return column == 0 ? mLeftWidth : mCellWidth;
        }

        @Override
        public int getCellHeight(int row) {
            return row == 0 ? mTopHeight : mCellHeight;
        }

        @Override
        public Cell onCreateCell(ViewGroup parent, int cellType) {
            Cell cell = null;

            switch (cellType) {
                case CELL_TYPE_0:
                case CELL_TYPE_LEFT:
                case CELL_TYPE_TOP:
                    TextView textView = new TextView(mContext);
                    cell = new Cell(textView);
                    break;
                case CELL_TYPE_BODY:
                    TextView imageView = new TextView(mContext);
                    cell = new Cell(imageView);
                    break;
            }

            return cell;
        }

        @Override
        public void onBindCell(Cell cell, int cellPosition) {
            int cellType = getCellType(cellPosition);
            int x = CellPosition.getX(cellPosition);
            int y = CellPosition.getY(cellPosition);
            switch (cellType) {
                case CELL_TYPE_0:
                    break;
                case CELL_TYPE_LEFT:
                    ((TextView)cell.getView()).setText(String.valueOf(y));
                    ((TextView)cell.getView()).setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                    break;
                case CELL_TYPE_TOP:
                    ((TextView)cell.getView()).setText(String.valueOf((char) ('A' + x - 1)));
                    ((TextView)cell.getView()).setGravity(Gravity.CENTER);
                    break;
                case CELL_TYPE_BODY:
                    String content = String.valueOf((char) ('A' + x - 1)) + y;
                    ((TextView)cell.getView()).setText(content);
                    ((TextView)cell.getView()).setGravity(Gravity.CENTER);
                    break;
            }
        }

        @Override
        public int getParentCell(int position) {
            return -1;
        }
    }
}
