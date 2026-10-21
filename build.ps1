# build.ps1 - rebuild

# 1. Compile and build the mini Java (stop if it fails)
mvn clean javafx:jlink
if ($LASTEXITCODE -ne 0) { Write-Host "Build failed"; exit 1 }

# 2. Delete the old app, because jpackage will not overwrite it
Remove-Item -Recurse -Force dist -ErrorAction SilentlyContinue

# 3. Package it as a folder with QA-System.exe
jpackage --name "QA-System" `
  --runtime-image target/qasystem-runtime `
  --module qasystem/qasystem.App `
  --type app-image `
  --dest dist `
  --app-version 1.0 `
  --java-options "--enable-native-access=javafx.graphics"

Write-Host "Done: dist\QA-System\QA-System.exe"