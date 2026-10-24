# Mod of Mod — 발전과제 모험 (Fabric 26.3)

두 명이 서로 다른 안전한 랜덤 위치에서 출발해 발전과제를 깨고 코인을 모아 상점 아이템을 구매하는 작은 야생 서버 모드입니다.

## 설치 (현재 0.1.0 시제품)

**Minecraft Java Edition 26.3 / Fabric Loader 0.19.5+ / Fabric API 0.163.0+26.3 / Java 25** 대상입니다.

하나의 **동일한 JAR 파일**을 서버와 두 명의 플레이어 PC 모두의 `mods/` 폴더에 설치합니다. 서버만 설치하면 B키·상점·전용 팝업은 작동하지 않습니다. 기존 서버의 백업본에서 먼저 테스트하세요.

**현재 상태: 소스 초기 구현이며 GitHub Actions 및 실제 게임에서 빌드/실행 성공은 아직 검증되지 않았습니다.**

## 구현된 목표 기능

| 기능 | 현재 구현 |
| --- | --- |
| 최초 접속 시 개인별 랜덤 스폰 | 오버월드 ±2,200좌표 내에서 최대 32회 안전 지점 시도. 서로 700블록 이상 이격 시도. 지형이 안전하지 않으면 원래 스폰 유지 |
| 발전과제 보상 | 일반 3코인·목표 15코인·도전 50코인, 특별 보상 최대 150코인; 서버가 20틱마다 2명 수준만 확인 |
| 맞춤 문구 | 대표 발전과제 11개 개별 멘트, 나머지는 난이도별 문구 |
| 보상 UI | 화면 가운데 별도 종이형 알림에 `[5코인]+`처럼 표시 |
| 코인 | 서버 JSON 영구 저장, 플레이어 UUID별 관리, 동일 발전과제 중복 보상 방지 |
| 상점 | **B** 키 / 8종 판매 / 구매·잔액을 서버가 직접 검증 |
| 왼쪽 위 코인 UI | 상점 화면이 켜져 있을 때만 찢어진 종이 형태로 표시 |
| 지도 지급 | 개인 스폰 중심의 **바닐라 채워진 지도** 지급. 지도는 손에 들고 탐험할 때 갱신 |
| 지도 표시 설정 | **J** 키로 우측 상단 지도 좌표 안내를 켜거나 끔. 개인 PC 설정 `config/modofmod-client.json`에 저장 |
| 지도 보호 | 사망 후 원본 Map ID를 가진 지도를 다시 지급. 실제 사망 드롭 자체가 억제되는 완전한 인벤세이브는 아직 미구현 |

**아직 없는 것:** 지형을 보여 주는 독립 실시간 미니맵, 지도 표시 자체 ON/OFF, 지도 아이템 완전한 영구 인벤세이브(원본을 드롭하지 않음), 모든 발전과제에 서로 다른 126개 수작업 멘트, 기본 발전과제 토스트 제거, 전용 PNG 텍스처/리소스팩. 현재 UI는 텍스처 업로드 없는 프로시저 기반 종이 모양입니다. 기능을 구현했다고 오해하지 않도록 현재 범위를 명확히 표시합니다.

## 빌드

1. GitHub **Actions** → **Build Fabric 26.3 Mod** 진입
2. 빌드가 시작되지 않았다면 **Run workflow** 실행
3. 성공 시 **Artifacts → mod-of-mod-fabric-26.3** 다운로드
4. `mod-of-mod-0.1.0.jar`를 서버와 **두 클라이언트**에 설치

GitHub Actions는 Java 25 / Gradle 9.7.1 / Loom 1.18-SNAPSHOT 사용.

## Simple Voice Chat과 함께 사용

**Minecraft 26.3용 Simple Voice Chat Fabric 2.6.24+26.3** 모드는 Mod of Mod와 **별도로 설치**합니다. 게임 서버와 두 플레이어의 PC에 Minecraft/Fabric 버전이 같은 Simple Voice Chat JAR을 추가하세요. 이 저장소는 Simple Voice Chat 자체를 내장하거나 변경하지 않습니다.

- B: Mod of Mod 상점
- J: Mod of Mod 지도 좌표 안내 표시 전환
- V: Simple Voice Chat 메뉴
- M: Simple Voice Chat 마이크 음소거
- N: Simple Voice Chat 비활성화
- 다른 모드나 개인 설정으로 단축키가 겹치면 **설정 → 조작 → 키 지정**에서 변경합니다.

Simple Voice Chat에는 별도의 **UDP 포트**가 필요합니다(기본 `24454/UDP`). 호스팅 업체에서 해당 UDP 포트를 제공해야 음성 채팅이 작동합니다. 서버는 `config/voicechat/voicechat-server.properties`의 `port` 값을 확인해 주세요.

공식 안내: https://modrepo.de/minecraft/voicechat/wiki/server_setup

## 조작법

- **B**: 모험가 상점 오픈
- **J**: 우측 상단 지도 좌표 안내 켜기/끄기 (실제 미니맵이 아닌 안내)
- 가격: 빵8 8C, 횃불16 10C, 철곡괭이24C, 다이아2 60C, 엔더진주8 80C, 황금사과2 95C, 토템220C, 겉날개400C
- 서버 `server.properties`의 `max-players=2` 권장 (모드 자체에서 접속 인원을 제한하지 않음)

## 데이터

- 서버 설정 경로 `config/modofmod-players.json`: 플레이어별 코인, 보상한 발전과제 ID 집합, 지도 ID, 최초 스폰 여부.
- 클라이언트 `config/modofmod-client.json`: 지도 좌표 안내 여부.
- 코인 변경은 임시 파일에 기록 후 파일 교체. 서버 재시작과 UUID별 재접속 후에도 유지.
- 서버 백업에는 `world/`뿐 아니라 `config/modofmod-players.json`도 포함할 것.
- 기존 플레이어에게 설치하면 **이미 달성한 발전과제도 첫 스캔에서 보상될 수 있음**. 새 월드에서 플레이를 권장.

## 개발 의도 / 성능

Minecraft 원본 코드나 클라이언트 HUD를 매 틱 강제로 스캔하지 않음. 발전과제 완료 여부만 서버에서 1초 간격으로 검사하며, 상품과 가격은 클라이언트에서 조작해도 서버 값으로 계산함. 영구 저장 후 코인만 지급하고 상점은 빈 인벤토리 칸을 요구함.

현재 버전은 **테스트 전 개발 시제품**입니다. 2026-10-25 빌드 로그의 6개 컴파일 오류를 수정했으며, GitHub Actions 재빌드 성공은 아직 확인되지 않았습니다. 실제 서버 투입 전에 Actions 결과, 최초 접속 스폰, 엔더 드래곤 보상, 구매/재시작, 사망 후 지도, 유저 두 명의 개별 잔고 동기화를 확인해야 합니다.

## 공식 자료

- [Fabric 26.3 release](https://fabricmc.net/2026/09/15/263.html)
- [Fabric setup](https://fabricmc.net/develop/)
- [Fabric networking](https://docs.fabricmc.net/develop/networking)
- [Fabric custom screens](https://docs.fabricmc.net/develop/rendering/gui/custom-screens)
