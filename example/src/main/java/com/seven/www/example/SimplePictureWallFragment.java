package com.seven.www.example;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.seven.www.excelview.ExcelAdapter;
import com.seven.www.excelview.ViewUtil;

/**
 * Created by chenshiwen on 18-6-2.
 */
public class SimplePictureWallFragment extends BaseFragment {

    private int[] mPicWidth = new int[]{4, 4, 1, 5, 3, 3,3,3,2,3,2,3,3,3,4,3,3,2,2, 4,2,3,3,2, 3,1,3,1,3,3};
    private int[] mPicResIds = new int[30];

    public SimplePictureWallFragment(){}

    public static SimplePictureWallFragment newInstance(String title) {
        SimplePictureWallFragment simplePictureWallFragment = new SimplePictureWallFragment();
        Bundle args = new Bundle();
        args.putString(MainListFragment.TITLE_SUB_FRAGMENT, title);
        simplePictureWallFragment.setArguments(args);
        return simplePictureWallFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        Resources resources = getResources();
        String type = "drawable";
        String packageName = getActivity().getPackageName();
        for (int i = 0; i < mPicResIds.length; ++i) {
            mPicResIds[i] = resources.getIdentifier("i" + (i+1), type, packageName);
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mExcelView.setDividerDrawable(new ColorDrawable(Color.WHITE));
        mExcelView.setDividerWidth(ViewUtil.dp2px(getResources(), 4));
    }

    @Override
    public ExcelAdapter onCreateAdapter() {
        return new PictureWallAdapter(getActivity());
    }

    public class PictureWallAdapter implements ExcelAdapter {

        private static final int ROW = 6;
        private static final int COLUMN = 14;

        private int mCellWidth;
        private int mCellHeight;
        private Context mContext;
        private SparseIntArray mCellTypes = new SparseIntArray(30);
        private SparseIntArray mCellImageResources = new SparseIntArray(30);
        private SparseIntArray mParentCellPos = new SparseIntArray(100);

        public PictureWallAdapter(Context context) {
            mContext = context;
            mCellWidth = ViewUtil.dp2px(context.getResources(), 100);
            mCellHeight = ViewUtil.dp2px(context.getResources(), 200);

            int row = 0;
            int column = 0;
            int leftWidth = COLUMN;

            int index = 0;
            for (int width : mPicWidth) {

                int pos = CellPosition.create(column, row);
                mCellTypes.put(pos, width);
                mCellImageResources.put(pos, mPicResIds[index]);

                for (int i = 1; i < width; ++i) {
                    mParentCellPos.put(CellPosition.create(column + i, row), pos);
                }

                leftWidth -= width;
                if (leftWidth <= 0) {
                    column = 0;
                    row += 1;
                    leftWidth = COLUMN;
                } else {
                    column += width;
                }

                ++index;
            }
        }

        @Override
        public int getRowCount() {
            return ROW;
        }

        @Override
        public int getColumnCount() {
            return COLUMN;
        }

        @Override
        public int getCellType(int position) {
            return mCellTypes.get(position, CELL_TYPE_EMPTY);
        }

        @Override
        public int getCellTypeCount() {
            return 6;
        }

        @Override
        public int getCellWidth(int column) {
            return mCellWidth;
        }

        @Override
        public int getCellHeight(int row) {
            return mCellHeight;
        }

        @Override
        public Cell onCreateCell(ViewGroup parent, int cellType) {
            if (cellType != CELL_TYPE_EMPTY) {
                ImageView imageView = new ImageView(mContext);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                return new Cell(imageView, cellType, 1);
            }
            return Cell.empty();
        }

        @Override
        public void onBindCell(Cell cell, int cellPosition) {
            ((ImageView)cell.getView()).setImageResource(mCellImageResources.get(cellPosition));
        }

        @Override
        public int getParentCell(int position) {
            return mParentCellPos.get(position, -1);
        }
    }
}
