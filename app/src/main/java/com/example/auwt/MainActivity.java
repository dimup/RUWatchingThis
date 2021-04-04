package com.example.auwt;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.face.LBPHFaceRecognizer;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.Manifest.permission.CAMERA;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    public int m_Camidx = 1;//front : 1, back : 0
    private CameraBridgeViewBase m_CameraView;
    public Mat rgbResult;
    public Mat grayResult;
    private static final int CAMERA_PERMISSION_CODE = 200;
    private static final String TAG = "opencv";
    private File mCascadeFile;
    public FaceCheck faceCheck;
    public Collect collect;
    public CascadeClassifier cascadeClassifier;
    private LBPHFaceRecognizer model;
    private int failedCount = 0;
    private int collectCount = 1;
    private File storage;
    private ArrayList<Integer> ids;
    private File storedFile;
    private File[] storedFiles;
    private ArrayList<Mat> fileMats;
    private int fileNum;

    private boolean collectFlag = false; // 첫 실행 시 바로 collect 하기 위한 플래그
    private boolean isCollectToastFlag = true; // Toast를 반복문 내에서 한 번만 띄우기 위한 플래그

    private SharedPreferences preferences; // 앱 종료 후에도 변수값을 남기기 위한 객체
    private SharedPreferences.Editor editor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        OpenCVLoader.initDebug();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // haar cascade 모델 불러오기
        initializeCascadeDependencies();
        cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        cascadeClassifier.load(mCascadeFile.getAbsolutePath());

        //SharedPreferences 객체 설정
        preferences = getPreferences(Activity.MODE_PRIVATE);
        editor = preferences.edit();
        fileNum = preferences.getInt("fileNum", 1);

        // CameraView 설정 SDK 버전 안맞으면 오류. 26->30 으로 올림
        m_CameraView = findViewById(R.id.activity_surface_view);
        m_CameraView.setVisibility(SurfaceView.VISIBLE);
        m_CameraView.setCvCameraViewListener(this);
        m_CameraView.setCameraIndex(m_Camidx);

        // 저장소 설정
        storage = getCacheDir();
    }

    // 권한 설정
    @Override
    protected void onStart() {
        super.onStart();

        //최소 버전보다 버전이 높은지 확인
        if(checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{CAMERA}, CAMERA_PERMISSION_CODE);
        }
        //여기서 카메라뷰 받아옴
        onCameraPermissionGranted();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            m_LoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }



    @Override
    public void onPause()
    {
        super.onPause();

        if (m_CameraView != null)
            m_CameraView.disableView();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (m_CameraView != null)
            m_CameraView.disableView();
    }

    // opencv가 load 된 후에 관련 객체들을 생성하기 위함.
    private final BaseLoaderCallback m_LoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                m_CameraView.enableView();
                rgbResult = new Mat();
                grayResult = new Mat();
                faceCheck = new FaceCheck();
                collect = new Collect();
                if(model == null) {
                    model = train(storage);
                }
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    protected void onCameraPermissionGranted() {
        Log.e("onCameraPermissionGranted", "start");
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase: cameraViews) {
            if (cameraBridgeViewBase != null) {
                cameraBridgeViewBase.setCameraPermissionGranted();
            }
        }
        Log.e("onCameraPermissionGranted", "end");
    }

    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(m_CameraView);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {
    }

    // 현재 frame을 추출하여 다음을 진행
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        rgbResult = inputFrame.rgba();
        // 흑백화
        grayResult = new Mat();
        Imgproc.cvtColor(rgbResult, grayResult, Imgproc.COLOR_BGR2GRAY);
        // frame에서 얼굴 인식 실행 후 영역 받아오기
        Rect resultRect = faceCheck.faceDetecting(cascadeClassifier, grayResult);

        // 탐지된 결과가 있다면
        if (resultRect != null) {
            // 새로 이미지를 수집해야 하는 상황이라면 or 예측 실패 횟수가 50이 넘은 경우
            if (failedCount >= 50 || collectFlag) {
                if(isCollectToastFlag) {
                    // Toast는 메인스레드에서만 호출이 가능하므로, runOnUiThread에서 호출하도록 함
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "사용자 등록을 진행합니다.", Toast.LENGTH_SHORT).show());
                    isCollectToastFlag = false;
                }
                // 얼굴 영역, 텍스트 띄우기
                Imgproc.rectangle(rgbResult, resultRect, new Scalar(255, 0, 0), 5);
                Imgproc.putText(rgbResult, "Save... num : "+ collectCount, new Point(resultRect.x + 10, resultRect.y + 10),
                        Imgproc.FONT_HERSHEY_COMPLEX, 2, new Scalar(0, 255, 0), 3);

                // 얼굴 수집 진행
                collect.collect(grayResult, resultRect, storage, fileNum + "_" + collectCount);
                collectCount += 1;
                // 100장 까지 수집
                if (collectCount == 101) {
                    failedCount = 0;
                    fileNum += 1;
                    editor.putInt("fileNum", fileNum);
                    editor.apply();
                    collectFlag = false;
                    isCollectToastFlag = true;

                    // 이후 액티비티 변환
                    Intent intent = new Intent(this, SecondActivity.class);
                    intent.putExtra("id", String.valueOf(fileNum));
                    startActivity(intent);
                    finish();
                }
            }

            // 수집된 이미지가 있는 경우
            else {
                // 학습된 얼굴 인식 모델로 예측 진행
                int resultInt = faceCheck.faceAnalyze(grayResult, resultRect, model);
                Imgproc.rectangle(rgbResult, resultRect, new Scalar(255, 0, 0), 5);

                // 이미 등록된 사용자라면
                if (resultInt == 1) {
                    // 안내문 띄우고 종료
                    Imgproc.putText(rgbResult, "UnLock", new Point(resultRect.x + 10, resultRect.y + 10),
                            Imgproc.FONT_HERSHEY_COMPLEX, 2, new Scalar(0, 0, 255), 3);
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "이미 등록된 사용자입니다. 앱을 종료합니다.",
                            Toast.LENGTH_SHORT).show());
                    finish();
                }
                // 등록되지 않은 사용자라면
                else if (resultInt == 0) {
                    Imgproc.putText(rgbResult, "Locked", new Point(resultRect.x + 10, resultRect.y + 10),
                            Imgproc.FONT_HERSHEY_COMPLEX, 2, new Scalar(255, 0, 0), 3);
                    // 예측 실패 횟수 + 1
                    failedCount += 1;
                }

            }
        }
        // 탐지된 얼굴이 없다면 detecting... 텍스트 띄우기
        else Imgproc.putText(rgbResult, "detecting...", new Point( 250, 250), Imgproc.FONT_HERSHEY_COMPLEX, 2, new Scalar(0, 255, 0), 3);
        return rgbResult;
    }

    // 내부에 저장된 haarcascade 파일의 절대경로 불러오기
    private void initializeCascadeDependencies() {
        try {
            File cascadeDir = getDir("haarcascade_frontalface_default", Context.MODE_PRIVATE);
            mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_default.xml");

            if (!mCascadeFile.exists()) {
                FileOutputStream os = new FileOutputStream(mCascadeFile);
                InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_default);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                is.close();
                os.close();
            }
        } catch (Throwable throwable) {
            throw new RuntimeException("Failed to load Haar Cascade file");
        }
    }

    // 이미지 학습 진행
    private LBPHFaceRecognizer train(File storage) {
        ids = new ArrayList<>(); // 라벨링 숫자를 위한 리스트
        storedFile = new File(storage.toString());
        storedFiles = storedFile.listFiles();
        fileMats = new ArrayList<>();

        // 캐시메모리에 아무 파일 없으면 수행 x
        if(storedFiles.length == 0) {
            collectFlag = true;
        }
        // 파일이 있다면 학습 시작
        else {
            for (File temp : storedFiles) {
                Mat tempMat = new Mat();
                String tempName = storage+"/"+temp.getName();
                Bitmap tempBit = BitmapFactory.decodeFile(tempName);
                Utils.bitmapToMat(tempBit, tempMat);
                Mat grayMat = new Mat();
                Imgproc.cvtColor(tempMat, grayMat, Imgproc.COLOR_BGR2GRAY);
                fileMats.add(grayMat);
                // 파일 이름이 1_12.jpg, 1_45.jpg 이므로 고유 번호인 첫 숫자를 label로 사용
                ids.add((int) temp.getName().charAt(0));
            }

            int[] idsInt = new int[ids.size()];
            for (int i = 0; i < idsInt.length; i++) {
                idsInt[i] = ids.get(i);
            }
            MatOfInt labels = new MatOfInt(idsInt);
            model = LBPHFaceRecognizer.create();
            if (fileMats.size() != 0) model.train(fileMats, labels);
        }
        return model;
    }



}