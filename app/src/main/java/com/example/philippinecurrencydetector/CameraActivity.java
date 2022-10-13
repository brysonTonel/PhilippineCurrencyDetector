package com.example.philippinecurrencydetector;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.philippinecurrencydetector.database.model.CurrencyModel;
import com.example.philippinecurrencydetector.database.repositories.CurrencyRepo;
import com.example.philippinecurrencydetector.env.ImageUtils;
import com.example.philippinecurrencydetector.env.Logger;
import com.google.android.material.bottomsheet.BottomSheetBehavior;



import org.tensorflow.lite.examples.classification.tflite.Classifier;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;


public abstract class CameraActivity extends AppCompatActivity
        implements ImageReader.OnImageAvailableListener,
        Camera.PreviewCallback,
        View.OnClickListener,
        AdapterView.OnItemSelectedListener {
    private static final Logger LOGGER = new Logger();

    private static final int PERMISSIONS_REQUEST = 1;


    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    protected int previewWidth = 0;
    protected int previewHeight = 0;
    private Handler handler;
    private HandlerThread handlerThread;
    private boolean useCamera2API;
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private int yRowStride;
    private Runnable postInferenceCallback;
    private Runnable imageConverter;
    private LinearLayout bottomSheetLayout;
    private LinearLayout gestureLayout;
    private BottomSheetBehavior<LinearLayout> sheetBehavior;
    protected TextView recognitionTextView,
            recognition1TextView,
            recognition2TextView,
            recognition3TextView,
            recognition4TextView,
            recognitionValueTextView,
            recognition1ValueTextView,
            recognition2ValueTextView,
            recognition3ValueTextView,
            recognition4ValueTextView;
    protected TextView frameValueTextView,
            cropValueTextView,
            cameraResolutionTextView,
            rotationTextView,
            inferenceTimeTextView;
    protected ImageView bottomSheetArrowImageView;
    private RecyclerView recyclerView;

    CameraActivityViewModel viewModel;
    private Classifier.Model model = Classifier.Model.QUANTIZED_MOBILENET;
    private Classifier.Device device = Classifier.Device.CPU;
    private int numThreads = -1;
    MediaPlayer mp1,mp5,mp10,mp20,mp50,mp100,mp200,mp500,mp1000;


    private CurrencyAdapter adapter;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        LOGGER.d("onCreate " + this);
        super.onCreate(null);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);



        if(Build.VERSION.SDK_INT >= 23) {
            ExternalStoragePermissions.verifyStoragePermissions(this);
        }
        viewModel = ViewModelProviders.of(
                this,
                new ModelFactory(this.getApplication())
        ).get(CameraActivityViewModel.class);

        viewModel.loadCurrencyItem();
            observers();


        if (hasPermission()) {
            setFragment();
        } else {
            requestPermission();
        }


        recyclerView = findViewById(R.id.rv_currency);
        bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
        gestureLayout = findViewById(R.id.gesture_layout);
        sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow);


        adapter = new CurrencyAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, layoutManager.getOrientation()));
        recyclerView.setAdapter(adapter);

        ViewTreeObserver vto = gestureLayout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                            gestureLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        } else {
                            gestureLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                        //                int width = bottomSheetLayout.getMeasuredWidth();
                        int height = gestureLayout.getMeasuredHeight();

                        sheetBehavior.setPeekHeight(height);
                    }
                });
        sheetBehavior.setHideable(false);

        sheetBehavior.setBottomSheetCallback(
                new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                        switch (newState) {
                            case BottomSheetBehavior.STATE_HIDDEN:
                                break;
                            case BottomSheetBehavior.STATE_EXPANDED:
                            {
                                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);
                            }
                            break;
                            case BottomSheetBehavior.STATE_COLLAPSED:
                            {
                                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                            }
                            break;
                            case BottomSheetBehavior.STATE_DRAGGING:
                                break;
                            case BottomSheetBehavior.STATE_SETTLING:
                                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                                break;
                        }
                    }

                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
                });

        recognitionTextView = findViewById(R.id.detected_item);
        recognitionValueTextView = findViewById(R.id.detected_item_value);

        recognition1TextView = findViewById(R.id.detected_item1);
        recognition1ValueTextView = findViewById(R.id.detected_item1_value);

        recognition2TextView = findViewById(R.id.detected_item2);
        recognition2ValueTextView = findViewById(R.id.detected_item2_value);

        recognition3TextView = findViewById(R.id.detected_item3);
        recognition3ValueTextView = findViewById(R.id.detected_item3_value);

        recognition4TextView = findViewById(R.id.detected_item4);
        recognition4ValueTextView = findViewById(R.id.detected_item4_value);

    }

    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }


    private void observers() {
        viewModel.getMasterItem().observe(this, this::renderCurrencyItem);
    }

    private void renderCurrencyItem(List<CurrencyModel> currencyItem) {
        Log.e("size",String.valueOf(currencyItem.size()));
        adapter.setCurrencyModelsItem(currencyItem);

    }

    protected int getLuminanceStride() {
        return yRowStride;
    }

    protected byte[] getLuminance() {
        return yuvBytes[0];
    }

    /** Callback for android.hardware.Camera API */
    @Override
    public void onPreviewFrame(final byte[] bytes, final Camera camera) {
        if (isProcessingFrame) {
            LOGGER.w("Dropping frame!");
            return;
        }

        try {
            // Initialize the storage bitmaps once when the resolution is known.
            if (rgbBytes == null) {
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                previewHeight = previewSize.height;
                previewWidth = previewSize.width;
                rgbBytes = new int[previewWidth * previewHeight];
                onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
            }
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            return;
        }

        isProcessingFrame = true;
        yuvBytes[0] = bytes;
        yRowStride = previewWidth;

        imageConverter =
                new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
                    }
                };

        postInferenceCallback =
                new Runnable() {
                    @Override
                    public void run() {
                        camera.addCallbackBuffer(bytes);
                        isProcessingFrame = false;
                    }
                };
        processImage();
    }

    /** Callback for Camera2 API */
    @Override
    public void onImageAvailable(final ImageReader reader) {
        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }
        if (rgbBytes == null) {
            rgbBytes = new int[previewWidth * previewHeight];
        }
        try {
            final Image image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            if (isProcessingFrame) {
                image.close();
                return;
            }
            isProcessingFrame = true;
            Trace.beginSection("imageAvailable");
            final Image.Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            imageConverter =
                    new Runnable() {
                        @Override
                        public void run() {
                            ImageUtils.convertYUV420ToARGB8888(
                                    yuvBytes[0],
                                    yuvBytes[1],
                                    yuvBytes[2],
                                    previewWidth,
                                    previewHeight,
                                    yRowStride,
                                    uvRowStride,
                                    uvPixelStride,
                                    rgbBytes);
                        }
                    };

            postInferenceCallback =
                    new Runnable() {
                        @Override
                        public void run() {
                            image.close();
                            isProcessingFrame = false;
                        }
                    };

            processImage();
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            Trace.endSection();
            return;
        }
        Trace.endSection();
    }

    @Override
    public synchronized void onStart() {
        LOGGER.d("onStart " + this);
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        LOGGER.d("onResume " + this);
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        mp1 = MediaPlayer.create(this, R.raw.one);
        mp5 = MediaPlayer.create(this, R.raw.five);
        mp10 = MediaPlayer.create(this, R.raw.ten);
        mp20 = MediaPlayer.create(this, R.raw.twenty);
        mp50 = MediaPlayer.create(this, R.raw.fifthy);
        mp100 = MediaPlayer.create(this, R.raw.onehundred);
        mp200 = MediaPlayer.create(this, R.raw.twohundred);
        mp500 = MediaPlayer.create(this, R.raw.fivehundred);
        mp1000 = MediaPlayer.create(this, R.raw.onethousand);
    }

    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }

        super.onPause();
    }

    @Override
    public synchronized void onStop() {
        LOGGER.d("onStop " + this);
        super.onStop();
    }

    @Override
    public synchronized void onDestroy() {
        LOGGER.d("onDestroy " + this);
        super.onDestroy();
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            if (allPermissionsGranted(grantResults)) {
                setFragment();
            } else {
                requestPermission();
            }
        }
    }

    private static boolean allPermissionsGranted(final int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
                Toast.makeText(
                        CameraActivity.this,
                        "Camera permission is required for this demo",
                        Toast.LENGTH_LONG)
                        .show();
            }
            requestPermissions(new String[] {PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
        }
    }



    // Returns true if the device supports the required hardware level, or better.
    private boolean isHardwareLevelSupported(
            CameraCharacteristics characteristics, int requiredLevel) {
        int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            return requiredLevel == deviceLevel;
        }
        // deviceLevel is not LEGACY, can use numerical sort
        return requiredLevel <= deviceLevel;
    }

    private String chooseCamera() {
        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                final StreamConfigurationMap map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (map == null) {
                    continue;
                }

                // Fallback to camera1 API for internal cameras that don't have full support.
                // This should help with legacy situations where using the camera2 API causes
                // distorted or otherwise broken previews.
                useCamera2API =
                        (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                                || isHardwareLevelSupported(
                                characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
                LOGGER.i("Camera API lv2?: %s", useCamera2API);
                return cameraId;
            }
        } catch (CameraAccessException e) {
            LOGGER.e(e, "Not allowed to access camera");
        }

        return null;
    }

    protected void setFragment() {
        String cameraId = chooseCamera();

        Fragment fragment;
        if (useCamera2API) {
            CameraConnectionFragment camera2Fragment =
                    CameraConnectionFragment.newInstance(
                            new CameraConnectionFragment.ConnectionCallback() {
                                @Override
                                public void onPreviewSizeChosen(final Size size, final int rotation) {
                                    previewHeight = size.getHeight();
                                    previewWidth = size.getWidth();
                                    CameraActivity.this.onPreviewSizeChosen(size, rotation);
                                }
                            },
                            this,
                            getLayoutId(),
                            getDesiredPreviewFrameSize());

            camera2Fragment.setCamera(cameraId);
            fragment = camera2Fragment;
        } else {
            fragment =
                    new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
        }

        getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }
    boolean one = false;
    boolean five = false;
    boolean ten = false;
    boolean twenty = false;
    boolean fifthy = false;
    boolean onehundred = false;
    boolean twohundred = false;
    boolean fivehundred = false;
    boolean onethousand = false;

    @UiThread
    protected void showResultsInBottomSheet(List<Classifier.Recognition> results) {
        if (results != null && results.size() >= 3) {
            Classifier.Recognition recognition = results.get(0);
            if (recognition != null) {
                if (recognition.getTitle() != null) recognitionTextView.setText(recognition.getTitle());
                if (recognition.getConfidence() != null)
                    recognitionValueTextView.setText(
                            String.format("%.2f", (100 * recognition.getConfidence())) + "%");
                float confi = 100 * recognition.getConfidence();
                try {
                    if (!one && recognitionTextView.getText().toString().equalsIgnoreCase("0 1 peso bill") && confi>99 ) {
                        mp1.start();
                        one =true;
                        five = false;
                        ten = false;
                        twenty = false;
                        fifthy = false;
                        onehundred = false;
                        twohundred = false;
                        fivehundred = false;
                        onethousand = false;
                        CurrencyModel currencyModel = new CurrencyModel();
                        currencyModel.setCurrency("1 PESO");
                        currencyModel.setDate(getStringCurrentDateTime());
                        Log.e("Inserting"," 1 PESO BILL");
                        viewModel.insertItem(currencyModel);
                    } else if (!five&& recognitionTextView.getText().toString().equalsIgnoreCase("1 5 peso bill")&& confi>99) {
                        mp5.start();
                        one =false;
                        five = true;
                        ten = false;
                        twenty = false;
                        fifthy = false;
                        onehundred = false;
                        twohundred = false;
                        fivehundred = false;
                        onethousand = false;
                        CurrencyModel currencyModel = new CurrencyModel();
                        currencyModel.setCurrency("5 PESO");
                        currencyModel.setDate(getStringCurrentDateTime());
                        Log.e("Inserting"," 5 PESO BILL");
                        viewModel.insertItem(currencyModel);
                    } else if (!ten&&recognitionTextView.getText().toString().equalsIgnoreCase("2 10 peso bill")&& confi>=100 ) {
                        mp10.start();
                        one =false;
                        five = false;
                        ten = true;
                        twenty = false;
                        fifthy = false;
                        onehundred = false;
                        twohundred = false;
                        fivehundred = false;
                        onethousand = false;
                        CurrencyModel currencyModel = new CurrencyModel();
                        currencyModel.setCurrency("10 PESO");
                        currencyModel.setDate(getStringCurrentDateTime());
                        Log.e("Inserting"," 10 PESO BILL");
                        viewModel.insertItem(currencyModel);
                    }else if (!twenty&&recognitionTextView.getText().toString().equalsIgnoreCase("3 20 peso bill")&& confi>=100 ) {
                        mp20.start();
                        one =false;
                        five = false;
                        ten = false;
                        twenty = true;
                        fifthy = false;
                        onehundred = false;
                        twohundred = false;
                        fivehundred = false;
                        onethousand = false;
                        CurrencyModel currencyModel = new CurrencyModel();
                        currencyModel.setCurrency("20 PESO");
                        currencyModel.setDate(getStringCurrentDateTime());
                        Log.e("Inserting"," 20 PESO BILL");
                        viewModel.insertItem(currencyModel);
                    }else if (!fifthy&&recognitionTextView.getText().toString().equalsIgnoreCase("4 50 peso bill")&& confi>=95 ) {
                        mp50.start();
                        one =false;
                        five = false;
                        ten = false;
                        twenty = false;
                        fifthy = true;
                        onehundred = false;
                        twohundred = false;
                        fivehundred = false;
                        onethousand = false;
                        CurrencyModel currencyModel = new CurrencyModel();
                        currencyModel.setCurrency("50 PESO");
                        currencyModel.setDate(getStringCurrentDateTime());
                        Log.e("Inserting"," 50 PESO BILL");
                        viewModel.insertItem(currencyModel);
                    }else if (!onehundred&&recognitionTextView.getText().toString().equalsIgnoreCase("5 100 peso bill")&& confi>=90 ) {
                        mp100.start();
                        one =false;
                        five = false;
                        ten = false;
                        twenty = false;
                        fifthy = false;
                        onehundred = true;
                        twohundred = false;
                        fivehundred = false;
                        onethousand = false;
                         CurrencyModel currencyModel = new CurrencyModel();
                         currencyModel.setCurrency("100 PESO");
                         currencyModel.setDate(getStringCurrentDateTime());
                         Log.e("Inserting"," 100 PESO BILL");
                         viewModel.insertItem(currencyModel);

                    }else if (!twohundred&&recognitionTextView.getText().toString().equalsIgnoreCase("6 200 peso bill")&& confi>=99 ) {
                        mp200.start();
                        one =false;
                        five = false;
                        ten = false;
                        twenty = false;
                        fifthy = false;
                        onehundred = false;
                        twohundred = true;
                        fivehundred = false;
                        onethousand = false;
                        CurrencyModel currencyModel = new CurrencyModel();
                        currencyModel.setCurrency("200 PESO");
                        currencyModel.setDate(getStringCurrentDateTime());
                        Log.e("Inserting"," 200 PESO BILL");
                        viewModel.insertItem(currencyModel);
                    }else if (!fivehundred&&recognitionTextView.getText().toString().equalsIgnoreCase("7 500 peso bill")&& confi>=99 ) {
                        mp500.start();
                        one =false;
                        five = false;
                        ten = false;
                        twenty = false;
                        fifthy = false;
                        onehundred = false;
                        twohundred = false;
                        fivehundred = true;
                        onethousand = false;
                        CurrencyModel currencyModel = new CurrencyModel();
                        currencyModel.setCurrency("500 PESO");
                        currencyModel.setDate(getStringCurrentDateTime());
                        Log.e("Inserting"," 500 PESO BILL");
                        viewModel.insertItem(currencyModel);
                    }else if (!onethousand&&recognitionTextView.getText().toString().equalsIgnoreCase("8 1000 peso bill")&& confi>=99 ) {
                        mp1000.start();
                        one =false;
                        five = false;
                        ten = false;
                        twenty = false;
                        fifthy = false;
                        onehundred = false;
                        twohundred = false;
                        fivehundred = false;
                        onethousand = true;
                        CurrencyModel currencyModel = new CurrencyModel();
                        currencyModel.setCurrency("1000 PESO");
                        currencyModel.setDate(getStringCurrentDateTime());
                        Log.e("Inserting"," 1000 PESO BILL");
                        viewModel.insertItem(currencyModel);
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            Classifier.Recognition recognition1 = results.get(1);
            if (recognition1 != null) {
                if (recognition1.getTitle() != null) recognition1TextView.setText(recognition1.getTitle());
                if (recognition1.getConfidence() != null)
                    recognition1ValueTextView.setText(
                            String.format("%.2f", (100 * recognition1.getConfidence())) + "%");
            }

            Classifier.Recognition recognition2 = results.get(2);
            if (recognition2 != null) {
                if (recognition2.getTitle() != null) recognition2TextView.setText(recognition2.getTitle());
                if (recognition2.getConfidence() != null)
                    recognition2ValueTextView.setText(
                            String.format("%.2f", (100 * recognition2.getConfidence())) + "%");
            }

            Classifier.Recognition recognition3 = results.get(3);
            if (recognition3 != null) {
                if (recognition3.getTitle() != null) recognition3TextView.setText(recognition3.getTitle());
                if (recognition3.getConfidence() != null)
                    recognition3ValueTextView.setText(
                            String.format("%.2f", (100 * recognition3.getConfidence())) + "%");
            }

            Classifier.Recognition recognition4 = results.get(4);
            if (recognition4 != null) {
                if (recognition4.getTitle() != null) recognition4TextView.setText(recognition4.getTitle());
                if (recognition4.getConfidence() != null)
                    recognition4ValueTextView.setText(
                            String.format("%.2f", (100 * recognition4.getConfidence())) + "%");
            }


        }
    }

    protected void showFrameInfo(String frameInfo) {

    }

    protected void showCropInfo(String cropInfo) {

    }

    protected void showCameraResolution(String cameraInfo) {

    }

    protected void showRotationInfo(String rotation) {
//        rotationTextView.setText(rotation);
    }

    protected void showInference(String inferenceTime) {

    }

    protected Classifier.Model getModel() {
        return model;
    }

    private void setModel(Classifier.Model model) {
        if (this.model != model) {
            LOGGER.d("Updating  model: " + model);
            this.model = model;
            onInferenceConfigurationChanged();
        }
    }

    protected Classifier.Device getDevice() {
        return device;
    }

    private void setDevice(Classifier.Device device) {
        if (this.device != device) {
            LOGGER.d("Updating  device: " + device);
            this.device = device;
            final boolean threadsEnabled = device == Classifier.Device.CPU;

            onInferenceConfigurationChanged();
        }
    }

    protected int getNumThreads() {
        return numThreads;
    }

    private void setNumThreads(int numThreads) {
        if (this.numThreads != numThreads) {
            LOGGER.d("Updating  numThreads: " + numThreads);
            this.numThreads = numThreads;
            onInferenceConfigurationChanged();
        }
    }

    protected abstract void processImage();

    protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

    protected abstract int getLayoutId();

    protected abstract Size getDesiredPreviewFrameSize();

    protected abstract void onInferenceConfigurationChanged();

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing.
    }

    public static String getStringCurrentDateTime(){

        String dateStamp = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss aa").format(Calendar.getInstance().getTime());

        return dateStamp;
    }
}

abstract class ExternalStoragePermissions {

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public ExternalStoragePermissions(Activity callingActivity) {}
    // Note call this method
    public static void verifyStoragePermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if(permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
}