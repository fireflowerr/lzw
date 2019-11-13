@echo off
echo initializing build dir
set workingdir=%~dp0
IF NOT EXIST target (
    md target
) 
cd target

set builddir=%cd%
IF NOT EXIST classes (
    md classes
) 

echo compiling class files
set srcdir=%workingdir%src\main\java
FOR /R %workingdir%src %%f IN (*.java) DO (
    javac -cp %srcdir% -d %builddir%\classes %%f
)

echo packaging lzw.jar
jar -cfe lzw.jar lamar.app.App -C classes lamar

echo packaging lzw-test.jar
jar -cfe lzw-test.jar lamar.app.Test -C classes lamar

cd %workingdir%
echo build complete!