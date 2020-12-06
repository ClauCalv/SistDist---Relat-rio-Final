set ZK="C:\Program Files\Zookeeper"
set CP_ZK=.;%ZK%\zookeeper-3.4.14.jar;%ZK%\lib\slf4j-log4j12-1.7.25.jar;%ZK%\lib\slf4j-api-1.7.25.jar;%ZK%\lib\log4j-1.2.17.jar
javac -cp %CP_ZK% *.java
java -cp %CP_ZK% ClientCLI localhost