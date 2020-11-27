set ZK="C:\temp\zookeeper-3.4.14"
set CP_ZK=.;%ZK%\zookeeper-3.4.14.jar;%ZK%\lib\slf4j-log4j12-1.7.25.jar;%ZK%\lib\slf4j-api-1.7.25.jar;%ZK%\lib\log4j-1.2.17.jar
java -cp %CP_ZK% -Dlog4j.configuration=file:%ZK%\conf\log4j.properties Executor localhost /mcta025/ex4 out.txt lista.bat