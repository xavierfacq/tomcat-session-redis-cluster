# tomcat-session-redis-cluster

This project provides an Apache Tomcat 7 SessionManager able to save and restore sessions into a Redis cluster, using the Jedis client.
This manager implements the serialization for authType and Principal, that can be usefull when a BASIC_AUTH is enabled.

Requirements: a configured and running Redis cluster


##How to get this Apache Tomcat 7 session manager

1°/ Clone this project

```
    git clone https://github.com/xavierfacq/tomcat-session-redis-cluster.git
```


2°/ Build with maven

```
    mvn clean install
```


3°/ Install jars into your Apache Tomcat 7

```
    cp jedis-2.9.0.jar $catalina_home/lib/
    cp commons-pool2-2.4.2.jar $catalina_home/lib/
    cp tomcat-session-redis-cluster.jar $catalina_home/lib/

```


4°/ Add the following snippet into your {catalina_home}/conf/context.xml

```xml
    <Manager className="org.apache.tomcat.session.redis.RedisClusterSessionManager" 
        nodes="172.16.50.12:7000,172.16.50.12:7001,172.16.50.12:7002" /> 
```


5°/ Restart your Apache Tomcat 7


Enjoy!
