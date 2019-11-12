# Store your Tomcat sessions in your Redis Cluster

This project provides an Apache Tomcat Manager able to save and restore sessions into a [Redis cluster](https://redis.io/topics/cluster-spec), using the [Jedis](https://github.com/xetorthio/jedis) or [Lettuce](https://lettuce.io/) client.
This manager implements the serialization for authType and Principal, that can be usefull when a BASIC_AUTH is enabled.

Requirements:
- JDK (8, 9, 11), Maven
- an Apache Tomcat
- a Redis cluster


## Supports:
   - Apache Tomcat 7
   - Apache Tomcat 8
   - Apache Tomcat 9

   - Redis cluster 3.2
   - Redis cluster 5.0


## How do I use it?

1°/ Clone this project

```
    git clone https://github.com/xavierfacq/tomcat-session-redis-cluster.git
```


2°/ Build with maven

```
    mvn clean install
```


3°/ Install the session manager into your Apache Tomcat

You can choose the implementation driver you prefer by setting the value of the properties "implementation". Possible values are: jedis, lettuce

3.1 With Jedis

Get more informations about Lettuce: 
- https://github.com/xetorthio/jedis


3.1.1 Install requested Jar files

```
    cp tomcat-session-redis-cluster.jar $catalina_home/lib/
    cp jedis-2.9.0.jar $catalina_home/lib/
    cp commons-pool2-2.4.2.jar $catalina_home/lib/

```

3.1.2 Add the following snippet into your {catalina_home}/conf/context.xml

```xml
    <Manager className="org.apache.tomcat.session.redis.RedisClusterSessionManager" 
        nodes="172.16.50.12:7000,172.16.50.12:7001,172.16.50.12:7002" implementation="jedis" /> 
```

3.1 With Lettuce

Get more informations about Lettuce: 
- https://lettuce.io/
- https://github.com/lettuce-io


3.1.1 Install requested Jar files

```
    cp tomcat-session-redis-cluster.jar $catalina_home/lib/
    cp lettuce-core-5.2.1.RELEASE.jar $catalina_home/lib/
    cp netty-*.jar $catalina_home/lib/
    cp reactive-streams-1.0.2.jar $catalina_home/lib/
    cp reactor-core-3.2.11.RELEASE.jar $catalina_home/lib/

```

3.1.2 Add the following snippet into your {catalina_home}/conf/context.xml

```xml
    <Manager className="org.apache.tomcat.session.redis.RedisClusterSessionManager" 
        nodes="172.16.50.12:7000,172.16.50.12:7001,172.16.50.12:7002" implementation="lettuce" /> 
```

4°/ Restart your Apache Tomcat


Enjoy!
