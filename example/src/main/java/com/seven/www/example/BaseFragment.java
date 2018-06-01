package com.seven.www.example;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.seven.www.excelview.ExcelAdapter;
import com.seven.www.excelview.ExcelView;

/**
 * Created by chenshiwen on 18-6-1.
 */
public class BaseFragment extends Fragment {

    private String mTitle;
    protected ExcelView mExcelView;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mTitle = getArguments().getString(MainListFragment.TITLE_SUB_FRAGMENT);
        getActivity().setTitle(mTitle);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_simple_excelview, container, false);
        mExcelView = (ExcelView) rootView.findViewById(R.id.ev_simple_fixed_lefttop_table);
        ExcelAdapter excelAdapter = onCreateAdapter();
        if (excelAdapter != null) {
            mExcelView.setExcelAdapter(excelAdapter);
        }
        return rootView;
    }

    public ExcelAdapter onCreateAdapter() {
        return null;
    }
}
