package vn.bnh.oim.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.octetstring.vde.util.Base64;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;

import java.io.IOException;
import java.text.SimpleDateFormat;

public class RESTUtil {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ODLLogger logger = ODLLogger.getODLLogger(RESTUtil.class.getName());
    private static final String FROM_ID = "IDM";
    private static final String FROM_NAME = "IDM";
    private static final SimpleDateFormat ID_FORMAT = new SimpleDateFormat("yyyyMMddHH24mmss");

    public static JsonNode sendRequest(String url, String username, String password, String requestBody) throws IOException {
        logger.log(ODLLevel.INFO, "send request {0} to {1}", new Object[]{"POST", url});
        String authString = Base64.encode(username.concat(":").concat(password).getBytes());
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url).method("POST", RequestBody.create(requestBody.getBytes())).addHeader("Authorization", authString)
                .build(); // defaults to GET
        Response response = client.newCall(request).execute();
        String responseBody = response.body().string();
        logger.log(ODLLevel.INFO, "Request Response: {0}", responseBody);
        return mapper.readTree(responseBody);
    }


}
