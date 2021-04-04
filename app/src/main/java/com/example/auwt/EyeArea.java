package com.example.auwt;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 주의! : mat = new Mat(width, height) 가 아닌 -> Mat(height, width) 로 해야 함.

public class EyeArea {
    public Mat frame;
    public Point origin;
    public Point center;
    public Point pupilPoint;
    private final ArrayList<Double> xCoordList;
    private final ArrayList<Double> yCoordList;
    private final Range xRange;
    private final Range yRange;
    public PupilArea pupilArea;
    private final ArrayList<MatOfPoint> matOfPointArrayList;

    public EyeArea() {
        xCoordList = new ArrayList<>();
        yCoordList = new ArrayList<>();
        xRange = new Range();
        yRange = new Range();
        pupilArea = new PupilArea();
        matOfPointArrayList = new ArrayList<>();
    }

    // 원본 frame에서 눈 영역 분리 진행
    private void isolate(Mat frame, List<Point> landmarks) {
        int width = frame.width();
        int height = frame.height();

        xCoordList.clear();
        yCoordList.clear();

        // 랜드마크 x,y 좌표 각각의 최소, 최대 값을 구하기 위해 리스트에 넣음.
        for(Point landmark : landmarks) {
            xCoordList.add(landmark.x);
            yCoordList.add(landmark.y);
        }
        // 흰 바탕의 빈 frame 생성
        Mat mask = new Mat(height, width, CvType.CV_8UC1, new Scalar(255));
        // fillpoly로 만든 빈 frame에 landmark로 둘러쌓인 다각형을 그림 (검정색으로)
        MatOfPoint matOfPoint = new MatOfPoint();
        matOfPoint.fromList(landmarks);
        matOfPointArrayList.add(matOfPoint);
        Imgproc.fillPoly(mask, matOfPointArrayList, new Scalar(0));
        // 검은 바탕의 빈 frame 생성
        Mat blackFrame = Mat.zeros(height, width, CvType.CV_8UC1);
        // bitwise_not 연산으로 눈영역 landmark 부분은 원본 frame을 유지하고, 나머지는 하얀색인 frame 생성 (mask : 픽셀값이 0이 아닌 부분만 연산.)
        Core.bitwise_not(blackFrame, frame, mask);

        // margin 값, 적당한 여유 공간을 위해
        int margin = 5;
        // landmark 중 각 좌표의 최소, 최대 값 추출
        double xMax = Collections.max(xCoordList);
        double yMax = Collections.max(yCoordList);
        double xMin = Collections.min(xCoordList);
        double yMin = Collections.min(yCoordList);
        // margin 처리
        xRange.start = (int)xMin - margin;
        xRange.end = (int)xMax + margin;
        yRange.start = (int)yMin - margin;
        yRange.end = (int)yMax + margin;

        // 기존에 참조중인 객체가 있다면 Free
        if(this.frame != null) this.frame.release();

        // margin 처리 한 너비와 높이를 갖는 눈 영역 frame 생성
        this.frame = new Mat(frame, yRange, xRange);
        // landmark의 최소 좌표 값 저장
        this.origin = new Point(xMin, yMin);
        int frameWidth = this.frame.width();
        int frameHeight = this.frame.height();
        // 눈 영역 정중앙 지점 저장
        double centerX = (origin.x + origin.x + frameWidth)/2;
        double centerY = (origin.y + origin.y + frameHeight)/2;
        this.center = new Point(centerX , centerY);

        mask.release();
        matOfPoint.release();
        matOfPointArrayList.clear();

    }
    // isolate 함수 호출 및 pupilArea 객체 생성하여 동공 위치 추출
    public void analyze(Mat frame, List<Point> landmarks) {
        isolate(frame, landmarks);
        int threshold = 30 ; // 일단 상수값으로. 사용자마다, 주변 밝기 등의 환경마다 결과가 달라짐
        pupilPoint = pupilArea.detectPupil(this.frame, threshold);
    }

    // 동공 좌표 값 반환하기
    public Point pupilLeftCoords() {
        double x = this.origin.x + pupilPoint.x;
        double y = this.origin.y + pupilPoint.y;

        return new Point(x, y);
    }

}
