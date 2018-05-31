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
    };

    private static final int INDEX_SIMPLE_FULL_SCROLL_TABLE = 0;
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
                            //.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                            //.setCustomAnimations(R.animator.animator_fragment_enter, R.animator.animator_fragment_enter, R.animator.animator_fragment_popenter, R.animator.animator_fragment_popexit)
                            .replace(R.id.fl_main, SimpleFullScrollTableFragment.newInstance(title))
                            .addToBackStack(null)
                            .commit();
                    break;
            }
        }
    }
}
