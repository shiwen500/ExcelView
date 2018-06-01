package com.seven.www.excelview;

import android.content.res.Resources;

/**
 * The ViewUtil is a helper for view.
 *
 * @author chenshiwen
 */
public class ViewUtil {

    /**
     * Calculate real pixel for provided dp value.
     *
     * @param res app resources
     * @param dp dp which should be convert
     * @return px value
     */
    public static int dp2px(Resources res, float dp) {
        return (int)(res.getDisplayMetrics().density * dp + 0.5f);
    }
}
