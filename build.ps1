# build.ps1 - rebuild the standalone app from scratch
# Run with:  powershell -ExecutionPolicy Bypass -File build.ps1

# 1. Compile and build the mini Java (Java + JavaFX + our app). Stop if it fails.
mvn clean javafx:jlink
if ($LASTEXITCODE -ne 0) { Write-Host "Build failed"; exit 1 }

# 2. Copy the database driver (H2) into target/drivers.
#    H2 is not a full Java module, so jlink cannot put it INSIDE the mini Java.
#    Instead it ships next to the app, and the app loads it from there (step 4).
mvn dependency:copy-dependencies "-DincludeArtifactIds=h2" "-DoutputDirectory=target/drivers"
if ($LASTEXITCODE -ne 0) { Write-Host "Copying the database driver failed"; exit 1 }

# 3. Delete the old app, because jpackage will not overwrite it.
#    (Your saved data is NOT in dist - it is in C:\Users\<you>\.qasystem - so it is safe.)
Remove-Item -Recurse -Force dist -ErrorAction SilentlyContinue

# 4. Package it as a folder with QA-System.exe
#    --app-content   copies target/drivers into the app folder (QA-System\drivers)
#    --module-path   tells Java to look for modules (H2) in that folder
#    The SINGLE quotes matter: in double quotes PowerShell would replace $APPDIR with nothing.
#    jpackage itself fills in $APPDIR when the app starts.
jpackage --name "QA-System" `
  --runtime-image target/qasystem-runtime `
  --module qasystem/qasystem.App `
  --app-content target/drivers `
  --type app-image `
  --dest dist `
  --app-version 1.1 `
  --java-options "--enable-native-access=javafx.graphics" `
  --java-options '--module-path $APPDIR/../drivers'
if ($LASTEXITCODE -ne 0) { Write-Host "Packaging failed"; exit 1 }

Write-Host "Done: dist\QA-System\QA-System.exe"
