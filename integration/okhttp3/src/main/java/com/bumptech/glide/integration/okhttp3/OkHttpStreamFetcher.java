package com.bumptech.glide.integration.okhttp3;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.HttpException;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.util.ContentLengthInputStream;
import com.bumptech.glide.util.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Call.Factory;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Fetches an {@link InputStream} using the okhttp library.
 */
public class OkHttpStreamFetcher implements DataFetcher<InputStream>, okhttp3.Callback {
  private static final String TAG = "OkHttpFetcher";
  private final Call.Factory client;
  private final GlideUrl url;
  private InputStream stream;
  private ResponseBody responseBody;
  private DataCallback<? super InputStream> callback;
  // call may be accessed on the main thread while the object is in use on other threads. All other
  // accesses to variables may occur on different threads, but only one at a time.
  private volatile Call call;

  private final int width;
  private final int height;
  @Nullable private final RequestBuilderInterceptor interceptor;

  // Public API.
  @SuppressWarnings("WeakerAccess")
  public OkHttpStreamFetcher(
      Factory client, GlideUrl url, int width, int height,
      @Nullable RequestBuilderInterceptor interceptor
  ) {
    this.client = client;
    this.url = url;
    this.width = width;
    this.height = height;
    this.interceptor = interceptor;
  }

  @Override
  public void loadData(@NonNull Priority priority,
      @NonNull final DataCallback<? super InputStream> callback) {
    Request.Builder requestBuilder = new Request.Builder().url(url.toStringUrl());
    for (Map.Entry<String, String> headerEntry : url.getHeaders().entrySet()) {
      String key = headerEntry.getKey();
      requestBuilder.addHeader(key, headerEntry.getValue());
    }
    if (interceptor != null) {
      interceptor.interceptBuilder(requestBuilder, width, height);
    }
    Request request = requestBuilder.build();
    this.callback = callback;

    call = client.newCall(request);
    call.enqueue(this);
  }

  @Override
  public void onFailure(@NonNull Call call, @NonNull IOException e) {
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "OkHttp failed to obtain result", e);
    }

    callback.onLoadFailed(e);
  }

  @Override
  public void onResponse(@NonNull Call call, @NonNull Response response) {
    responseBody = response.body();
    if (response.isSuccessful()) {
      long contentLength = Preconditions.checkNotNull(responseBody).contentLength();
      stream = ContentLengthInputStream.obtain(responseBody.byteStream(), contentLength);
      callback.onDataReady(stream);
    } else {
      callback.onLoadFailed(new HttpException(response.message(), response.code()));
    }
  }

  @Override
  public void cleanup() {
    try {
      if (stream != null) {
        stream.close();
      }
    } catch (IOException e) {
      // Ignored
    }
    if (responseBody != null) {
      responseBody.close();
    }
    callback = null;
  }

  @Override
  public void cancel() {
    Call local = call;
    if (local != null) {
      local.cancel();
    }
  }

  @NonNull
  @Override
  public Class<InputStream> getDataClass() {
    return InputStream.class;
  }

  @NonNull
  @Override
  public DataSource getDataSource() {
    return DataSource.REMOTE;
  }
}
