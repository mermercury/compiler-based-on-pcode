@echo off

for /L %%a in (1, 1, 30) do (
java -jar my_pcodeV.jar  "D:\testBox\A\testfile%%a.txt"  my_ans.txt < "D:\testBox\A\input%%a.txt"
fc my_ans.txt "D:\testBox\A\output%%a.txt"
)
pause