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
    int getCellType(CellPosition position);

    /**
     * The count of cell type.
     *
     * @return cell type's count
     */
    int getCellTypeCount();

    int getCellWidth(int column);

    int getCellHeight(int row);

    Cell onCreateCell(ViewGroup parent, int cellType);
    void onBindCell(Cell cell, CellPosition cellPosition);


    CellPosition getParentCell(CellPosition position);

    class CellPosition extends Point {
        public static CellPosition create(int x, int y) {
            CellPosition pos = new CellPosition();
            pos.x = x;
            pos.y = y;
            return pos;
        }
    }

    class Cell {

        /**
         * The coordinate of this cell.
         */
        private CellPosition mCellPosition;

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
            return mCellPosition.y;
        }

        public int getColumn(){
            return mCellPosition.x;
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
            mCellPosition = null;
        }

        public void setCellPosition(CellPosition position) {
            mCellPosition = position;
        }

        public CellPosition getPosition() {
            return mCellPosition;
        }

        public Cell(View view, CellPosition position, int mergeWidth, int mergeHeight) {
            mView = view;
            mCellPosition = position;
            mMergeWidth = mergeWidth;
            mMergeHeight = mergeHeight;
        }

        public Cell(View view, CellPosition position) {
            mView = view;
            mCellPosition = position;
            mMergeWidth = 1;
            mMergeHeight = 1;
        }

        public static Cell createEmptyCell(CellPosition position) {
            return new Cell(null, position, 0, 0);
        }
    }
}
