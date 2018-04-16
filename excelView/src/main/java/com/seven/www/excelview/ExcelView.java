package com.seven.www.excelview;

import android.content.Context;
import android.graphics.Rect;
import android.support.v4.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.Vector;

/**
 * This view show a table likes excel.
 *
 * @author chenshiwen
 */
public class ExcelView extends ViewGroup{

    private static final String TAG = "ExcelView";

    private ExcelAdapter mAdapter;

    private CellRecycler mCellRecycler;

    private int[] mWidths;
    private int[] mHeights;

    private List<List<ExcelAdapter.Cell>> mCells = new ArrayList<>();

    private Rect mCellsRect = new Rect();

    private int mScrollX;
    private int mScrollY;

    private int mFirstVisibleRow;
    private int mLastVisibleRow;
    private int mFirstVisibleColumn;
    private int mLastVisibleColumn;

    private boolean mFirstLaunch;

    public ExcelView(Context context) {
        super(context);
    }

    public ExcelView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExcelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed) {
            _fillDown(0, 0, 0, 0);
            int cellCount = mAllCells.size();
        }
    }

    private void layoutAllCell() {

        int realLeft = mCellsRect.left - mScrollX;
        int realTop = mCellsRect.top - mScrollY;
        int realRight = mCellsRect.right - mScrollX;
        int realBottom = mCellsRect.bottom - mScrollY;

        layoutCells(realLeft, realTop, realRight, realBottom);

    }

    private void layoutCells(int left, int top, int right, int bottom) {

        int rowTop = top;
        int rowLeft = left;

        for (List<ExcelAdapter.Cell> r : mCells) {

            Log.d(TAG, "layoutCells " + rowTop);

            if (!r.isEmpty()) {
                Log.d(TAG, "layoutCells r " + rowLeft);
                for (ExcelAdapter.Cell c : r) {

                    if (!c.isEmpty()) {
                        int w = getCellWidth(c);
                        int h = getCellHeight(c);

                        c.getView().layout(rowLeft, rowTop, rowLeft + w, rowTop + h);
                    }

                    rowLeft += mWidths[c.getColumn()];
                }

                rowTop += mHeights[r.get(0).getRow()];
                rowLeft = left;
            }
        }
    }

    private void initAllCell() {
        mFirstLaunch = true;
        while (mCellsRect.bottom < getMeasuredHeight()) {
            addBottom();
        }
        mFirstLaunch = false;
    }

    private void removeAllCell() {
        int visibleRows = getCurrentVisibleRowCount();
        for (int i = 0; i < visibleRows; ++i) {
            removeTop();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        setMeasuredDimension(getMeasureWidthOrHeight(widthMeasureSpec, mWidths),
            getMeasureWidthOrHeight(heightMeasureSpec, mHeights));
    }

    private int getMeasureWidthOrHeight(int measureSpec, int[] sizes) {
        final int mode = MeasureSpec.getMode(measureSpec);
        final int size = MeasureSpec.getSize(measureSpec);

        switch (mode) {
            case MeasureSpec.EXACTLY:
                return size;
            case MeasureSpec.UNSPECIFIED:
                return sum(sizes);
            case MeasureSpec.AT_MOST:
                return Math.min(sum(sizes), size);
        }

        return 0;
    }

    private int sum(int[] sizes) {
        int sum = 0;
        for (int size : sizes) {
            sum += size;
        }
        return sum;
    }

    public void setExcelAdapter(ExcelAdapter adapter) {
        if (adapter == null) {
            throw new IllegalArgumentException("Don't pass an null adapter");
        }
        mAdapter = adapter;
        mCellRecycler = new CellRecycler(mAdapter.getCellTypeCount());
        setUpAdapter();
    }

    private void setUpAdapter() {
        // init body's heights & widths.
        mWidths = new int[mAdapter.getColumnCount()];
        mHeights = new int[mAdapter.getRowCount()];

        for (int col = 0; col < mWidths.length; ++col) {
            mWidths[col] = mAdapter.getCellWidth(col);
        }
        for (int row = 0; row < mHeights.length; ++row) {
            mHeights[row] = mAdapter.getCellHeight(row);
        }

    }

    private ExcelAdapter.Cell makeAndAddCell(int row, int column) {

        ExcelAdapter.CellPosition position = ExcelAdapter.CellPosition.create(column, row);
        int cellType = mAdapter.getCellType(position);
        ExcelAdapter.Cell ret = mCellRecycler.getRecycledCell(cellType);
        if (ret == null) {
            ret = mAdapter.onCreateCell(this, cellType);
        }

        ret.setCellPosition(position);

        int width = getCellWidth(ret);
        int height = getCellHeight(ret);

        if (!ret.isEmpty()) {

            View view = ret.getView();
            if (view.getLayoutParams() == null) {
                view.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            }

            int widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST);
            int heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);

            ret.getView().measure(widthMeasureSpec, heightMeasureSpec);


            mAdapter.onBindCell(ret, position);

            addCell(ret);
        }

        return ret;
    }

    private void addLeft() {
        addColumn(mFirstVisibleColumn);
        mCellsRect.left -= mWidths[mFirstVisibleColumn];
        mFirstVisibleColumn--;
    }

    private void addRight() {
        int newColumnNumber = mFirstVisibleColumn + getCurrentVisibleColumnCount();
        addColumn(newColumnNumber);
        mCellsRect.right += mWidths[newColumnNumber];
    }

    private void addTop() {
        addRow(mFirstVisibleRow);
        mCellsRect.top -= mHeights[mFirstVisibleRow];
        mFirstVisibleRow--;
    }

    private void addBottom() {
        int newRowNumber = mFirstVisibleRow + getCurrentVisibleRowCount();
        addRow(newRowNumber);
        mCellsRect.bottom += mHeights[newRowNumber];
    }

    private void addColumn(int column) {
        int internalColIndex = column - mFirstVisibleColumn;

        int row = mFirstVisibleRow;

        if (internalColIndex < 0) {
            internalColIndex = 0;
        } else if (internalColIndex >= getCurrentVisibleColumnCount()) {
            internalColIndex = getCurrentVisibleColumnCount();
        }

        for (List<ExcelAdapter.Cell> r : mCells) {
            ExcelAdapter.Cell cell = makeAndAddCell(row, column);

            r.add(internalColIndex, cell);

            row++;
        }
    }

    private void addRow(int row) {
        int internalRowIndex = row - mFirstVisibleRow;

        int column = mFirstVisibleColumn;
        if (internalRowIndex < 0) {
            internalRowIndex = 0;
        } else if (internalRowIndex >= getCurrentVisibleRowCount()) {
            internalRowIndex = getCurrentVisibleRowCount();
        }

        List<ExcelAdapter.Cell> r = new ArrayList<>();
        mCells.add(internalRowIndex, r);

        int colCount = getCurrentVisibleColumnCount();
        if (mFirstLaunch) {
            colCount = getMaxVisibleColumnCount();
        }
        for (int i = 0; i < colCount; ++i) {

            ExcelAdapter.Cell cell = makeAndAddCell(row, column);

            r.add(i, cell);

            column++;
        }
    }

    private void removeLeft() {
        removeColumn(mFirstVisibleColumn);
        mCellsRect.left += mWidths[mFirstVisibleColumn];
        mFirstVisibleColumn++;
    }

    private void removeRight() {
        int removedColumnNumber = mFirstVisibleColumn + getCurrentVisibleColumnCount() - 1;
        removeColumn(removedColumnNumber);
        mCellsRect.right -= mWidths[removedColumnNumber];
    }

    private void removeTop() {
        removeRow(mFirstVisibleRow);
        mCellsRect.top += mHeights[mFirstVisibleRow];

        mFirstVisibleRow++;
    }

    private void removeBottom() {
        int removedRowNumber = mFirstVisibleRow + getCurrentVisibleRowCount() - 1;
        removeColumn(removedRowNumber);
        mCellsRect.bottom -= mHeights[removedRowNumber];
    }

    private void removeColumn(int column) {
        int internalColIndex = column - mFirstVisibleColumn;

        int row = mFirstVisibleRow;
        for (List<ExcelAdapter.Cell> r : mCells) {
            int cellType = getCellType(row, column);

            ExcelAdapter.Cell cell = r.remove(internalColIndex);
            removeCell(cell);

            mCellRecycler.addRecycledCell(cell, cellType);

            row++;
        }
    }

    private void removeRow(int row) {
        int internalRowIndex = row - mFirstVisibleRow;

        int column = mFirstVisibleColumn;

        List<ExcelAdapter.Cell> cellRow = mCells.remove(internalRowIndex);
        for (ExcelAdapter.Cell c : cellRow) {

            int cellType = getCellType(row, column);
            removeCell(c);

            mCellRecycler.addRecycledCell(c, cellType);

            column++;
        }
    }

    private int getCellType(int row, int column) {
        ExcelAdapter.CellPosition position = ExcelAdapter.CellPosition.create(column, row);
        return mAdapter.getCellType(position);
    }

    private void removeCell(ExcelAdapter.Cell cell) {
        if (!cell.isEmpty()) {
            removeView(cell.getView());
        }
        mAllCells.remove(cell.getPosition());
    }

    private void addCell(ExcelAdapter.Cell cell) {
        if (!cell.isEmpty()) {
            addView(cell.getView());
        }
        mAllCells.put(cell.getPosition(), cell);
    }

    private int getCurrentVisibleRowCount() {
        return mCells.size();
    }

    private int getCurrentVisibleColumnCount() {
        if (mCells.isEmpty()) {
            return 0;
        }
        return mCells.get(0).size();
    }

    private int getMaxVisibleRowCount() {
        int maxRowCount = 0;
        int index = mFirstVisibleRow;
        int bottom = mCellsRect.top;
        while (bottom < mScrollY + getMeasuredHeight()) {
            bottom += mHeights[index];
            ++maxRowCount;
            ++index;
        }
        return maxRowCount;
    }

    private int getMaxVisibleColumnCount() {
        int maxColumnCount = 0;
        int index = mFirstVisibleColumn;
        int right = mCellsRect.left;
        while (right < mScrollX + getMeasuredWidth()) {
            right += mWidths[index];
            ++index;
            ++maxColumnCount;
        }
        return maxColumnCount;
    }

    private int getCellWidth(ExcelAdapter.Cell cell) {
        if (cell.isEmpty()) {
            return 0;
        }
        int width = mAdapter.getCellWidth(cell.getColumn());
        for (int i = 1; i < cell.getMergeWidth(); ++i) {
            width += mAdapter.getCellWidth(cell.getColumn() + i);
        }
        return width;
    }

    private int getCellHeight(ExcelAdapter.Cell cell) {
        if (cell.isEmpty()) {
            return 0;
        }
        int height = mAdapter.getCellHeight(cell.getRow());
        for (int i = 1; i < cell.getMergeHeight(); ++i) {
            height += mAdapter.getCellHeight(cell.getRow() + i);
        }
        return height;
    }


    /*******************************/

    private SortedMap<ExcelAdapter.CellPosition, ExcelAdapter.Cell>
        mAllCells = new TreeMap<>(new Comparator<ExcelAdapter.CellPosition>() {
        @Override
        public int compare(ExcelAdapter.CellPosition o1, ExcelAdapter.CellPosition o2) {
            if (o1.y > o2.y) {
                return 1;
            } else if (o1.y < o2.y) {
                return -1;
            }

            if (o1.x > o2.x) {
                return 1;
            } else if (o1.x < o2.x) {
                return -1;
            }

            return 0;
        }
    });

    private float mLastTouchX;
    private float mLastTouchY;

    private void _fillDown(int x, int y, int nextTop, int nextLeft) {

        int yEnd = getMeasuredHeight(); // ignore padding
        int maxY = mAdapter.getRowCount() - 1;

        while (y <= maxY && nextTop < yEnd) {

            _makeRow(x, y, nextTop, nextLeft);

            mLastVisibleRow = Math.max(mLastVisibleRow, y);
            /*
            if (mReferenceCell != null && !mReferenceCell.isEmpty()) {
                nextTop += mReferenceCell.getView().getBottom();
            }
            */
            nextTop += mAdapter.getCellHeight(y);

            y++;
        }
    }

    private void _fillUp(int x, int y, int nextBottom, int nextLeft) {

        int yStart = 0; // ignore padding
        int minY = 0;

        while (y >= minY && nextBottom > yStart) {

            _makeRow(x, y, nextBottom + mAdapter.getCellHeight(y), nextLeft);

            mFirstVisibleRow = Math.min(mFirstVisibleRow, y);

            nextBottom -= mAdapter.getCellHeight(y);

            y--;
        }
    }

    private void _makeRow(int startX, int y, int top, int nextLeft) {

        int xEnd = getMeasuredWidth(); // ignore padding.
        int maxX = mAdapter.getColumnCount() - 1;

        while (startX <= maxX && nextLeft < xEnd) {

            _makeAndAddCell(startX, y, nextLeft, top);

            nextLeft += mAdapter.getCellWidth(startX);
            startX++;
        }
    }

    private ExcelAdapter.Cell _makeAndAddCell(int x, int y, int cellLeft, int cellTop) {

        Log.d(TAG, String.format("x = %d y = %d left = %d top = %d", x, y, cellLeft, cellTop));

        ExcelAdapter.CellPosition position = ExcelAdapter.CellPosition.create(x, y);

        ExcelAdapter.Cell active = mAllCells.get(position);
        if (active != null) {
            Log.d(TAG, String.format("x = %d y = %d is Active", x, y));
            return active;
        }

        int cellType = mAdapter.getCellType(position);
        ExcelAdapter.Cell ret = mCellRecycler.getRecycledCell(cellType);
        if (ret == null) {
            ret = mAdapter.onCreateCell(this, cellType);
        }

        ret.setCellPosition(position);

        int width = getCellWidth(ret);
        int height = getCellHeight(ret);


        if (!ret.isEmpty()) {

            View view = ret.getView();
            if (view.getLayoutParams() == null) {
                view.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            }

            int widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST);
            int heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);

            ret.getView().measure(widthMeasureSpec, heightMeasureSpec);
            mAdapter.onBindCell(ret, position);


            ret.getView().layout(cellLeft, cellTop,
                cellLeft + width,
                cellTop + height);
        }

        addCell(ret);

        return ret;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                break;

            case MotionEvent.ACTION_MOVE:
                int dx = (int)(event.getX() - mLastTouchX);
                int dy = (int)(event.getY() - mLastTouchY);
                _scrollBy(dx, dy);
                break;

            case MotionEvent.ACTION_UP:
                break;
        }

        mLastTouchX = event.getX();
        mLastTouchY = event.getY();

        return true;
    }

    private void _scrollBy(int dx, int dy) {

        if (dx > 0) {

        }

        _relayoutChildren(dx, dy);
    }

    private ExcelAdapter.Cell findLeftReferenceCell() {
        int columnCount = mAdapter.getColumnCount();
        int columnIndex = 0;
        ExcelAdapter.Cell cell = getCell(columnIndex, 0);

        while ((cell == null || cell.isEmpty()) && columnIndex < columnCount - 1) {
            cell = getCell(++columnIndex, 0);
        }
        return cell;
    }

    private ExcelAdapter.Cell findRightReferenceCell() {
        int columnIndex = mAdapter.getColumnCount() - 1;
        ExcelAdapter.Cell cell = getCell(columnIndex, 0);

        while ((cell == null || cell.isEmpty()) && columnIndex > 0) {
            cell = getCell(--columnIndex, 0);
        }

        return cell;
    }

    private ExcelAdapter.Cell getCell(int x, int y) {
        if (mAdapter.getRowCount() == 0 || mAdapter.getColumnCount() == 0) {
            return null;
        }
        ExcelAdapter.CellPosition cellPosition = ExcelAdapter.CellPosition.create(x, y);
        return mAllCells.get(cellPosition);
    }

    private void _relayoutChildren(int dx, int dy) {
        int count = getChildCount();

        for (int i = 0; i < count; ++i) {
            View child = getChildAt(i);

            child.offsetLeftAndRight(dx);
            child.offsetTopAndBottom(dy);
        }
    }

    private int _calculateDeltaValue(int currentVal, int deltaVal, int maxVal, int minVal) {
        if (deltaVal < 0) {
            return Math.max(currentVal + deltaVal, minVal) - currentVal;
        } else if (deltaVal > 0) {
            return Math.min(currentVal + deltaVal, maxVal) - currentVal;
        }
        return deltaVal;
    }
}
