call "C:\Program Files\Java\javacc-6.0\bin\open_cmd.bat"
call jjtree JavaMM.jjt
call javacc JavaMM.jj
call javac JavaMM.java
call java JavaMM test.jmm
cmd