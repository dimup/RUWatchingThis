# **AUWatchingThis (README 파일 수정중)**

**얼굴 인식을 통해 사용자 등록 여부를 판단하고, 실시간 시선추적을 통해 광고 시청 여부를 판단하는 Android 어플리케이션입니다.**


<br>

### **주요 파일 설명 (app/src/main/java/com/example/auwt/)**
+ **얼굴 인식 및 사용자 등록 여부 판단 관련 파일**
  + "MainActivity.java" : 얼굴 인식 및 사용자 등록 여부 판단 과정에서 휴대폰의 전면 카메라로부터 Frame을 받아오고 사용자가 보는 화면을 담당하는 등 기반적인 역할을 합니다.
  + "FaceCheck.java" : 현재 탐지된 얼굴이 등록된 사용자인지 판단하는 역할을 합니다.
  + "Collect.java" : 등록된 사용자가 아닌 것으로 판단되었을 경우 새로 등록하기 위한 얼굴 이미지 데이터를 수집하는 역할을 합니다.
<p align="center"><img src="https://user-images.githubusercontent.com/46772883/114376067-434bcf80-9bc0-11eb-81a2-9cf8101c8a6d.png" width="75%" height="75%"/></p>

<br>

+ **"시선 추적 및 광고 시청 여부 판단 관련 파일"**
  + "SecondActivity.java" : 시선 추적 및 광고 시청 여부 판단 과정에서 휴대폰의 전면 카메라로부터 Frame을 받아오고 사용자가 보는 화면을 담당하는 등 기반적인 역할을 합니다.
  + "EyeArea.java" : 전체 frame에서 눈 영역만을 나타내는 frame을 새롭게 생성하는 역할을 합니다.
  + "PupilArea.java" : 눈 영역 frame에서 동공의 위치를 파악하는 역할을 합니다.
<p align="center"><img src="https://user-images.githubusercontent.com/46772883/114376432-96258700-9bc0-11eb-8f3c-2fcc94012da4.png" width="75%" height="75%"/></p>


+ **"아직 사용되지 않는 파일"**
  + "SendData.java" : 추후 Elastic search 도구를 이용해서 매 초마다의 광고 시청 여부를 시각화 하기 위한 파일입니다. 
