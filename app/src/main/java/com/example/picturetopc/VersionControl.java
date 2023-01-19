package com.example.picturetopc;

import android.os.Build;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

class Data{
    public String url;
    public String assets_url;
    public String upload_url;
    public String html_url;
    public int id;
    public Object author;
    public String node_id;
    public String tag_name;
    public String target_commitish;
    public String name;
    public boolean draft;
    public boolean prerelease;
    public Object created_at;
    public Object published_at;
    public ArrayList<Object> assets;
    public String tarball_url;
    public String zipball_url;
    public String body;
}

interface GitHubService {
    @GET("repos/{user}/{repo}/releases/latest")
    Call<Data> listRepos(@Path("user") String user, @Path("repo") String repo);
}

public class VersionControl {
    private Retrofit retrofit = new Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.github.com/")
            .build();
    private GitHubService service = retrofit.create(GitHubService.class);


    void getRequest(Runnable func){
        Call<Data> call = service.listRepos(Settings.AUTHOR, Settings.REPOSITORY);

        call.enqueue(new onData(func));
    }

    class onData implements Callback {

        private final Runnable func;

        public onData(Runnable callback){
            func = callback;
        }

        @Override
        public void onResponse(Call call, Response response) {

            if (response.isSuccessful()) {
                if (response.body() != null) {
                    String[] onlineVersion = ((Data)response.body()).tag_name.substring(1).split("\\.");
                    String[] localVersion = BuildConfig.VERSION_NAME.replaceFirst("v", "").split("\\.");
                    Log.d("Online", ((Data)response.body()).tag_name);
                    Log.d("Local", BuildConfig.VERSION_NAME);
                    if (Integer.parseInt(onlineVersion[0]) > Integer.parseInt(localVersion[0]) ||
                            (Objects.equals(onlineVersion[0], localVersion[0]) && Integer.parseInt(onlineVersion[1]) > Integer.parseInt(localVersion[1]))  ||
                            (Objects.equals(onlineVersion[0], localVersion[0]) && Objects.equals(onlineVersion[1], localVersion[1]) && Integer.parseInt(onlineVersion[2]) > Integer.parseInt(localVersion[2]))
                    )
                    {
                        func.run();
                    }
                }
            }
        }

        @Override
        public void onFailure(Call call, Throwable t) {

        }
    }
}
