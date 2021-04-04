package com.example.auwt;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Calibrate {
    private int numOfFrames;
    private ArrayList<Integer> thresholdsOfLeft;
    private PupilArea pupilArea;


    public Calibrate() {
        numOfFrames = 20;
        thresholdsOfLeft = new ArrayList<>();
        pupilArea = new PupilArea();
    }

    public boolean isCalibrated() {
        return this.thresholdsOfLeft.size() >= numOfFrames;
    }

    public int setThreshold() {
        int total = 0;
        for(int i=0; i<thresholdsOfLeft.size(); i++) {
            total += thresholdsOfLeft.get(i);
        }
        return (total / thresholdsOfLeft.size());
    }

    public double getPupuilSize(Mat eyeFrame) {
        Mat resizeFrame = eyeFrame.colRange(5, eyeFrame.cols()-5);
        resizeFrame = resizeFrame.rowRange(5, resizeFrame.rows()-5);

        int width = resizeFrame.width();
        int height = resizeFrame.height();

        double numOfPixel = width * height;
        double numOfBlack = numOfPixel - Core.countNonZero(resizeFrame);

        return numOfBlack / numOfPixel;
    }

    public double findBestTreshold(Mat eyeFrame) {
        double avePupilSize = 0.48;
        Map<Integer, Double> trials = new HashMap<>();

        for(int threshold = 5; threshold <= 100; threshold += 5) {
            Mat eyeFrame2 = pupilArea.imageProcessing(eyeFrame, threshold);
            trials.put(threshold, getPupuilSize(eyeFrame2));
        }
        int bestThreshold = 14;
        //trials 최소값 구해야함

        return bestThreshold;
    }
}
