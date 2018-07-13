package greg.android.computervision.almonddetector;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String LABELS_FILE = "labels.txt";
    private static final String modelPath = "frozen_inference_graph-gross.pb";

    private Classifier detector;


    private ImageView photoView;
    private TextView peeledTextView, unpeeledTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        photoView = findViewById(R.id.myPhotoView);
        peeledTextView = findViewById(R.id.myPeeledTextView);
        unpeeledTextView = findViewById(R.id.myUnpeeledTextView);

        setModel();

    }

    private void setModel() {

        try {
            if (detector != null) detector.close();

            detector = Classifier.create(
                    getAssets(), modelPath, LABELS_FILE);

        } catch (Exception ex) {
            Log.e("Exception!!", "Failed to load inference graph", ex);
            finish();
        }
    }

    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.myGalleryButton:
                openGalery();
                break;
            case R.id.myCameraButton:
                startCameraApp();
                break;

        }
    }

    public void openGalery() {
        Intent myIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(myIntent, 1);
    }

    public void startCameraApp() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, 2);
        }
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
            Log.e("Exception!!", "Failed to get bitmap from  uri", e);
            return null;
        }
        return bitmap;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bitmap bitmap = null;
        if (requestCode == 1 && resultCode == RESULT_OK) {
            bitmap = getBitmapFromUri(data.getData());
        } else if (requestCode == 2 && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                Log.d("onActivityResult", String.valueOf(bitmap));
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } else {
            bitmap = null;
        }
        // Return to listening state
        if (bitmap == null) {
            return;
        }

        // Resize image to fir photo view
        Bitmap resizedBitmap = resizeBitmap(bitmap);
        // Detect object
        List<Classifier.Recognition> results = detector.recognizeImage(resizedBitmap);

        drawResults(resizedBitmap, results);


    }

    public Bitmap resizeBitmap(Bitmap bitmap) {
        int srcWidth = bitmap.getWidth();
        int srcHeight = bitmap.getHeight();
        int dstWidth = photoView.getWidth();
        int dstHeight = photoView.getHeight();

        float xScale = (float) dstWidth / srcWidth;
        float yScale = (float) dstHeight / srcHeight;

        float scale = Math.min(xScale, yScale);

        float scaledWidth = scale * srcWidth;
        float scaledHeight = scale * srcHeight;

        return Bitmap.createScaledBitmap(bitmap, (int) scaledWidth, (int) scaledHeight, true);
    }


    public void drawResults(Bitmap bitmap, List<Classifier.Recognition> results) {
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        int peeledAlmonds = 0, unpeeledAlmonds = 0;

        for (Classifier.Recognition result : results) {
            RectF location = result.getLocation();


            if (location != null && result.getConfidence() >= 0.7) {


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
        String peeledText = "Geschält:\t " + peeledAlmonds;
        String unpeeledText = "Ungeschählt:\t " + unpeeledAlmonds;
        photoView.setImageBitmap(bitmap);
        peeledTextView.setText(peeledText);
        unpeeledTextView.setText(unpeeledText);

        }

}

