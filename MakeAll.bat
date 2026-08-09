javac *.java
dir *.class
del Galler*.class
jar -cfm Tyler.jar Tyler.mf *.class
del *.class
cd ..
jar -cfM tyler\Tyler.src.zip tyler\*.java tyler\todo.txt tyler\MakeAll.bat tyler\Makefile tyler\Tyler.htm tyler\TylerSave.php tyler\Tyler.mf
cd tyler


