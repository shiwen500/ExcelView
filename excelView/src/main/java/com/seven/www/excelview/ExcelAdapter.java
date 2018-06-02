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

    /**
     * The width in pixel of the cell
     *
     * @param column the column index, from 0 on
     * @return the width value in pixel
     */
    int getCellWidth(int column);

    /**
     * The height in pixel of the row
     *
     * @param row the row index , from 0 on
     * @return the width value in pixel
     */
    int getCellHeight(int row);

    /**
     * Create a cell by the type
     *
     * @param parent the cell's parent view, should be ExcelView
     * @param cellType the cell's type
     * @return The created cell
     */
    Cell onCreateCell(ViewGroup parent, int cellType);

    /**
     * Initialize the cell in the given position
     *
     * @param cell the cell should be initialized
     * @param cellPosition the given position
     */
    void onBindCell(Cell cell, int cellPosition);

    /**
     * Get the parent cell's pos of the given pos. A cell which had a parent cell
     * (Didn't return -1) should be empty.
     *
     * @param position the given position of the empty cell
     * @return the parent cell's pos
     */
    int getParentCell(int position);

    /**
     * We use a 32 bits integer express a cell's pos, high 16 bits express column and
     * low 16 bits express row.
     */
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

        public static Cell empty() {
            return new Cell(null, 0, 0);
        }

        /**
         * The position of this cell.
         */
        private int mCellPosition;

        /**
         * This cell's view, null if it is subcell of others.
         */
        private View mView;

        /**
         * Greater than 1 if the cell is other cell's parent cell.
         */
        private int mMergeWidth;

        /**
         * Greater than 1 if the cell is other cell's parent cell.
         */
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

        /*package*/ void setCellPosition(int position) {
            mCellPosition = position;
        }

        public int getPosition() {
            return mCellPosition;
        }

        public Cell(View view) {
            this(view, 1, 1);
        }

        public Cell(View view, int mergeWidth, int mergeHeight) {
            mView = view;
            mMergeHeight = mergeHeight;
            mMergeWidth = mergeWidth;
        }
    }
}
