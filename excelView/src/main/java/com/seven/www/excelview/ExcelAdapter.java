package com.seven.www.excelview;

import android.graphics.Point;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * This interface provide some methods for {@link ExcelView} to
 * access the data items & cell's dimension of the excel.
 *
 * @author chenshiwen
 */

public interface ExcelAdapter {

    /**
     * The type of cell is only a child of another cell
     */
    int CELL_TYPE_EMPTY = -1;

    /**
     * Provides how many rows the excel body should show, exclude fixed header.
     *
     * @return count of row
     */
    int getRowCount();

    /**
     * Provides how many columns the excel body should show, exclude fixed left.
     *
     * @return count of column
     */
    int getColumnCount();

    /**
     * The cell's type for the position.
     *
     * @param position cell's position
     * @return cell's type
     */
    int getCellType(int position);

    /**
     * The count of cell type.
     *
     * @return cell type's count
     */
    int getCellTypeCount();

    int getCellWidth(int column);

    int getCellHeight(int row);

    Cell onCreateCell(ViewGroup parent, int cellType);
    void onBindCell(Cell cell, int cellPosition);


    int getParentCell(int position);

    class CellPosition {

        private CellPosition(){}

        public static final int MASK = 0xffff;

        public static int getX(int pos) {
            return pos >> 16;
        }

        public static int getY(int pos) {
            return pos & MASK;
        }

        public static int create(int x, int y) {
            return (x << 16) + (y & MASK);
        }
    }

    class Cell {

        /**
         * The coordinate of this cell.
         */
        private int mCellPosition;

        /**
         * This cell's view, nullable if it is subcell of others.
         */
        private View mView;

        private int mMergeWidth;

        private int mMergeHeight;

        public View getView() {
            return mView;
        }

        public int getRow(){
            return CellPosition.getY(mCellPosition);
        }

        public int getColumn(){
            return CellPosition.getX(mCellPosition);
        }

        public int getMergeWidth() {
            return mMergeWidth;
        }

        public int getMergeHeight() {
            return mMergeHeight;
        }

        public boolean isEmpty() {
            return mMergeWidth == 0 || mMergeHeight == 0;
        }

        public void recycle() {
            mCellPosition = 0;
        }

        public void setCellPosition(int position) {
            mCellPosition = position;
        }

        public int getPosition() {
            return mCellPosition;
        }

        public Cell(View view, int position, int mergeWidth, int mergeHeight) {
            mView = view;
            mCellPosition = position;
            mMergeWidth = mergeWidth;
            mMergeHeight = mergeHeight;
        }

        public Cell(View view, int position) {
            mView = view;
            mCellPosition = position;
            mMergeWidth = 1;
            mMergeHeight = 1;
        }

        public static Cell createEmptyCell(int position) {
            return new Cell(null, position, 0, 0);
        }
    }
}
