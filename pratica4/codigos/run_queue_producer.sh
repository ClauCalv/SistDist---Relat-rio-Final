export ZK=~/mcta025/zookeeper-3.4.14
echo "ZK=$ZK"
export CP_ZK=.:$ZK'/zookeeper-3.4.14.jar':$ZK'/lib/slf4j-log4j12-1.7.25.jar':$ZK'/lib/slf4j-api-1.7.25.jar':$ZK'/lib/log4j-1.2.17.jar'
echo "CP=$CP_ZK"
javac -cp $CP_ZK *.java
echo "***** Queue test - producer"
export SIZE=2
echo "Size = $SIZE"
java -cp $CP_ZK -Dlog4j.configuration=file:$ZK/conf/log4j.properties SyncPrimitive qTest localhost $SIZE p
