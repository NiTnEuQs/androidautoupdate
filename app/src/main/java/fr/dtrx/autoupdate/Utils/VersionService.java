package fr.dtrx.autoupdate.Utils;

import com.google.gson.JsonElement;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface VersionService {

    String ENDPOINT = "http://sn-prod4.com/versioning/api/";

    VersionService service = RetrofitClient.with(ENDPOINT).create(VersionService.class);

    @GET("versions/{id}")
    Call<JsonElement> getVersion(@Path("id") String id);

}
