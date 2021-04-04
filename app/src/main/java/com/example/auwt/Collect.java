package com.example.auwt;

import android.graphics.Bitmap;
import android.util.Log;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Collect {
    public Bitmap bitmap;
    private boolean createResult = false;

    // 캐시메모리에 이미지 수집(저장) 진행
    public void collect(Mat inputMat, Rect rect, File storage, String name) {
        Mat saveMat = new Mat(inputMat, rect);
        bitmap = Bitmap.createBitmap(saveMat.cols(), saveMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(saveMat, bitmap);

        //저장할 파일 이름
        String fileName = name + ".jpg";
        //storage 에 파일 인스턴스를 생성
        File tempFile = new File(storage, fileName);

        try {
            // 자동으로 빈 파일을 생성
            createResult = tempFile.createNewFile();
            // 파일을 쓸 수 있는 스트림을 준비
            FileOutputStream out = new FileOutputStream(tempFile);
            // compress 함수를 사용해 스트림에 비트맵을 저장
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            // 스트림 사용후 닫아줍니다.
            out.close();

        } catch (FileNotFoundException e) {
            Log.e("MyTag","FileNotFoundException : " + e.getMessage());
        } catch (IOException e) {
            Log.e("MyTag","IOException : " + e.getMessage());
        }
    }
}
