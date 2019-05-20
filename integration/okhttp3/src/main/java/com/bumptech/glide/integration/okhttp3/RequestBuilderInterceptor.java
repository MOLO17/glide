package com.bumptech.glide.integration.okhttp3;

import android.support.annotation.NonNull;
import okhttp3.Request;

/**
 * Created by Damiano Giusti on 2019-05-20.
 */
public interface RequestBuilderInterceptor {

  @NonNull Request.Builder interceptBuilder(@NonNull Request.Builder builder, int width, int height);
}
