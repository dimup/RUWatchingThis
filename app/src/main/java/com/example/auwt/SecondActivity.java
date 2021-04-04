package com.example.auwt;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.VideoView;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.face.Face;
import org.opencv.face.Facemark;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static android.Manifest.permission.CAMERA;

/*
* Todo list
*  1. 자동으로 최적의 threshold 값 설정하는 코드 필요 (Calibrate class)
* */

public class SecondActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{
    private File mLbfFile;
    private File mCascadeFile;
    private Facemark facemark;
    public int m_Camidx = 1;//front : 1, back : 0
    private CameraBridgeViewBase m_CameraView;
    private static final int CAMERA_PERMISSION_CODE = 200;
    private static final String TAG = "opencv";
    private CascadeClassifier cascadeClassifier;
    private EyeArea eyeArea;
    private final List<MatOfPoint2f> landmarks = new ArrayList<>();
    private boolean isDetected = false;
    private final List<Point> boundaryPointList = new ArrayList<>();
    private Point centerPoint = new Point();
    private Point pupilCoords;
    private Point center;
    private int capNumber = 0;
    private VideoView videoView;
    private final List<String> NOTYLIST = Arrays.asList("좌측 끝", "우측 끝", "위쪽 끝", "아래쪽 끝");
    private double minBoundaryX;
    private double maxBoundaryX;
    private double minBoundaryY;
    private boolean adStartFlag = false;
    private ImageButton capBtn;
    private double maxBoundaryY;
    private int numOfFrame;
    private int numOfIntervelFrame;
    private ArrayList<Integer> watchingList = new ArrayList<>();
    private String intervelWatchResult = "";
    private static String phpServerURL = "url";
    private String id;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        OpenCVLoader.initDebug();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        Intent intent = getIntent();
        id = intent.getStringExtra("id");

        // haar cascade, lbf 모델 불러오기
        initializeLBFDependencies();
        initializeCascadeDependencies();
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), "광고 시청을 위한 단계입니다. \n 중앙을 보고 버튼을 눌러 캡쳐해주세요.", Toast.LENGTH_LONG).show());
        cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        cascadeClassifier.load(mCascadeFile.getAbsolutePath());
        facemark = Face.createFacemarkLBF();
        facemark.loadModel(mLbfFile.getAbsolutePath());

        // eyeArea 객체 생성
        eyeArea = new EyeArea();

        // CameraView 설정. SDK 버전 안맞으면 오류. 26->30 으로 올림
        m_CameraView = findViewById(R.id.activity_surface_view);
        m_CameraView.setVisibility(SurfaceView.VISIBLE);
        m_CameraView.setCvCameraViewListener(this);
        m_CameraView.setCameraIndex(m_Camidx);

        // 캡쳐 버튼 설정
        capBtn = findViewById(R.id.capButton);
        // 캡처 버튼 눌렀을 때의 이벤트 처리
        capBtn.setOnClickListener(e -> {
            if(capNumber < 5) {
                // 얼굴이 감지된 경우
                if(isDetected) {
                    // 버튼 누를 시 동공 좌표 & 눈 영역의 중앙 위치 좌표가 리스트에 저장됨.
                    if(capNumber == 0) centerPoint = eyeArea.center;
                    else boundaryPointList.add(pupilCoords);

                    // 마지막 캡쳐인 경우
                    if (capNumber == 4) {
                        runOnUiThread(() -> Toast.makeText(getApplicationContext(), "캡쳐 완료. \n시청 할 지점에서 버튼을 누르면 광고 시청이 시작됩니다.", Toast.LENGTH_SHORT).show());
                        // Boundary 설정 및 캡쳐버튼 삭제
                        setBoundary(centerPoint, boundaryPointList);
                        Log.e("center", String.valueOf(centerPoint));
                        Log.e("point", String.valueOf(boundaryPointList));
                        capNumber += 1;
                    }
                    else {
                        runOnUiThread(() -> Toast.makeText(getApplicationContext(), "캡쳐 완료. \n다음은 " + NOTYLIST.get(capNumber) + "을 보고 캡쳐해주세요.", Toast.LENGTH_SHORT).show());
                        capNumber += 1;
                    }
                }
                // 얼굴이 감지되지 않은 채 버튼을 눌렀을 경우
                else {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "얼굴 인식 후 캡쳐를 다시 진행해주세요.", Toast.LENGTH_SHORT).show());
                }
            }
            else {
                center = eyeArea.center;
                capNumber += 1;
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "해당 위치에서 시선 추적을 진행합니다.", Toast.LENGTH_SHORT).show());
            }
        });

        // 광고 재생용 videoView 설정
        videoView = findViewById(R.id.adVideoView);
        videoView.setVisibility(View.INVISIBLE);
        videoView.setVideoPath(String.valueOf(Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.test)));


        // videoView 크기 설정을 위한 코드
        ViewGroup.LayoutParams parmas = videoView.getLayoutParams();
        int width = parmas.width * 2 / 3;
        int height = parmas.height * 2 / 3;
        parmas.width = width;
        parmas.height = height;
        videoView.setLayoutParams(parmas);

        //광고 재생이 끝날 경우 DB 서버로 넘겨주기 위함
        videoView.setOnCompletionListener(e -> {
            adStartFlag = false;
            int before = 0;
            numOfIntervelFrame = numOfFrame / 15;
            for(int i=0; i<=numOfFrame; i++) {
                if(i % numOfIntervelFrame == 0 && i != 0) {
                    watchingAnalyze(before, i);
                    before = i;
                }
            }
            /* 추후 php 서버 연동 시 
            SendData task = new SendData();
            task.execute(phpServerURL, id, intervelWatchResult);
            */

        });

    }

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
    public void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            m_LoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onPause() {
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

    private final BaseLoaderCallback m_LoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                m_CameraView.enableView();
            } else {
                super.onManagerConnected(status);
            }
        }
    };
    // 카메라 권한 처리
    protected void onCameraPermissionGranted() {
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase : cameraViews) {
            if (cameraBridgeViewBase != null) {
                cameraBridgeViewBase.setCameraPermissionGranted();
            }
        }
    }

    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(m_CameraView);
    }

    @Override
    public void onCameraViewStarted(int i, int i1) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    // 현재 frame을 추출하여 다음을 진행
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        // 캡쳐가 모두 완료됐다면 광고 재생
        if(capNumber == 6) {
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "광고를 시청합니다.", Toast.LENGTH_SHORT).show());
            runOnUiThread(() -> videoView.setVisibility(View.VISIBLE));
            runOnUiThread(() -> videoView.start());
            capNumber += 1;
            adStartFlag = true;
        }

        // 흑백화
        Mat rgbResult = inputFrame.rgba();
        Mat grayResult = new Mat();
        Imgproc.cvtColor(rgbResult, grayResult, Imgproc.COLOR_BGR2GRAY);

        // 얼굴 인식 진행
        MatOfRect faces = new MatOfRect();
        cascadeClassifier.detectMultiScale(grayResult, faces, 1.3, 3, 0,
                new Size(600, 600));

        // 탐지된 얼굴이 하나 이상 있을 경우
        if(faces.total() > 0) {
            isDetected = true;
            Imgproc.rectangle(rgbResult, faces.toList().get(0), new Scalar(0,255,0), 5);

            // face landmarks 추출. 왼쪽 눈 영역 랜드마크만 사용
            facemark.fit(grayResult, faces, landmarks);
            for (MatOfPoint2f landmark : landmarks) {
                List<Point> landmarkList = landmark.toList();
                List<Point> eyeLandmarkList = new ArrayList<>();
                // 36 - 47 까지가 눈 영역 (36~41 : 왼쪽 | 42~47 : 오른쪽)
                for (int i = 36; i <= 47; i++) {
                    Imgproc.circle(rgbResult, landmarkList.get(i), 4,
                            new Scalar(0, 0, 255), 3);
                    if(i<=41)
                        eyeLandmarkList.add(landmarkList.get(i));
                }

                // frame에서 눈 영역 분리 진행
                eyeArea.analyze(grayResult, eyeLandmarkList);
                if(!adStartFlag)
                    Imgproc.drawContours(rgbResult, eyeArea.pupilArea.contours, -1, new Scalar(0, 255, 0), 2);

                // 원본 frame에서의 동공 좌표 받아오기
                pupilCoords = eyeArea.pupilLeftCoords();
                double x = pupilCoords.x;
                double y = pupilCoords.y;
                // 광고가 재생된 상태라면
                if(adStartFlag) {
                    if(faces.total() > 0) {
                        numOfFrame += 1;
                        // 시청 여부 판단 진행
                        boolean isWatchFlag = isWatching(center, new Point(x, y));
                        if (isWatchFlag) {
                            watchingList.add(1);
                            Imgproc.putText(rgbResult, "Watching", new Point(35, 35),
                                    Imgproc.FONT_HERSHEY_COMPLEX, 2, new Scalar(0, 255, 0), 3);
                        }
                        else {
                            watchingList.add(0);
                            Imgproc.putText(rgbResult, "Not watching", new Point(35, 35),
                                    Imgproc.FONT_HERSHEY_COMPLEX, 2, new Scalar(255, 0, 0), 3);
                        }
                    }
                    else watchingList.add(2);
                }
                Imgproc.line(rgbResult, new Point(x - 10, y), new Point(x + 10, y), new Scalar(255, 0, 0), 1);
                Imgproc.line(rgbResult, new Point(x, y - 10), new Point(x, y + 10), new Scalar(255, 0, 0), 1);
            }
        }
        else isDetected = false;

        grayResult.release();
        faces.release();
        landmarks.clear();

        return rgbResult;
    }

    // 랜드마크를 위한 lbf 모델 불러오기
    private void initializeLBFDependencies() {
        try {
            File lbfDir = getDir("lbf", Context.MODE_PRIVATE);
            mLbfFile = new File(lbfDir, "lbf.yaml");

            if (!mLbfFile.exists()) {

                FileOutputStream os = new FileOutputStream(mLbfFile);
                InputStream is = getResources().openRawResource(R.raw.lbf);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                is.close();
                os.close();
            }
        } catch (Throwable throwable) {
            throw new RuntimeException("Failed to load lbfmodel file");
        }

    }

    // 얼굴 인식을 위한 haar cascade 모델 불러오기
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

    // Boundary 설정
    private void setBoundary(Point centerPoint, List<Point> boundaryPointList) {
        minBoundaryX = centerPoint.x - boundaryPointList.get(0).x;
        maxBoundaryX = centerPoint.x - boundaryPointList.get(1).x;
        maxBoundaryY = centerPoint.y - boundaryPointList.get(2).y;
        minBoundaryY = centerPoint.y - boundaryPointList.get(3).y;
    }

    // 시청 여부 판단
    private boolean isWatching(Point center, Point pupilCoords) {
        double centerX = center.x;
        double centerY = center.y;
        double diffX = centerX - pupilCoords.x;
        double diffY = centerY - pupilCoords.y;
        Log.e("watching boundary : ", diffX + " , " + diffY);
        return !(diffX < minBoundaryX) && !(diffX > maxBoundaryX) &&
                !(diffY > maxBoundaryY) && !(diffY < minBoundaryY);
    }

    private void watchingAnalyze(int before, int intervel) {
        double watch = 0;
        double not = 0;

        for(int i=before; i<=intervel; i++) {
            int result = watchingList.get(i);
            if(result == 1) watch += 1;
            else not += 1;
        }
        Log.e("watch // not", watch + "//" + not);
        if(watch / (watch+not) > 0.80) intervelWatchResult += "1";
        else intervelWatchResult += "0";
    }
}