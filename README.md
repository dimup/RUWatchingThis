# **AUWatchingThis**

**얼굴 인식을 통해 사용자 등록 여부를 판단하고, 실시간 시선추적을 통해 광고 시청 여부를 판단하는 Android 어플리케이션입니다.**


<br>

### **주요 파일 설명 (app/src/main/java/com/example/auwt/)**
+ **얼굴 인식 및 사용자 등록 여부 판단 관련 파일**
  + "MainActivity.java" : 얼굴 인식 및 사용자 등록 여부 판단 과정에서 휴대폰의 전면 카메라로부터 Frame을 받아오고 사용자가 보는 화면을 담당하는 등 기반적인 역할을 합니다.
  + "FaceCheck.java" : 현재 탐지된 얼굴이 등록된 사용자인지 판단하는 역할을 합니다.
  + "Collect.java" : 등록된 사용자가 아닌 것으로 판단되었을 경우 새로 등록하기 위한 얼굴 이미지 데이터를 수집하는 역할을 합니다.
<p align="center"><img src="https://user-images.githubusercontent.com/46772883/114376067-434bcf80-9bc0-11eb-81a2-9cf8101c8a6d.png" width="80%" height="80%"/></p>

<br>

+ **"시선 추적 및 광고 시청 여부 판단 관련 파일"**
  + "SecondActivity.java" : 시선 추적 및 광고 시청 여부 판단 과정에서 휴대폰의 전면 카메라로부터 Frame을 받아오고 사용자가 보는 화면을 담당하는 등 기반적인 역할을 합니다.
  + "EyeArea.java" : 전체 frame에서 눈 영역만을 나타내는 frame을 새롭게 생성하는 역할을 합니다.
  + "PupilArea.java" : 눈 영역 frame에서 동공의 위치를 파악하는 역할을 합니다.
<p align="center"><img src="https://user-images.githubusercontent.com/46772883/114376432-96258700-9bc0-11eb-8f3c-2fcc94012da4.png" width="80%" height="80%"/></p>


+ **"아직 사용되지 않는 파일"**
  + "SendData.java" : 추후 Elastic search 도구를 이용해서 매 초마다의 광고 시청 여부를 시각화 하기 위한 파일입니다. 


<br>

## **구성도 및 진행 과정**

<br>

### **얼굴 인식 및 사용자 등록 여부 판단 과정**

+ **구성도**
<p align="center"><img src="https://user-images.githubusercontent.com/46772883/114499285-b9a00e80-9c60-11eb-9f48-1ff10d9aa805.png" /></p>

1) 어플리케이션 실행 시 전면 카메라의 frame을 통해 얼굴 인식을 진행하며, 모델을 통해 등록된 사용자인지 판단을 진행합니다.
2) 등록된 사용자라면, 광고를 이미 본 사람이므로 어플리케이션을 종료합니다.
3) 등록된 사용자가 아니거나 어플리케이션이 처음 실행된 상태라면, 현재 얼굴에 대한 이미지를 수집하고 얼굴 인식 모델 학습을 진행합니다. 이후 시선 추적 단계로 전환합니다.


+ **진행 과정**
  + 사용자의 전면 카메라로부터 frame을 추출하고, frame 속 얼굴 영역을 탐지하는 것은 Haarcascade 모델 파일을 불러와서 진행합니다.
  + LBPHFaceRecognizer 모델을 생성해서 등록된 사용자인지 인식을 진행합니다. App이 처음 시작된 경우, 이미지 수집 단계로 넘어갑니다.
  + 이미지 수집은 탐지된 얼굴 영역에 대해 100장을 수집하도록 합니다. 이미지 데이터는 App 내부 캐시메모리에 저장합니다. 이는 외부에서 임의로 접근하는 것을 방지하고, App 삭제 시 함께 삭제된다는 장점이 있다고 생각했기 때문입니다.
  + 수집된 이미지를 통해 얼굴 인식 모델 학습을 진행하고, 등록 여부를 판단합니다. Confidence 값이 15 이하일 경우 등록된 사용자로 판단하며, 50frame 동안 등록된 사용자로 판단되지 않을 경우 미등록 사용자로 판단합니다.

+ **동작 예시**
<p align="center"><img src="https://user-images.githubusercontent.com/46772883/114501244-970ff480-9c64-11eb-8888-681010a23307.png" /></p>

<br>

### **시선 추적 및 광고 시청 여부 판단 과정**

+ **구성도**
<p align="center"><img src="https://user-images.githubusercontent.com/46772883/114501370-d63e4580-9c64-11eb-8297-3245eef70b5b.png" /></p>

1) 전면 카메라의 Frame에서 face landmark를 추출합니다. 이 중 왼쪽 눈을 나타내는 landmark의 좌표들을 통해 눈 영역만을 나타내는 frame을 생성합니다.
2) 눈 영역만을 나타내는 Frame에서 thresholding, 윤곽선 추출, 윤곽선 무게중심 추출 등을 통해 동공의 위치를 추적합니다.
3) 동공의 움직임에 대한 일정 기준점을 설정하여, 시청 여부를 판단합니다.


+ **진행 과정(시선 추적)**
  + 눈 영역만을 나타내는 Frame을 생성하기 위한 face landmark 추출은 FacemarkLBF 모델 파일을 불러와서 진행합니다. 추출된 landmark 중 왼쪽 눈을 나타내는 좌표(36번 - 41번)를 활용하게 됩니다.
  + fill_poly(), bitwise_not() 함수들을 통해 눈 영역 frame을 생성합니다.
  + 눈 영역 frame에서 스무딩, 침식 연산 등의 처리를 진행하고 지정한 threshold 값을 통해 이진화를 진행합니다.
  + 이진화 결과에서 윤곽선들을 추출하고 내부 면적이 큰 순서대로 정렬합니다. 이후 가장 큰 윤곽선에 대한 무게중심을 추출하고 이를 동공(시선)의 위치로 판단합니다.
<p align="center"><img src="https://user-images.githubusercontent.com/46772883/114502824-55347d80-9c67-11eb-9ba7-d74264856813.png" /></p>


+ **진행 과정(시청 여부 판단)**
  + 시선 추적 단계가 시작되면 시청 여부 판단을 위한 기준을 설정합니다. 위에서 진행되는 시선 추적을 통해 사용자가 휴대전화의 상,하,좌,우 끝 지점을 각각 쳐다봤을 때 버튼을 눌러 동공의 위치를 캡쳐합니다. 이 때 눈 영역의 중앙 지점과, 각 끝 지점을 쳐다봤을 때의 동공 좌표 값 차이를 통해 경계를 생성합니다.
  + 기준 설정을 마치고 광고 시청을 시작할 위치에서 버튼을 눌러 시청을 시작합니다. 해당 위치에서 눈영역의 중앙 지점을 기준으로, 동공의 위치가 생성한 경계를 벗어나는지 확인하여 시청 여부를 판단합니다.
<p align="center"><img src="https://user-images.githubusercontent.com/46772883/114503248-f4597500-9c67-11eb-907e-2225ed31b185.png" /></p>

+ **동작 예시**
<p align="center"><img src="https://user-images.githubusercontent.com/46772883/114503690-a98c2d00-9c68-11eb-895f-df879fce5de3.png" /></p>
