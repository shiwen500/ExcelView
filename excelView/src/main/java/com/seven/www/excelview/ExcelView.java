package com.seven.www.excelview;

import android.content.Context;
import android.graphics.Rect;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

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

    private FlingRunnable mFlingRunnable;
    private VelocityTracker mVelocityTracker;

    private int mMaxVelocity;
    private int mMinVelocity;


    private int mFixedX = 1;
    private int mFixedY = 1;

    public ExcelView(Context context) {
        this(context, null);
    }

    public ExcelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExcelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mMaxVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        mMinVelocity = viewConfiguration.getScaledMinimumFlingVelocity();

        mFlingRunnable = new FlingRunnable(getContext());
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed) {
            init(getMinFullScrollX(), getMinFullScrollY());
        }
    }

    private void init(int x, int y) {
        _fillDown(x, y, getMinFullScrollTop(), getMinFullScrollLeft());

        mFirstVisibleRow = y;
        mFirstVisibleColumn = x;

        int lastVisableX = mFirstVisibleColumn;
        int maxX = getMaxFullScrollX();
        int boundWidth = getMaxFullScrollRight();
        int right = mAdapter.getCellWidth(lastVisableX) + getMinFullScrollLeft();

        while (right < boundWidth && lastVisableX < maxX) {
            lastVisableX++;
            right += mAdapter.getCellWidth(lastVisableX);
        }

        mLastVisibleColumn = lastVisableX;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        final int height = MeasureSpec.getSize(heightMeasureSpec);
        final int width = MeasureSpec.getSize(widthMeasureSpec);

        int totalWidth = 0;
        for (int i = 0; i < mAdapter.getColumnCount(); ++i) {
            totalWidth += mAdapter.getCellWidth(i);
            if (totalWidth > width) {
                break;
            }
        }

        int totalHeight = 0;
        for (int i = 0; i < mAdapter.getRowCount(); ++i) {
            totalHeight += mAdapter.getCellHeight(i);
            if (totalHeight > height) {
                break;
            }
        }

        setMeasuredDimension(getMeasureWidthOrHeight(widthMeasureSpec, totalWidth),
            getMeasureWidthOrHeight(heightMeasureSpec, totalHeight));
    }

    private int getMeasureWidthOrHeight(int measureSpec, int expect) {
        final int mode = MeasureSpec.getMode(measureSpec);
        final int size = MeasureSpec.getSize(measureSpec);

        switch (mode) {
            case MeasureSpec.EXACTLY:
                return size;
            case MeasureSpec.UNSPECIFIED:
                return expect;
            case MeasureSpec.AT_MOST:
                return Math.min(expect, size);
        }

        return 0;
    }

    public void setExcelAdapter(ExcelAdapter adapter) {
        if (adapter == null) {
            throw new IllegalArgumentException("Don't pass an null adapter");
        }
        mAdapter = adapter;
        mCellRecycler = new CellRecycler(mAdapter.getCellTypeCount());
    }

    private void removeCell(ExcelAdapter.Cell cell) {
        if (!cell.isEmpty()) {
            removeView(cell.getView());
        }
        ExcelAdapter.Cell recycle = mAllCells.remove(cell.getPosition());
        mCellRecycler.addRecycledCell(recycle, mAdapter.getCellType(cell.getPosition()));
    }

    private void addCell(ExcelAdapter.Cell cell) {
        if (!cell.isEmpty()) {
            addView(cell.getView());
        }
        mAllCells.put(cell.getPosition(), cell);
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

    private SortedMap<Integer, ExcelAdapter.Cell>
        mAllCells = new TreeMap<>(new Comparator<Integer>() {
        @Override
        public int compare(Integer o1, Integer o2) {

            int y1 = ExcelAdapter.CellPosition.getY(o1);
            int y2 = ExcelAdapter.CellPosition.getY(o2);

            if (y1 > y2) {
                return 1;
            } else if (y1 < y2) {
                return -1;
            }

            int x1 = ExcelAdapter.CellPosition.getX(o1);
            int x2 = ExcelAdapter.CellPosition.getX(o2);

            if (x1 > x2) {
                return 1;
            } else if (x1 < x2) {
                return -1;
            }

            return 0;
        }
    });

    private float mLastTouchX;
    private float mLastTouchY;

    private void _fillDown(int x, int y, int nextTop, int nextLeft) {

        Log.d("Seven", " x -> " + x + "  " + y + " " + nextTop + "  " + nextLeft);

        int yEnd = getMaxFullScrollBottom(); // ignore padding
        int maxY = getMaxFullScrollY();

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

        int xEnd = getMaxFullScrollRight();
        int maxX = getMaxFullScrollX();

        while (x <= maxX && left < xEnd) {

            _makeColumn(x, y, top, left);

            left += mAdapter.getCellWidth(x);

            x++;
        }

        mLastVisibleColumn = x - 1;
    }

    private void _fillUp(int x, int y, int bottom, int left) {

        int yStart = getMinFullScrollTop(); // ignore padding
        int minY = getMinFullScrollY();

        while (y >= minY && bottom > yStart) {

            _makeRow(x, y, bottom - mAdapter.getCellHeight(y), left);

            bottom -= mAdapter.getCellHeight(y);

            y--;
        }

        mFirstVisibleRow = y + 1;
    }

    private void _fillLeft(int x, int y, int top, int right) {

        int xStart = getMinFullScrollLeft();
        int minX = getMinFullScrollX();

        while (x >= minX && right > xStart) {

            _makeColumn(x, y, top, right - mAdapter.getCellWidth(x));

            right -= mAdapter.getCellWidth(x);

            --x;
        }

        mFirstVisibleColumn = x + 1;
    }

    private void _makeRow(int startX, int y, int top, int nextLeft) {

        int xEnd = getMaxFullScrollRight(); // ignore padding.
        int maxX = getMaxFullScrollX();

        while (startX <= maxX && nextLeft < xEnd) {

            _makeAndAddCell(startX, y, nextLeft, top);

            nextLeft += mAdapter.getCellWidth(startX);
            startX++;
        }
    }

    private void _makeColumn(int x, int startY, int top, int left) {
        int yEnd = getMaxFullScrollBottom();
        int maxY = getMaxFullScrollY();

        while (startY <= maxY && top < yEnd) {

            _makeAndAddCell(x, startY, left, top);

            top += mAdapter.getCellHeight(startY);
            startY++;
        }
    }

    private ExcelAdapter.Cell _makeAndAddCell(int x, int y, int cellLeft, int cellTop) {

        //Log.d(TAG, String.format("x = %d y = %d left = %d top = %d", x, y, cellLeft, cellTop));

        int position = ExcelAdapter.CellPosition.create(x, y);

        ExcelAdapter.Cell active = mAllCells.get(position);
        if (active != null) {
            //Log.d(TAG, String.format("x = %d y = %d is Active", x, y));
            return active;
        }

        int cellType = mAdapter.getCellType(position);
        ExcelAdapter.Cell ret = mCellRecycler.getRecycledCell(cellType);

        boolean inCache = false;

        if (ret == null) {
            ret = mAdapter.onCreateCell(this, cellType);
        } else {
            inCache = true;
        }

        ret.setCellPosition(position);

        int width = getCellWidth(ret);
        int height = getCellHeight(ret);


        if (!ret.isEmpty()) {

            View view = ret.getView();
            if (!inCache) {
                if (view.getLayoutParams() == null) {
                    view.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                }

                int widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST);
                int heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);

                ret.getView().measure(widthMeasureSpec, heightMeasureSpec);
            }
            mAdapter.onBindCell(ret, position);


            ret.getView().layout(cellLeft, cellTop,
                cellLeft + width,
                cellTop + height);
        } else {

            int parent = mAdapter.getParentCell(
                    ExcelAdapter.CellPosition.create(x, y)
            );

            int parentX = ExcelAdapter.CellPosition.getX(parent);
            int parentY = ExcelAdapter.CellPosition.getY(parent);

            if (parent == -1) {
                throw new IllegalStateException("must not null");
            }

            if (!mAllCells.containsKey(parent)) {
                int parentLeft = cellLeft;
                for (int dx = x - 1; dx >= parentX; --dx) {
                    parentLeft -= mAdapter.getCellWidth(dx);
                }

                int parentTop = cellTop;
                for (int dy = y - 1; dy >= parentY; --dy) {
                    parentTop -= mAdapter.getCellHeight(dy);
                }

                _makeAndAddCell(parentX, parentY, parentLeft, parentTop);
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

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        final int action = event.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mFlingRunnable.stop();
                break;

            case MotionEvent.ACTION_MOVE:
                int dx = (int)(event.getX() - mLastTouchX);
                int dy = (int)(event.getY() - mLastTouchY);
                _scrollBy(dx, dy);
                break;

            case MotionEvent.ACTION_UP:

                mVelocityTracker.computeCurrentVelocity(1000, mMaxVelocity);

                int velocityX = (int) mVelocityTracker.getXVelocity();
                int velocityY = (int) mVelocityTracker.getYVelocity();

                if (Math.abs(velocityX) > mMinVelocity || Math.abs(velocityY) > mMinVelocity) {
                    mFlingRunnable.start(velocityX, velocityY);
                } else {
                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }
                }
                break;
        }

        mLastTouchX = event.getX();
        mLastTouchY = event.getY();

        return true;
    }

    private void _scrollBy(int dx, int dy) {

        dx = attemptScrollX(dx);
        dy = attemptScrollY(dy);

        _relayoutChildren(dx, dy);

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

        recycle(dx, dy);
    }

    private int attemptScrollX(int dx) {
        if (dx < 0) {
            int right = getRight(mLastVisibleColumn);

            int targetLastVisibleColumn = mLastVisibleColumn;
            final int maxColumn = mAdapter.getColumnCount() - 1;

            int targetRight = right + dx;

            while (targetRight < getMeasuredWidth() && targetLastVisibleColumn < maxColumn) {
                targetLastVisibleColumn++;
                targetRight += mAdapter.getCellWidth(targetLastVisibleColumn);
            }

            if (targetRight >= getMeasuredWidth()) {
                return dx;
            } else {
                return dx + (getMeasuredWidth() - targetRight);
            }
        } else if (dx > 0) {

            int left = getLeft(mFirstVisibleColumn);
            int targetFirstVisibleColumn = mFirstVisibleColumn;
            final int minColumn = 0;

            int targetLeft = left + dx;

            while (targetLeft > 0 && targetFirstVisibleColumn > minColumn) {
                targetFirstVisibleColumn--;

                targetLeft -= mAdapter.getCellWidth(targetFirstVisibleColumn);
            }

            if (targetLeft <= 0) {
                return dx;
            } else {
                return dx - targetLeft;
            }
        }

        return dx;
    }

    private int attemptScrollY(int dy) {
        if (dy < 0) {
            int bottom = getBottom(mLastVisibleRow);
            int targetLastVisibleRow = mLastVisibleRow;
            final int maxRow = mAdapter.getRowCount() - 1;
            int targetBottom = bottom + dy;
            while (targetBottom < getMeasuredHeight() && targetLastVisibleRow < maxRow) {
                targetLastVisibleRow++;
                targetBottom += mAdapter.getCellHeight(targetLastVisibleRow);
            }
            if (targetBottom >= getMeasuredHeight()) {
                return dy;
            } else {
                return dy + (getMeasuredHeight() - targetBottom);
            }
        } else if (dy > 0) {
            int top = getTop(mFirstVisibleRow);
            int targetFirstVisibleRow = mFirstVisibleRow;
            final int minRow = 0;
            int targetTop = top + dy;
            while (targetTop > 0 && targetFirstVisibleRow > minRow) {
                targetFirstVisibleRow--;
                targetTop -= mAdapter.getCellHeight(targetFirstVisibleRow);
            }
            if (targetTop <= 0) {
                return dy;
            } else {
                return dy - targetTop;
            }
        }
        return dy;
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
        int pos = ExcelAdapter.CellPosition.create(x, y);
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
            int parent = mAdapter.getParentCell(pos);

            removeAndRecycleCell(ExcelAdapter.CellPosition.getX(parent),
                ExcelAdapter.CellPosition.getY(parent));

        }


        if (shouldRemove) {
            removeCell(cell);
        }
    }

    private void recycleColumn(int column) {

        int startRow = mFirstVisibleRow;
        int endRow = mLastVisibleRow;

        for (int r = startRow; r <= endRow; ++r) {
            removeAndRecycleCell(column, r);
        }
    }

    private int getMinFullScrollX() {
        return mFixedX + 1;
    }

    private int getMinFullScrollY() {
        return mFixedY + 1;
    }

    private int getMaxFullScrollX() {
        return mAdapter.getColumnCount() - 1;
    }

    private int getMaxFullScrollY() {
        return mAdapter.getRowCount() - 1;
    }

    private int getMinFullScrollLeft() {
        int ret = 0;
        for (int i = 0; i <= mFixedX; ++i) {
            ret += mAdapter.getCellWidth(i);
        }
        return ret;
    }

    private int getMinFullScrollTop() {
        int ret = 0;
        for (int i = 0; i <= mFixedY; ++i) {
            ret += mAdapter.getCellHeight(i);
        }
        return ret;
    }

    private int getMaxFullScrollRight() {
        return getWidth();
    }

    private int getMaxFullScrollBottom() {
        return getHeight();
    }

    private class FlingRunnable implements Runnable {

        private Scroller mScroller;
        private int mLastScrollX;
        private int mLastScrollY;

        private FlingRunnable(Context context) {
            mScroller = new Scroller(context);
        }

        @Override
        public void run() {
            if (mScroller.isFinished()) {
                return;
            }

            if (mScroller.computeScrollOffset()) {
                int curX = mScroller.getCurrX();
                int curY = mScroller.getCurrY();

                int dx = curX - mLastScrollX;
                int dy = curY - mLastScrollY;

                _scrollBy(dx, dy);

                mLastScrollX = curX;
                mLastScrollY = curY;
                ViewCompat.postOnAnimation(ExcelView.this, this);
            }
        }

        private void start(int velocityX, int velocityY) {
            mScroller.fling(0, 0, velocityX, velocityY,
                    Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
            mLastScrollY = 0;
            mLastScrollX = 0;

            ViewCompat.postOnAnimation(ExcelView.this, this);
        }

        private void stop() {
            if (!mScroller.isFinished()) {
                mScroller.forceFinished(true);
            }
        }
    }
}
