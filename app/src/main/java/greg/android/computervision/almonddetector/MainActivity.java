package greg.android.computervision.almonddetector;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String LABELS_FILE = "labels.txt";

    private Classifier detector;
    private static final String modelPath = "frozen_inference_graph.pb";

    private Button openGalleryButton;
    private ImageView photoView;
    private TextView peeledTextView, unpeeledTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        openGalleryButton = findViewById(R.id.myGalleryButton);
        photoView = findViewById(R.id.myPhotoView);
        peeledTextView = findViewById(R.id.myPeeledTextView);
        unpeeledTextView = findViewById(R.id.myUnpeeledTextView);

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

    private Bitmap getBitmapFromUri(Uri uri) {
        Bitmap bitmap;

        try {
            ParcelFileDescriptor parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
        } catch (IOException e) {
            Log.e("Exception!!", "Failed to get bitmap from uri", e);
            return null;
        }
        return bitmap;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            final Bitmap bitmap;
            bitmap = getBitmapFromUri(data.getData());

            int srcWidth = bitmap.getWidth();
            int srcHeight = bitmap.getHeight();
            int dstWidth = photoView.getWidth();
            int dstHeight = photoView.getHeight();

            float xScale = (float) dstWidth / srcWidth;
            float yScale = (float) dstHeight / srcHeight;

            float scale = Math.min(xScale, yScale);

            float scaledWidth = scale * srcWidth;
            float scaledHeight = scale * srcHeight;

            Bitmap copyBitmap = Bitmap.createScaledBitmap(bitmap, (int) scaledWidth, (int) scaledHeight, true);

            List<Classifier.Recognition> results = detector.recognizeImage(copyBitmap);

            Canvas canvas = new Canvas(copyBitmap);

            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);

            int peeledAlmonds = 0, unpeeledAlmonds = 0;

            for (Classifier.Recognition result : results) {
                RectF location = result.getLocation();

                if (location != null && result.getConfidence() >= 0.9) {

                    if (result.getTitle().equals("peeled")) {
                        paint.setColor(Color.GREEN);
                        peeledAlmonds++;
                    } else {
                        paint.setColor(Color.RED);
                        unpeeledAlmonds++;
                    }
                    canvas.drawRect(location, paint);
                }
            }
            String peeledText = "Geschählt: " + peeledAlmonds;
            String unpeeledText = "ungeschählt: " + unpeeledAlmonds;
            photoView.setImageBitmap(copyBitmap);
            peeledTextView.setText(peeledText);
            unpeeledTextView.setText(unpeeledText);
        }


        }
}
