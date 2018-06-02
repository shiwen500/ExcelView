package com.seven.www.example;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * Created by chenshiwen on 18-5-31.
 */

public class MainListFragment extends Fragment {

    private final String[] mLists = new String[]{
        "Simple Full Scroll Table",
        "Simple Fixed LeftTop Table",
        "Simple Picture Wall",
    };

    private static final int INDEX_SIMPLE_FULL_SCROLL_TABLE = 0;
    private static final int INDEX_SIMPLE_FIXED_LEFTTOP_TABLE = 1;
    private static final int INDEX_SIMPLE_PICTURE_WALL = 2;
    public static final String TITLE_SUB_FRAGMENT = "title_sub_fragment";

    private ListView mListView;
    private ListAdapter mAdapter;

    public MainListFragment(){}

    public static MainListFragment newInstance() {
        return new MainListFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle("ExcelView Demo List");
        View rootView = inflater.inflate(R.layout.fragment_main_list, container, false);
        mListView = (ListView) rootView.findViewById(R.id.lv_main);
        mAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1, mLists);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new OnItemClickHandler());
        return rootView;
    }

    private class OnItemClickHandler implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            FragmentManager fm = getFragmentManager();
            String title = mLists[position];

            switch (position) {
                case INDEX_SIMPLE_FULL_SCROLL_TABLE:
                    fm.beginTransaction()
                            .replace(R.id.fl_main, SimpleFullScrollTableFragment.newInstance(title))
                            .addToBackStack(null)
                            .commit();
                    break;
                case INDEX_SIMPLE_FIXED_LEFTTOP_TABLE:
                    fm.beginTransaction()
                        .replace(R.id.fl_main, SimpleFixedLeftTopTable.newInstance(title))
                        .addToBackStack(null)
                        .commit();
                    break;
                case INDEX_SIMPLE_PICTURE_WALL:
                    fm.beginTransaction()
                        .replace(R.id.fl_main, SimplePictureWallFragment.newInstance(title))
                        .addToBackStack(null)
                        .commit();
                    break;
            }
        }
    }
}
