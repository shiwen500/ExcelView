package com.seven.www.excelview;

import android.util.SparseArray;

import java.util.Stack;

/**
 * The CellRecycler is used to the cell's reuse.
 *
 * @author chenshiwen
 */

public class CellRecycler {

    private SparseArray<Stack<ExcelAdapter.Cell>> mRecyclers;

    public CellRecycler(int typeCount) {
        mRecyclers = new SparseArray<>(typeCount);
    }

    public void addRecycledCell(ExcelAdapter.Cell cell, int cellType) {
        Stack<ExcelAdapter.Cell> typeCells = mRecyclers.get(cellType);
        if (typeCells == null) {
            typeCells = new Stack<>();
            mRecyclers.put(cellType, typeCells);
        }
        typeCells.push(cell);
    }

    public ExcelAdapter.Cell getRecycledCell(int cellType) {
        Stack<ExcelAdapter.Cell> typeCells = mRecyclers.get(cellType);
        if (typeCells == null) {
            return null;
        }
        try {
            return typeCells.pop();
        } catch (Exception e) {
            return null;
        }
    }

    public void clear() {
        mRecyclers.clear();
    }
}
