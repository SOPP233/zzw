@echo off
setlocal

set "BASEDIR=%~dp0"
set "WRAPPER_DIR=%BASEDIR%.mvn\wrapper"
set "MAVEN_VERSION=3.9.11"
set "MAVEN_DIST=apache-maven-%MAVEN_VERSION%-bin.zip"
set "MAVEN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/%MAVEN_DIST%"
set "MAVEN_HOME=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%"
set "MAVEN_BIN=%MAVEN_HOME%\bin\mvn.cmd"
set "ARCHIVE_PATH=%WRAPPER_DIR%\%MAVEN_DIST%"
set "JDK_DIST=temurin-17-jdk.zip"
set "JDK_URL=https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse"
set "JDK_ARCHIVE=%WRAPPER_DIR%\%JDK_DIST%"
set "JDK_HOME=%WRAPPER_DIR%\jdk-17"

if not exist "%MAVEN_BIN%" (
  if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"
  echo Downloading Maven %MAVEN_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri '%MAVEN_URL%' -OutFile '%ARCHIVE_PATH%'"
  if errorlevel 1 (
    echo ERROR: Failed to download Maven from %MAVEN_URL%
    exit /b 1
  )
  echo Extracting Maven...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%ARCHIVE_PATH%' -DestinationPath '%WRAPPER_DIR%' -Force"
  if errorlevel 1 (
    echo ERROR: Failed to extract Maven archive %ARCHIVE_PATH%
    exit /b 1
  )
)

if not exist "%MAVEN_BIN%" (
  echo ERROR: Maven binary not found at %MAVEN_BIN%
  exit /b 1
)

set "JAVA_MAJOR="
for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do set "JAVA_VER_RAW=%%~i"
if defined JAVA_VER_RAW (
  for /f "tokens=1,2 delims=." %%a in ("%JAVA_VER_RAW%") do (
    set "JAVA_FIRST=%%a"
    set "JAVA_SECOND=%%b"
  )
  if "%JAVA_FIRST%"=="1" (
    set "JAVA_MAJOR=%JAVA_SECOND%"
  ) else (
    set "JAVA_MAJOR=%JAVA_FIRST%"
  )
)

if not defined JAVA_MAJOR set "JAVA_MAJOR=0"
if %JAVA_MAJOR% LSS 17 (
  if not exist "%JDK_HOME%\bin\java.exe" (
    if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"
    echo Java 17 not found. Downloading Temurin JDK 17...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri '%JDK_URL%' -OutFile '%JDK_ARCHIVE%'"
    if errorlevel 1 (
      echo ERROR: Failed to download JDK 17 from %JDK_URL%
      exit /b 1
    )
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$dest='%WRAPPER_DIR%'; $jdk='%JDK_HOME%'; $tmp=Join-Path $dest 'jdk-tmp'; if(Test-Path $tmp){Remove-Item -Recurse -Force $tmp}; Expand-Archive -Path '%JDK_ARCHIVE%' -DestinationPath $tmp -Force; $dir=Get-ChildItem $tmp -Directory | Select-Object -First 1; if(-not $dir){throw 'No JDK directory found in archive'}; if(Test-Path $jdk){Remove-Item -Recurse -Force $jdk}; Move-Item -Path $dir.FullName -Destination $jdk; Remove-Item -Recurse -Force $tmp"
    if errorlevel 1 (
      echo ERROR: Failed to extract JDK 17 archive %JDK_ARCHIVE%
      exit /b 1
    )
  )
  set "JAVA_HOME=%JDK_HOME%"
  set "PATH=%JAVA_HOME%\bin;%PATH%"
)

call "%MAVEN_BIN%" %*
endlocal
