
![Frame 4](https://user-images.githubusercontent.com/55226431/221772740-e9946fda-a24c-4b90-8871-4d1d8a340725.png)


<br/><br/>

# 두둥<img src="https://user-images.githubusercontent.com/55226431/221770112-27710500-f49a-4c7b-8765-8b3698566e55.png" align=left width=100>

> 모두를 위한 새로운 공연 라이프, 두둥! • <b>백엔드</b> 레포지토리

<br/><br/>


> **두둥은 홍익대학교 컴퓨터 공학과 소속 밴드부 <a href="https://github.com/Gosrock">고스락</a> 에서 만든 서비스에요!**

<br/>

<img width="100%" align=center alt="readme" src="https://user-images.githubusercontent.com/55226431/221773192-5e178d8e-93a4-4a50-821f-3dbd9c9ac759.png">

<br/>

## ✨ 서비스 관련
- [랜딩페이지](https://dudoong.com)
- [호스트 관리자 페이지](https://dudoong.com/admin)
- [서비스 소개 노션](https://dudoong.notion.site/c4999331a2aa47299e1c6821a7dee9af)
- [Storybook](https://gosrock.github.io/DuDoong-Front)
<div>
<img src="https://user-images.githubusercontent.com/55226431/221772278-78452025-d9df-4676-90e7-ca6d4033ed7e.gif"  width="100%" >
</div>

<br>

## 📚 사용 스택
<div align="left">
<div>
<img src="https://img.shields.io/badge/Spring Boot-6DB33F?style=flat-square&logo=Spring Boot&logoColor=white">
<img src="https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=Gradle&logoColor=white">
</div>

<div>
<img src="https://img.shields.io/badge/MySQL-4479A1.svg?style=flat-square&logo=MySQL&logoColor=white">
<img src="https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=Redis&logoColor=white">
</div>

<div>
<img src="https://img.shields.io/badge/Amazon AWS-232F3E?style=flat-square&logo=Amazon AWS&logoColor=white">
<img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=Docker&logoColor=white">
<img src="https://img.shields.io/badge/JSON Web Tokens-000000?style=flat-square&logo=JSON Web Tokens&logoColor=white">
</div>

<div>
<img src="https://img.shields.io/badge/SonarCloud-F3702A?style=flat-square&logo=SonarCloud&logoColor=white">
<img src="https://img.shields.io/badge/Amazon CloudWatch-FF4F8B?style=flat-square&logo=Amazon CloudWatch&logoColor=white">
<img src="https://img.shields.io/badge/Slack-4A154B?style=flat-square&logo=slack&logoColor=white">
</div>

</div>

<br/>



## 🔍 개발 과정
- [찬진 : Spring disable Aop in test](https://devnm.tistory.com/24)
- [경민 : Custom Enum Validator 구현하기](https://gengminy.tistory.com/47)
- [경민 : Relection 을 이용하여 Enum Validator 개선하기](https://gengminy.tistory.com/48)
- [경민 : Custom Enum Deserializer 구현하여 Enum 에 없는 값 null 로 파싱하기](https://gengminy.tistory.com/49)
- [경민 : 스프링 날짜 타입 JSON 변환 및 포맷팅하기 - @JsonFormat, @JacksonAnnotationsInside](https://gengminy.tistory.com/50)
  
  











## 📁 Project Structure
DDD와 멀티모듈 구조를 사용했습니다.

```bash
├── DuDoong-Api  
│   ├── #...
│   └── src  
│       ├── main  
│       │   ├── generated  
│       │   ├── java  
│       │   │   └── band  
│       │   │       └── gosrock  
│       │   │           └── api  
│       │   └── resources  
│       └── test  
├── DuDoong-Batch  
│   └── src  
│       └── main  
│           ├── java  
│           │   └── band  
│           │       └── gosrock  
│           │           ├── excel  
│           │           ├── job  
│           │           ├── parameter  
│           │           └── slack  
│           └── resources  
├── DuDoong-Common  
│   ├── #...
│   └── src  
│       ├── main  
│       │   ├── generated  
│       │   ├── java  
│       │   │   └── band  
│       │   │       └── gosrock  
│       │   │           └── common  
│       │   └── resources  
│       └── test  
├── DuDoong-Domain  
│   ├── #...
│   └── src  
│       ├── main  
│       │   ├── generated  
│       │   │   └── band  
│       │   │       └── gosrock  
│       │   │           └── domain  
│       │   │               ├── common  
│       │   │               └── domains  
│       │   ├── java  
│       │   │   └── band  
│       │   │       └── gosrock  
│       │   │           └── domain  
│       │   │               ├── common  
│       │   │               ├── config  
│       │   │               └── domains  
│       │   └── resources  
│       └── test  
├── DuDoong-Infrastructure  
│   ├── #...
│   └── src  
│       ├── main  
│       │   ├── generated  
│       │   ├── java  
│       │   │   └── band  
│       │   │       └── gosrock  
│       │   │           └── infrastructure  
│       │   │               ├── config  
│       │   │               └── outer 
│       │   │                   └── api  
│       └── test  
├── DuDoong-Socket  
│   ├── #...
│   └── src  
├── build  
└── gradle  
```


## 💻 Developers
<table>
    <tr align="center">
        <td><B>Lead•Backend</B></td>
        <td><B>Backend</B></td>
        <td><B>Backend</B></td>
        <td><B>Backend</B></td>
        <td><B>Backend</B></td>
    </tr>
    <tr align="center">
        <td><B>이찬진</B></td>
        <td><B>김민준</B></td>
        <td><B>김원진</B></td>
        <td><B>노경민</B></td>
        <td><B>이채린</B></td>
    </tr>
    <tr align="center">
        <td>
            <img src="https://github.com/ImNM.png?size=100">
            <br>
            <a href="https://github.com/ImNM"><I>ImNM</I></a>
        </td>
        <td>
            <img src="https://github.com/sanbonai06.png?size=100" width="100">
            <br>
            <a href="https://github.com/sanbonai06"><I>sanbonai06</I></a>
        </td>
        <td>
            <img src="https://github.com/kim-wonjin.png?size=100" width="100">
            <br>
            <a href="https://github.com/kim-wonjin"><I>kim-wonjin</I></a>
        </td>
        <td>
            <img src="https://github.com/gengminy.png?size=100" width="100">
            <br>
            <a href="https://github.com/gengminy"><I>gengminy</I></a>
        </td>
        <td>
            <img src="https://github.com/cofls6581.png?size=100" width="100">
            <br>
            <a href="https://github.com/cofls6581"><I>cofls6581</I></a>
        </td>
    </tr>
</table>