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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
            init(2, 2);
        }
    }

    private void init(int x, int y) {
        _fillDown(x, y, 0, 0);

        mFirstVisibleRow = y;
        mFirstVisibleColumn = x;

        int lastVisableX = mFirstVisibleColumn;
        int maxX = mAdapter.getColumnCount() - 1;
        int boundWidth = getWidth();
        int right = mAdapter.getCellWidth(lastVisableX);

        while (right < boundWidth && lastVisableX < maxX) {
            lastVisableX++;
            right += mAdapter.getCellWidth(lastVisableX);
        }

        mLastVisibleColumn = lastVisableX;
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

        Log.d("Seven2", "remove -> " + cell.getPosition());
        mAllCells.remove(cell.getPosition());
    }

    private void removeCell(ExcelAdapter.CellPosition pos) {
        ExcelAdapter.Cell cell = mAllCells.remove(pos);
        if (cell != null && !cell.isEmpty()) {
            removeView(cell.getView());
        }
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

        Log.d("Seven", " x -> " + x + "  " + y + " " + nextTop + "  " + nextLeft);

        int yEnd = getMeasuredHeight(); // ignore padding
        int maxY = mAdapter.getRowCount() - 1;

        while (y <= maxY && nextTop < yEnd) {

            _makeRow(x, y, nextTop, nextLeft);

            /*
            if (mReferenceCell != null && !mReferenceCell.isEmpty()) {
                nextTop += mReferenceCell.getView().getBottom();
            }
            */
            nextTop += mAdapter.getCellHeight(y);

            y++;
        }

        mLastVisibleRow = y - 1;
    }

    private void _fillRight(int x, int y, int top, int left) {

        int xEnd = getMeasuredWidth();
        int maxX = mAdapter.getColumnCount() - 1;

        while (x <= maxX && left < xEnd) {

            _makeColumn(x, y, top, left);

            left += mAdapter.getCellWidth(x);

            x++;
        }

        mLastVisibleColumn = x - 1;
    }

    private void _fillUp(int x, int y, int bottom, int left) {

        int yStart = 0; // ignore padding
        int minY = 0;

        while (y >= minY && bottom > yStart) {

            _makeRow(x, y, bottom - mAdapter.getCellHeight(y), left);

            bottom -= mAdapter.getCellHeight(y);

            y--;
        }

        mFirstVisibleRow = y + 1;
    }

    private void _fillLeft(int x, int y, int top, int right) {

        int xStart = 0;
        int minX = 0;

        while (x >= minX && right > xStart) {

            _makeColumn(x, y, top, right - mAdapter.getCellWidth(x));

            right -= mAdapter.getCellWidth(x);

            --x;
        }

        mFirstVisibleColumn = x + 1;
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

    private void _makeColumn(int x, int startY, int top, int left) {
        int yEnd = getMeasuredHeight();
        int maxY = mAdapter.getRowCount() - 1;

        while (startY <= maxY && top < yEnd) {

            _makeAndAddCell(x, startY, left, top);

            top += mAdapter.getCellHeight(startY);
            startY++;
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
        } else {

            ExcelAdapter.CellPosition parent = mAdapter.getParentCell(
                    ExcelAdapter.CellPosition.create(x, y)
            );

            if (parent == null) {
                throw new IllegalStateException("must not null");
            }

            if (!mAllCells.containsKey(parent)) {
                int parentLeft = cellLeft;
                for (int dx = x - 1; dx >= parent.x; --dx) {
                    parentLeft -= mAdapter.getCellWidth(dx);
                }

                int parentTop = cellTop;
                for (int dy = y - 1; dy >= parent.y; --dy) {
                    parentTop -= mAdapter.getCellHeight(dy);
                }

                _makeAndAddCell(parent.x, parent.y, parentLeft, parentTop);
            }
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

        _relayoutChildren(dx, dy);

        recycle(dx, dy);

        if (dy < 0) {
            _fillDown(mFirstVisibleColumn, mLastVisibleRow+1,
                    getTop(mLastVisibleRow + 1), getLeft(mFirstVisibleColumn));
        } else if (dy > 0) {
            _fillUp(mFirstVisibleColumn, mFirstVisibleRow - 1,
                    getBottom(mFirstVisibleRow - 1), getLeft(mFirstVisibleColumn));
        }

        if (dx < 0) {
            _fillRight(mLastVisibleColumn + 1, mFirstVisibleRow,
                    getTop(mFirstVisibleRow), getLeft(mLastVisibleColumn + 1));
        } else if (dx > 0) {

            _fillLeft(mFirstVisibleColumn - 1, mFirstVisibleRow,
                    getTop(mFirstVisibleRow), getRight(mFirstVisibleColumn - 1));
        }
    }

    private int getLeft(int column) {
        ExcelAdapter.Cell cell = null;
        for (ExcelAdapter.Cell c : mAllCells.values()) {
            if (!c.isEmpty()) {
                cell = c;
            }
        }

        if (cell == null) {
            return 0;
        }

        if (cell.getColumn() == column) {
            return cell.getView().getLeft();
        } else if (cell.getColumn() > column) {
            int left = cell.getView().getLeft();
            for (int c = cell.getColumn() - 1; c >= column; --c) {
                left -= mAdapter.getCellWidth(c);
            }
            return left;
        } else {
            int left = cell.getView().getLeft();
            for (int c = cell.getColumn() + 1; c <= column; ++c) {
                left += mAdapter.getCellWidth(c - 1);
            }
            return left;
        }
    }

    private int getRight(int column) {
        return getLeft(column) + mAdapter.getCellWidth(column);
    }

    private int getTop(int row) {
        ExcelAdapter.Cell cell = null;
        for (ExcelAdapter.Cell c : mAllCells.values()) {
            if (!c.isEmpty()) {
                cell = c;
            }
        }

        if (cell == null) {
            return 0;
        }

        if (cell.getRow() == row) {
            return cell.getView().getTop();
        } else if (cell.getRow() > row) {
            int top = cell.getView().getTop();
            for (int r = cell.getRow() - 1; r >= row; --r) {
                top -= mAdapter.getCellHeight(r);
            }
            return top;
        } else {
            int top = cell.getView().getTop();
            for (int r = cell.getRow() + 1; r <= row; ++r) {
                top += mAdapter.getCellHeight(r - 1);
            }
            return top;
        }
    }

    private int getBottom(int row) {
        int top = getTop(row);
        return top + mAdapter.getCellHeight(row);
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

    private void recycle(int dx, int dy) {

        if (dx > 0) {
            recycleRight();
        } else if (dx < 0) {
            recycleLeft();
        }

        if (dy > 0) {
            recycleBottom();
        } else if (dy < 0) {
            recycleTop();
        }
    }

    private void recycleTop() {

        int y = mFirstVisibleRow;
        int maxY = mAdapter.getRowCount() - 1;
        int bottom = getBottom(y);
        while (y <= maxY && bottom <= 0) {
            recycleRow(y);

            y++;
            bottom += mAdapter.getCellHeight(y);
        }

        mFirstVisibleRow = y;
    }

    private void recycleLeft() {

        int x = mFirstVisibleColumn;
        int maxX = mAdapter.getColumnCount() - 1;
        int right = getRight(x);

        while (x <= maxX && right <= 0) {

            recycleColumn(x);

            x++;
            right += mAdapter.getCellWidth(x);
        }

        mFirstVisibleColumn = x;
    }

    private void recycleBottom() {

        int y = mLastVisibleRow;
        int minY = 0;

        int top = getTop(y);
        while (y >= minY && top >= getMeasuredHeight()) {

            recycleRow(y);

            y--;
            top -= mAdapter.getCellHeight(y);
        }

        mLastVisibleRow = y;
    }

    private void recycleRight() {

        int x = mLastVisibleColumn;
        int minX = 0;

        int left = getLeft(x);

        while (x >= minX && left >= getMeasuredWidth()) {

            recycleColumn(x);
            x--;
            left -= mAdapter.getCellWidth(x);
        }

        mLastVisibleColumn = x;
    }

    private void recycleRow(int row) {
        int startColumn = mFirstVisibleColumn;
        int endColumn = mLastVisibleColumn;

        for (int c = startColumn; c <= endColumn; ++c) {
            removeAndRecycleCell(c, row);
        }
    }

    private void removeAndRecycleCell(int x, int y) {
        ExcelAdapter.CellPosition pos = ExcelAdapter.CellPosition.create(x, y);
        ExcelAdapter.Cell cell = mAllCells.get(pos);

        if (cell == null) {
            return;
        }

        boolean shouldRemove = false;

        if (!cell.isEmpty()) {
            View cellView = cell.getView();
            int left = cellView.getLeft();
            int right = cellView.getRight();
            int top = cellView.getTop();
            int bottom = cellView.getBottom();

            if (left >= getMeasuredWidth() ||
                    right <= 0 ||
                    top >= getMeasuredHeight() ||
                    bottom <= 0) {
                shouldRemove = true;
            }
        } else {
            shouldRemove = true;
            ExcelAdapter.CellPosition parent = mAdapter.getParentCell(pos);

            removeAndRecycleCell(parent.x, parent.y);

        }


        if (shouldRemove) {
            removeCell(cell);
        }
    }

    private void recycleColumn(int column) {

        Log.d("Seven2", "recyc " + column + " ");

        int startRow = mFirstVisibleRow;
        int endRow = mLastVisibleRow;

        for (int r = startRow; r <= endRow; ++r) {
            removeAndRecycleCell(column, r);
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
