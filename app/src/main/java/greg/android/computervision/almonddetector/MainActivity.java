package greg.android.computervision.almonddetector;

import android.content.Intent;
import android.content.res.Resources;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private static final String LABELS_FILE = "labels.txt";

    private Classifier detector;
    private static final String modelPath = "frozen_inference_graph.pb";

    private Button openGalleryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        openGalleryButton = findViewById(R.id.myGalleryButton);

        setModel();

    }

    private void setModel() {
        final Snackbar snackBar = Snackbar.make(
                findViewById(R.id.container),
                "Initializing...",
                Snackbar.LENGTH_INDEFINITE);
        snackBar.show();

        new Thread(() -> {
            try {
                if (detector != null) detector.close();

                detector = Classifier.create(
                        getAssets(), modelPath, LABELS_FILE);
                runOnUiThread(snackBar::dismiss);

            } catch (Exception e) {
                Log.e( "Exception!!","Failed to load inference graph" , e);
                finish();
            }
        }).start();
    }

    public void openGalery(View view){
        Intent myIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(myIntent,100);
    }



}
