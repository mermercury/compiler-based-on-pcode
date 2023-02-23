@echo off

for /L %%a in (1, 1, 30) do (
java -jar my_pcodeV.jar  "D:\testBox\B\testfile%%a.txt"  my_ans.txt < "D:\testBox\B\input%%a.txt"
fc my_ans.txt "D:\testBox\B\output%%a.txt"
)
pause