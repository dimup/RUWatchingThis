package com.example.auwt;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import java.util.ArrayList;
import java.util.List;


public class PupilArea {
    private final Mat kernel;
    private final Point gravity;
    public List<MatOfPoint> contours; // secondactivity에서 프레임에 그려보려고 public으로 바꿔놓음
    public PupilArea() {
        kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        gravity = new Point();
        contours = new ArrayList<>();
    }

    // 필터링, 침식연산, thresholding 진행. 향후 자동 threshold 값 설정 코드에 쓰일 수 있어서, public으로 설정
    public Mat imageProcessing(Mat eyeFrame, int threshold) {
        Mat filteredFrame = new Mat();
        Imgproc.bilateralFilter(eyeFrame, filteredFrame, 10, 15, 15); // 필터링. 경계 보존하며 노이즈 감소
        Mat erodedFrame = new Mat();
        Imgproc.erode(filteredFrame, erodedFrame, kernel); // 침식연산
        Mat processedFrame = new Mat();
        Imgproc.threshold(erodedFrame, processedFrame, threshold, 255, Imgproc.THRESH_BINARY); // thresholding

        return processedFrame;
    }

    // 동공 위치 추출 진행
    public Point detectPupil(Mat eyeFrame, int threshold) {
        Mat pupilFrame = imageProcessing(eyeFrame, threshold);
        contours.clear();
        Mat hierarchy = new Mat();
        // contour 추출
        Imgproc.findContours(pupilFrame, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);
        // contour 크기가 큰 순서로 정렬
        contours.sort((c1, c2) -> (int) (Imgproc.contourArea(c2) - Imgproc.contourArea(c1)));

        try {
            if(contours.size() > 1) {
                // 0번째는 눈이 아닌 전체 프레임 윤곽을 따내는 것을 확인. 따라서 1번째 인덱스로 결정함.
                Moments moments = Imgproc.moments(contours.get(1));

                // moment 연산으로 윤곽선의 무게중심 추출
                gravity.x = (int) (moments.get_m10() / moments.get_m00());
                gravity.y = (int) (moments.get_m01() / moments.get_m00());
            }
        }

        catch (Exception e) {
            e.printStackTrace();
        }

        hierarchy.release();
        return gravity;
    }

}
