package com.telenav.osv.http;

import com.android.volley.AuthFailureError;
import com.android.volley.toolbox.StringRequest;
import com.telenav.osv.listener.network.GenericResponseListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.entity.mime.MultipartEntityBuilder;

/**
 * Created by Kalman on 10/6/2015.
 */
@SuppressWarnings("HardCodedStringLiteral")
public class DeleteSequenceRequest extends StringRequest {

  private static final String PARAM_SEQUENCE_ID = "sequenceId";

  private static final String PARAM_TOKEN = "access_token";

  private final GenericResponseListener mListener;

  private final int mSequenceId;

  private final String mToken;

  protected Map<String, String> headers;

  private MultipartEntityBuilder mBuilder = MultipartEntityBuilder.create();

  public DeleteSequenceRequest(String url, GenericResponseListener listener, int sequenceId, String token) {
    super(Method.POST, url, listener, listener);
    mToken = token;
    mSequenceId = sequenceId;
    mListener = listener;
  }

  @Override
  public Map<String, String> getHeaders() throws AuthFailureError {
    Map<String, String> headers = super.getHeaders();

    if (headers == null || headers.equals(Collections.emptyMap())) {
      headers = new HashMap<>();
    }

    headers.put("Accept", "application/json");

    return headers;
  }

  @Override
  protected Map<String, String> getParams() {
    Map<String, String> params = new HashMap<>();
    params.put(PARAM_SEQUENCE_ID, mSequenceId + "");
    params.put(PARAM_TOKEN, mToken);
    return params;
  }

  @Override
  protected void deliverResponse(String response) {
    mListener.onResponse(response);
  }
}