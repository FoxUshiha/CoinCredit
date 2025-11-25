@echo off
setlocal ENABLEDELAYEDEXPANSION
pushd "%~dp0"

REM ====== CONFIG ======
set "SPIGOT=spigot-api-1.21.6-R0.1-SNAPSHOT.jar"
set "VAULT=Vault.jar"
set "OUTJAR=CoinCredit.jar"
set "SRCPATH=src\com\foxsrv\credit"

REM ====== PREP ======
echo.
echo === Preparando diretórios ===
if not exist out mkdir out
if not exist out\classes mkdir out\classes
if exist out\%OUTJAR% del /q out\%OUTJAR%

REM ====== AMBIENTE ======
echo === Verificando javac e jars ===
where javac >nul 2>nul
if errorlevel 1 (
  echo [ERRO] javac nao encontrado. Instale o JDK ou ajuste JAVA_HOME/PATH.
  pause
  popd
  exit /b 1
)

if not exist "%SPIGOT%" (
  echo [ERRO] Spigot API nao encontrado: %SPIGOT%
  echo Coloque %SPIGOT% ao lado deste start.bat
  pause
  popd
  exit /b 1
)

if not exist "%VAULT%" (
  echo [AVISO] Vault.jar nao encontrado: %VAULT% - continuando sem Vault
  set "VAULT="
)

REM ====== COLETAR FONTES ======
echo.
echo === Coletando arquivos .java em %SRCPATH% ===
set "FILES="
for /R "%SRCPATH%" %%F in (*.java) do (
  set "FILES=!FILES! "%%~fF""
)

if "!FILES!"=="" (
  echo [ERRO] Nenhum arquivo .java encontrado em %SRCPATH%.
  pause
  popd
  exit /b 1
)

echo Arquivos coletados:
for /F "tokens=*" %%L in ('echo !FILES!') do (
  echo %%L
)

REM ====== COMPILAR ======
echo.
echo === Compilando CoinCredit ===
javac -encoding UTF-8 -Xlint:deprecation -Xlint:unchecked -classpath ".;%SPIGOT%;%VAULT%" -d out\classes !FILES!
if errorlevel 1 (
  echo.
  echo [ERRO] Falha na compilacao. Reveja os erros acima.
  pause
  popd
  exit /b 1
)

REM ====== COPIAR RECURSOS PARA O JAR ======
echo.
echo === Copiando resources para dentro do jar ===
if exist plugin.yml copy /Y plugin.yml out\classes >nul
if exist config.yml copy /Y config.yml out\classes >nul
if exist resources\plugin.yml copy /Y resources\plugin.yml out\classes >nul
if exist resources\config.yml copy /Y resources\config.yml out\classes >nul
if exist resources\users.yml copy /Y resources\users.yml out\classes >nul
if exist users.yml copy /Y users.yml out\classes >nul

REM opcional: semear pasta de dados do servidor
if not exist "plugins\CoinCredit" mkdir "plugins\CoinCredit"
if exist resources\config.yml copy /Y resources\config.yml "plugins\CoinCredit\" >nul
if exist config.yml copy /Y config.yml "plugins\CoinCredit\" >nul
if exist resources\users.yml copy /Y resources\users.yml "plugins\CoinCredit\" >nul
if exist users.yml copy /Y users.yml "plugins\CoinCredit\" >nul

REM ====== EMPACOTAR ======
echo.
echo === Empacotando JAR: out\%OUTJAR% ===

REM tenta usar %JAVA_HOME%\bin\jar primeiro
set "JARCMD="
if defined JAVA_HOME (
  if exist "%JAVA_HOME%\bin\jar.exe" (
    set "JARCMD=%JAVA_HOME%\bin\jar"
  )
)

REM se nao, tenta comando jar no PATH
if not defined JARCMD (
  where jar >nul 2>nul
  if not errorlevel 1 (
    set "JARCMD=jar"
  )
)

if not defined JARCMD (
  echo [ERRO] comando 'jar' nao encontrado nem em %%JAVA_HOME%%\\bin nem no PATH.
  echo Instale o JDK e/ou ajuste JAVA_HOME e PATH para incluir as ferramentas do JDK.
  pause
  popd
  exit /b 1
)

pushd out\classes
if exist "..\%OUTJAR%" del /q "..\%OUTJAR%"
"%JARCMD%" cvf ..\%OUTJAR% * >nul 2>nul
if errorlevel 1 (
  echo [ERRO] Falha ao criar o jar usando: %JARCMD%
  echo Tentando sem redirecionamento para ver o erro...
  "%JARCMD%" cvf ..\%OUTJAR% *
  if errorlevel 1 (
    echo [ERRO] Ainda falhou ao criar o jar.
    popd
    pause
    popd
    exit /b 1
  )
)
popd

echo.
echo === Build concluido com sucesso: out\%OUTJAR% ===
echo Copie out\%OUTJAR% para a pasta plugins do servidor e reinicie o servidor (removendo o plugin antigo).
pause
popd
