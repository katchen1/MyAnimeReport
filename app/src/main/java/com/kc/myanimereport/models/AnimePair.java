package com.kc.myanimereport.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;

/* PairCount (Parse model). For storing intermediate data for Slope One. */
@ParseClassName("AnimePair")
public class AnimePair extends ParseObject {
    public static final String KEY_MEDIA_ID1 = "mediaId1";
    public static final String KEY_MEDIA_ID2 = "mediaId2";
    public static final String KEY_COUNT = "count";
    public static final String KEY_DIFF_SUM = "diffSum";

    /* Default constructor required by Parse. */
    public AnimePair() { }

    public Integer getMediaId1() {
        return getInt(KEY_MEDIA_ID1);
    }

    public void setMediaId1(Integer mediaId) {
        put(KEY_MEDIA_ID1, mediaId);
    }

    public Integer getMediaId2() {
        return getInt(KEY_MEDIA_ID2);
    }

    public void setMediaId2(Integer mediaId) {
        put(KEY_MEDIA_ID2, mediaId);
    }

    public Integer getCount() {
        return getInt(KEY_COUNT);
    }

    public void setCount(Integer count) {
        put(KEY_COUNT, count);
    }

    public Double getDiffSum() {
        return getDouble(KEY_DIFF_SUM);
    }

    public void setDiffSum(Double diffSum) {
        put(KEY_DIFF_SUM, diffSum);
    }
}