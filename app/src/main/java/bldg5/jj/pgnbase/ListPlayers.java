package bldg5.jj.pgnbase;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.HashMap;

import bldg5.jj.pgnbase.adapters.ApiEndpoint;
import bldg5.jj.pgnbase.adapters.ListViewAdapter;
import bldg5.jj.pgnbase.common.Error;
import bldg5.jj.pgnbase.common.Game;
import bldg5.jj.pgnbase.common.Games;
import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ListPlayers extends AppCompatActivity {
    private static final String tag = "ListPlayers.java";
    // must end in a forward slash
    public static final String BASE_URL = "http://ec2-54-158-98-180.compute-1.amazonaws.com:8080/find_player/";
    private Games results;
    @BindView(R2.id.playersList) ListView listView;
    @BindView(R2.id.progressBar) ProgressBar pg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);

        Intent intent = this.getIntent();
        final String strWhite = (String) intent.getStringExtra("White");
        final String strBlack = (String) intent.getStringExtra("Black");
        final int nIC = (int) intent.getIntExtra("IgnoreColor", 0);

        // final ListView listView = (ListView) findViewById(R.id.playersList);
        getPlayers(listView, strWhite, strBlack, nIC);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
                Bundle params = new Bundle();

                String player_name = ((TextView) view.findViewById(R.id.txtName)).getText().toString();
                params.putString("SelectedPlayer", player_name);
                params.putInt("IgnoreColor", nIC);
                params.putSerializable("DisplayedGames", results);

                Intent navListGames = new Intent(ListPlayers.this, ListGames.class);
                navListGames.putExtras(params);
                ListPlayers.this.startActivity(navListGames);
            }
        });
    }

    public void getPlayers(final ListView lv, final String white, final String black, final int nIC) {
        if (white.equals("") && black.equals("")) {
            HashMap<String, String> hashMap = new HashMap<String, String>() {};
            hashMap.put("Please check your connection or search criteria.", "No games were found.");

            ListViewAdapter playersListAdapter = new ListViewAdapter(hashMap);
            lv.setAdapter(playersListAdapter);

            pg.setVisibility(View.GONE);
        } else {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiEndpoint service = retrofit.create(ApiEndpoint.class);
            Call<Games> call = service.getPlayers(white, black, nIC);

            call.enqueue(new Callback<Games>() {
                @Override
                public void onResponse(Call<Games> call, Response<Games> response) {
                    boolean bSuccess = response.isSuccessful();

                    HashMap<String, String> hashMap = new HashMap<String, String>() {};
                    if (bSuccess) {
                        results = response.body();

                        // the below code assumes the Games come in ordered by white
                        // the result will be that the number of games is listed next to the player.
                        String white = "";
                        int nNumGames = 0;
                        for(final Game g: results.listGames) {
                            String gWhite = g.getWhite();
                            nNumGames = (gWhite.equals(white)) ? (nNumGames + 1) : 1;
                            hashMap.put(gWhite, "Number of Games: " + String.valueOf(nNumGames));
                            white = gWhite;
                        }

                        if (nNumGames == 0) {
                            hashMap.put("Please check your connection or search criteria.", "No games were found.");
                        }
                    } else
                        hashMap.put("Please check your connection or search criteria.", "No games were found.");

                    ListViewAdapter playersListAdapter = new ListViewAdapter(hashMap);
                    lv.setAdapter(playersListAdapter);

                    pg.setVisibility(View.GONE);
                }

                @Override
                public void onFailure(Call<Games> call, Throwable t) {
                    HashMap<String, String> hashMap = new HashMap<String, String>() {};
                    hashMap.put("Please check your connection or search criteria.", "No games were found.");

                    ListViewAdapter playersListAdapter = new ListViewAdapter(hashMap);
                    lv.setAdapter(playersListAdapter);

                    // Log.e(tag, t.toString());
                    Error.sendError(t.toString());

                    pg.setVisibility(View.GONE);
                }
            });
        }
    }
}
