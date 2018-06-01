package com.seven.www.excelview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This view show a table likes excel.
 *
 * @author chenshiwen
 */
public class ExcelView extends ViewGroup{

    private static final String TAG = "ExcelView";

    private ExcelAdapter mAdapter;

    /**
     * Simple recycler of cell
     */
    private CellRecycler mCellRecycler;

    /**
     * First visible row of the excel body, exclude the fixed top.
     */
    private int mFirstVisibleRow;

    /**
     * Last visible row of the excel body.
     */
    private int mLastVisibleRow;

    /**
     * First visible column of the excel body, exclude the fixed left
     */
    private int mFirstVisibleColumn;

    /**
     * Last visible column of the excel body
     */
    private int mLastVisibleColumn;

    private FlingRunnable mFlingRunnable;
    private VelocityTracker mVelocityTracker;

    private int mMaxVelocity;
    private int mMinVelocity;

    private float mLastTouchX;
    private float mLastTouchY;

    /**
     * The fixed column, -1 mean no fixed column.
     */
    private int mFixedColumn = -1;

    /**
     * The fixed row, -1 mean no fixed row.
     */
    private int mFixedRow = -1;

    private boolean mNotifyChanged;

    private Drawable mDevider;
    private int mDividerWidth;

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

        mDevider = new ColorDrawable(Color.parseColor("#cfe2f3"));
        mDividerWidth = ViewUtil.dp2px(getResources(), 1.0f);
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed || mNotifyChanged) {
            init();

            mNotifyChanged = false;
        }
    }

    private void init() {

        clearAllCell();

        final int x = getMinBodyColumn();
        final int y = getMinBodyRow();

        _fillDown(x, y, getBodyTop(), getBodyLeft());
        _fillRight(x, y, getBodyTop(),getBodyLeft());

        _fillTopLeft();

        mFirstVisibleRow = y;
        mFirstVisibleColumn = x;

        int lastVisableX = mFirstVisibleColumn;
        int maxX = getMaxBodyColumn();
        int boundWidth = getBodyRight();
        int right = mAdapter.getCellWidth(lastVisableX) + getBodyLeft();

        while (right < boundWidth && lastVisableX < maxX) {
            lastVisableX++;
            right += mAdapter.getCellWidth(lastVisableX);
        }

        mLastVisibleColumn = lastVisableX;
    }

    /**
     * Clear all cell & the cache.
     */
    private void clearAllCell() {
        mAllCells.clear();
        removeAllViews();
        mCellRecycler.clear();
    }

    /**
     * Fill top-left part.
     */
    private void _fillTopLeft() {
        final int maxX = getMinBodyColumn();
        final int maxY = getMinBodyRow();

        int left = 0;
        int top = 0;

        for (int x = 0; x < maxX; ++x) {
            for (int y = 0; y < maxY; ++y) {
                _makeAndAddCell(x, y, left, top);

                top += mAdapter.getCellHeight(y);
            }

            left += mAdapter.getCellWidth(x);
            top = 0;
        }
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
        mAdapter = new ExcelAdapterWrapper(adapter, mDividerWidth);
        mCellRecycler = new CellRecycler(mAdapter.getCellTypeCount());
    }

    public void setFixedXAndY(int fixedX, int fixedY) {
        mFixedColumn = fixedX;
        mFixedRow = fixedY;

        notifyChanged();
    }

    public void notifyChanged() {
        mNotifyChanged = true;
        requestLayout();
    }

    private void removeCell(ExcelAdapter.Cell cell) {
        if (!cell.isEmpty()) {
            detachViewFromParent(cell.getView());
        }
        ExcelAdapter.Cell recycle = mAllCells.remove(cell.getPosition());
        mCellRecycler.addRecycledCell(recycle, mAdapter.getCellType(cell.getPosition()));
    }

    private void addCell(ExcelAdapter.Cell cell, boolean fromCache) {
        if (!cell.isEmpty()) {
            if (fromCache) {
                attachViewToParent(cell.getView(), -1, cell.getView().getLayoutParams());
            } else {
                addView(cell.getView());
            }
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

    private void _fillDown(int x, int y, int nextTop, int nextLeft) {

        int yEnd = getBodyBottom(); // ignore padding
        int maxY = getMaxBodyRow();

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

        int xEnd = getBodyRight();
        int maxX = getMaxBodyColumn();

        while (x <= maxX && left < xEnd) {

            _makeColumn(x, y, top, left);

            left += mAdapter.getCellWidth(x);

            x++;
        }

        mLastVisibleColumn = x - 1;
    }

    private void _fillUp(int x, int y, int bottom, int left) {

        int yStart = getBodyTop(); // ignore padding
        int minY = getMinBodyRow();

        while (y >= minY && bottom > yStart) {

            _makeRow(x, y, bottom - mAdapter.getCellHeight(y), left);

            bottom -= mAdapter.getCellHeight(y);

            y--;
        }

        mFirstVisibleRow = y + 1;
    }

    private void _fillLeft(int x, int y, int top, int right) {

        int xStart = getBodyLeft();
        int minX = getMinBodyColumn();

        while (x >= minX && right > xStart) {

            _makeColumn(x, y, top, right - mAdapter.getCellWidth(x));

            right -= mAdapter.getCellWidth(x);

            --x;
        }

        mFirstVisibleColumn = x + 1;
    }

    private void _makeRow(int startX, int y, int top, int nextLeft) {

        int xEnd = getBodyRight(); // ignore padding.
        int maxX = getMaxBodyColumn();

        while (startX <= maxX && nextLeft < xEnd) {

            _makeAndAddCell(startX, y, nextLeft, top);

            nextLeft += mAdapter.getCellWidth(startX);
            startX++;
        }

        nextLeft = 0;
        for (int x = 0; x < getMinBodyColumn(); ++x) {
            _makeAndAddCell(x, y, nextLeft, top);

            nextLeft += mAdapter.getCellWidth(x);
        }
    }

    private void _makeColumn(int x, int startY, int top, int left) {
        int yEnd = getBodyBottom();
        int maxY = getMaxBodyRow();

        while (startY <= maxY && top < yEnd) {

            _makeAndAddCell(x, startY, left, top);

            top += mAdapter.getCellHeight(startY);
            startY++;
        }

        top = 0;
        for (int y = 0; y < getMinBodyRow(); ++y) {

            _makeAndAddCell(x, y, left, top);
            top += mAdapter.getCellHeight(y);
        }
    }

    private ExcelAdapter.Cell _makeAndAddCell(int x, int y, int cellLeft, int cellTop) {

        int position = ExcelAdapter.CellPosition.create(x, y);

        ExcelAdapter.Cell active = mAllCells.get(position);
        if (active != null) {
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
            view.setTag(R.id.cell_x, x);
            view.setTag(R.id.cell_y, y);

            if (!inCache) {
                if (view.getLayoutParams() == null) {
                    view.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                }
                int widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
                int heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);

                ret.getView().measure(widthMeasureSpec, heightMeasureSpec);

                if (mDividerWidth > 0) {
                    int paddingBottom = view.getPaddingBottom() + mDividerWidth;
                    int paddingRight = view.getPaddingRight() + mDividerWidth;
                    int paddingLeft = view.getPaddingLeft();
                    int paddingTop = view.getPaddingTop();

                    view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
                }
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

        addCell(ret, inCache);

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

        // draw it now.
        invalidate();
    }

    /**
     * Dry run scroll by dx
     *
     * @param dx expect scroll value
     * @return actual value should scroll
     */
    private int attemptScrollX(int dx) {
        if (dx < 0) {
            int right = getRight(mLastVisibleColumn);

            int targetLastVisibleColumn = mLastVisibleColumn;
            final int maxColumn = getMaxBodyColumn();

            int targetRight = right + dx;

            final int maxRight = getBodyRight();

            while (targetRight < maxRight && targetLastVisibleColumn < maxColumn) {
                targetLastVisibleColumn++;
                targetRight += mAdapter.getCellWidth(targetLastVisibleColumn);
            }

            if (targetRight >= maxRight) {
                return dx;
            } else {
                return dx + (maxRight - targetRight);
            }
        } else if (dx > 0) {


            int left = getLeft(mFirstVisibleColumn);
            int targetFirstVisibleColumn = mFirstVisibleColumn;
            final int minColumn = getMinBodyColumn();

            int targetLeft = left + dx;

            final int minLeft = getBodyLeft();

            while (targetLeft > minLeft && targetFirstVisibleColumn > minColumn) {
                targetFirstVisibleColumn--;

                targetLeft -= mAdapter.getCellWidth(targetFirstVisibleColumn);
            }


            if (targetLeft <= minLeft) {
                return dx;
            } else {
                return dx - (targetLeft - minLeft);
            }
        }

        return dx;
    }

    /**
     * Dry run scroll by dy
     *
     * @param dy expect scroll value
     * @return actual value should scroll
     */
    private int attemptScrollY(int dy) {
        if (dy < 0) {
            int bottom = getBottom(mLastVisibleRow);
            int targetLastVisibleRow = mLastVisibleRow;
            final int maxRow = getMaxBodyRow();
            int targetBottom = bottom + dy;
            final int maxY = getBodyBottom();
            while (targetBottom < maxY && targetLastVisibleRow < maxRow) {
                targetLastVisibleRow++;
                targetBottom += mAdapter.getCellHeight(targetLastVisibleRow);
            }
            if (targetBottom >= maxY) {
                return dy;
            } else {
                return dy + (maxY - targetBottom);
            }
        } else if (dy > 0) {
            int top = getTop(mFirstVisibleRow);
            int targetFirstVisibleRow = mFirstVisibleRow;
            final int minRow = getMinBodyRow();
            int targetTop = top + dy;
            final int minY = getBodyTop();
            while (targetTop > minY && targetFirstVisibleRow > minRow) {
                targetFirstVisibleRow--;
                targetTop -= mAdapter.getCellHeight(targetFirstVisibleRow);
            }
            if (targetTop <= minY) {
                return dy;
            } else {
                return dy - (targetTop - minY);
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

        for (ExcelAdapter.Cell cell : mAllCells.values()) {

            if (cell.isEmpty()) {
                continue;
            }

            final int x = cell.getColumn();
            final int y = cell.getRow();
            final View view = cell.getView();

            if (x >= getMinBodyColumn() && x <= getMaxBodyColumn() &&
                    y >= getMinBodyRow() && y <= getMaxBodyRow()) {
                view.offsetLeftAndRight(dx);
                view.offsetTopAndBottom(dy);
                // x & y scroll
            } else if (x < getMinBodyColumn() && y < getMinBodyRow()) {
                // don't move
            } else {
                if (x >= getMinBodyColumn()) {
                    // x scroll
                    view.offsetLeftAndRight(dx);
                } else {
                    // y scroll
                    view.offsetTopAndBottom(dy);
                }
            }
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {

        canvas.save();

        final int x = (int) child.getTag(R.id.cell_x);
        final int y = (int) child.getTag(R.id.cell_y);

        final int minScrollX = getMinBodyColumn();
        final int maxScrollX = getMaxBodyColumn();
        final int minScrollY = getMinBodyRow();
        final int maxScrollY = getMaxBodyRow();

        if (x >= minScrollX && x <= maxScrollX &&
                y >= minScrollY && y <= maxScrollY) {
            Rect rect = new Rect(getBodyLeft(), getBodyTop(),
                    getBodyRight(), getBodyBottom());
            canvas.clipRect(rect);
        } else if (x < minScrollX && y < minScrollY) {
            Rect rect = new Rect(0, 0, getBodyLeft(), getBodyTop());
            canvas.clipRect(rect);
        } else {
            if (x >= minScrollX) {
                Rect rect = new Rect(getBodyLeft(), 0, getBodyRight(), getBodyTop());
                canvas.clipRect(rect);
            } else {
                Rect rect = new Rect(0, getBodyTop(), getBodyLeft(), getBodyBottom());

                canvas.clipRect(rect);
            }
        }
        boolean ret = super.drawChild(canvas, child, drawingTime);

        canvas.restore();
        return ret;
    }

    /**
     * Draw bottom & right divider for a cell
     *
     * @param child the divider's owner
     * @param canvas the canvas
     * @param divider the divider drawable
     */
    private void drawDivider(View child, Canvas canvas, Drawable divider) {
        if (divider == null || mDividerWidth <= 0) {
            return;
        }
        int left = child.getLeft();
        int right = child.getRight();
        int top = child.getTop();
        int bottom = child.getBottom();

        Rect bounds = new Rect(right - mDividerWidth, top, right, bottom);
        divider.setBounds(bounds);
        divider.draw(canvas);

        bounds = new Rect(left, bottom - mDividerWidth, right - mDividerWidth, bottom);
        divider.setBounds(bounds);
        divider.draw(canvas);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {

        int childCount = getChildCount();
        for (int i = 0; i < childCount; ++i) {
            View child = getChildAt(i);
            drawDivider(child, canvas, mDevider);
        }

        super.dispatchDraw(canvas);
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
        int maxY = getMaxBodyRow();

        int minTop = getBodyTop();

        int bottom = getBottom(y);
        while (y <= maxY && bottom <= minTop) {
            recycleRow(y);

            y++;
            bottom += mAdapter.getCellHeight(y);
        }

        mFirstVisibleRow = y;
    }

    private void recycleLeft() {

        int x = mFirstVisibleColumn;
        int maxX = getMaxBodyColumn();
        int minLeft = getBodyLeft();

        int right = getRight(x);

        while (x <= maxX && right <= minLeft) {

            recycleColumn(x);

            x++;
            right += mAdapter.getCellWidth(x);
        }

        mFirstVisibleColumn = x;
    }

    private void recycleBottom() {

        int y = mLastVisibleRow;
        int minY = getMinBodyRow();
        int maxBottom = getBodyBottom();

        int top = getTop(y);
        while (y >= minY && top >= maxBottom) {

            recycleRow(y);

            y--;
            top -= mAdapter.getCellHeight(y);
        }

        mLastVisibleRow = y;
    }

    private void recycleRight() {

        int x = mLastVisibleColumn;
        int minX = getMinBodyColumn();
        int maxRight = getBodyRight();

        int left = getLeft(x);

        while (x >= minX && left >= maxRight) {

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

        for (int c = 0; c < getMinBodyColumn(); ++c) {
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

            if (left >= getBodyRight() ||
                    right <= getBodyLeft() ||
                    top >= getBodyBottom() ||
                    bottom <= getBodyTop()) {
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

        for (int r = 0; r < getMinBodyRow(); ++r) {
            removeAndRecycleCell(column, r);
        }
    }

    private int getMinBodyColumn() {
        return mFixedColumn + 1;
    }

    private int getMinBodyRow() {
        return mFixedRow + 1;
    }

    private int getMaxBodyColumn() {
        return mAdapter.getColumnCount() - 1;
    }

    private int getMaxBodyRow() {
        return mAdapter.getRowCount() - 1;
    }

    private int getBodyLeft() {
        int ret = 0;
        for (int i = 0; i <= mFixedColumn; ++i) {
            ret += mAdapter.getCellWidth(i);
        }
        return ret;
    }

    private int getBodyTop() {
        int ret = 0;
        for (int i = 0; i <= mFixedRow; ++i) {
            ret += mAdapter.getCellHeight(i);
        }
        return ret;
    }

    private int getBodyRight() {
        return getWidth();
    }

    private int getBodyBottom() {
        return getHeight();
    }

    public void setDividerDrawable(Drawable drawable) {
        mDevider = drawable;
        requestLayout();
        invalidate();
    }

    public void setDividerWidth(int dividerWidth) {
        mDividerWidth = dividerWidth;
        requestLayout();
        invalidate();
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

    static class ExcelAdapterWrapper implements ExcelAdapter {

        ExcelAdapter mExcelAdapter;
        int mDividerWidth;

        ExcelAdapterWrapper(ExcelAdapter adapter, int dividerWidth) {
            mExcelAdapter = adapter;
            mDividerWidth = dividerWidth;
        }

        @Override
        public int getRowCount() {
            return mExcelAdapter.getRowCount();
        }

        @Override
        public int getColumnCount() {
            return mExcelAdapter.getColumnCount();
        }

        @Override
        public int getCellType(int position) {
            return mExcelAdapter.getCellType(position);
        }

        @Override
        public int getCellTypeCount() {
            return mExcelAdapter.getCellTypeCount();
        }

        @Override
        public int getCellWidth(int column) {
            return mExcelAdapter.getCellWidth(column) + mDividerWidth;
        }

        @Override
        public int getCellHeight(int row) {
            return mExcelAdapter.getCellHeight(row) + mDividerWidth;
        }

        @Override
        public Cell onCreateCell(ViewGroup parent, int cellType) {
            return mExcelAdapter.onCreateCell(parent, cellType);
        }

        @Override
        public void onBindCell(Cell cell, int cellPosition) {
            mExcelAdapter.onBindCell(cell, cellPosition);
        }

        @Override
        public int getParentCell(int position) {
            return mExcelAdapter.getParentCell(position);
        }
    }
}
