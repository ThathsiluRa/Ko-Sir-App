@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script
@REM Automatically downloads Maven if not already cached, then runs it.
@REM Usage: mvnw.cmd spring-boot:run
@REM ----------------------------------------------------------------------------
@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET __ MVNW_CMD__=%MAVEN_PROJECTBASEDIR%
@setlocal
@SET MAVEN_PROJECTBASEDIR=%~dp0
@SET MAVEN_WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar
@SET MAVEN_WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties
@SET DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar

@FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_WRAPPER_PROPERTIES%") DO (
    @IF "%%A"=="distributionUrl" SET DISTRIBUTION_URL=%%B
)

@IF NOT EXIST "%MAVEN_WRAPPER_JAR%" (
    @IF "%MVNW_VERBOSE%"=="true" echo Downloading %DOWNLOAD_URL% to %MAVEN_WRAPPER_JAR%
    @powershell -Command "&{"^
    "$webclient = new-object System.Net.WebClient;"^
    "if (-not ([string]::IsNullOrEmpty('%MVNW_USERNAME%') -and [string]::IsNullOrEmpty('%MVNW_PASSWORD%'))) {"^
    "$webclient.Credentials = new-object System.Net.NetworkCredential('%MVNW_USERNAME%', '%MVNW_PASSWORD%');"^
    "}"^
    "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $webclient.DownloadFile('%DOWNLOAD_URL%', '%MAVEN_WRAPPER_JAR%')"^
    "}"
    @IF "%MVNW_VERBOSE%"=="true" echo Finished downloading %DOWNLOAD_URL%
)

@SET MAVEN_JAVA_EXE=java
@IF NOT "%JAVA_HOME%"=="" SET MAVEN_JAVA_EXE=%JAVA_HOME%\bin\java

@SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
@%MAVEN_JAVA_EXE% ^
  %JVM_CONFIG_MAVEN_PROPS% ^
  %MAVEN_OPTS% ^
  %MAVEN_DEBUG_OPTS% ^
  -classpath "%MAVEN_WRAPPER_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  %WRAPPER_LAUNCHER% %MAVEN_CONFIG% %*
@IF ERRORLEVEL 1 GOTO error
@GOTO end
:error
SET ERROR_CODE=1
:end
@ENDLOCAL & SET ERROR_CODE=%ERROR_CODE%
@IF NOT "0"=="%ERROR_CODE%" cmd /C exit /B %ERROR_CODE%
