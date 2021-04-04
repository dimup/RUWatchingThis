package com.example.auwt;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.face.LBPHFaceRecognizer;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

public class FaceCheck {
    private final Mat resizedInput = new Mat();
    private Rect rc;
    public boolean isDetected = false;

    public Rect faceDetecting (CascadeClassifier cascadeClassifier, Mat frame) {
        Imgproc.resize(frame, resizedInput, new Size(640, 360)); // 크기 resizing
        MatOfRect faces = new MatOfRect();
        cascadeClassifier.detectMultiScale(resizedInput, faces, 1.3,
                3, 0, new Size(150, 150));
        if(faces.total() > 0) { // 탐지된 얼굴이 있을 경우
            isDetected = true;
            for (int i = 0; i < faces.total(); i++) {
                rc = faces.toList().get(i); // {x = 163, y = 45, wxh = 192x192}등의 형식을 가짐
                // 기존의 크기에 맞게 설정하는 과정
                rc.x *= 3;
                rc.y *= 3;
                rc.width *= 3;
                rc.height *= 3;
            }
        }
        else {
            isDetected = false;
            rc = null; // 빈 값
        }
        return rc;
    }

    public int faceAnalyze(Mat frame, Rect rect, LBPHFaceRecognizer model) {
        if(isDetected) {
            Mat testMat = new Mat(frame, rect);
            int[] resultLabel = new int[1];
            double[] confidence = new double[1];
            model.predict(testMat, resultLabel, confidence);
            Log.e("confidence : ", String.valueOf(confidence[0]));
                // 약 85% 이상의 정확도를 보였을 때, 인식 성공
                if(confidence[0] < 15) {
                    return 1;
                }
                else return 0;
        }
        else  {
            return 2;
        }
    }

}
