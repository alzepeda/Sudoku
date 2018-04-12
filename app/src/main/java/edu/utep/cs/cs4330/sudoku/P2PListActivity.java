package edu.utep.cs.cs4330.sudoku;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;


public class P2PListActivity extends AppCompatActivity {
  public static ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_p2_plist);
        listView = findViewById(R.id.listV);
    }
}
