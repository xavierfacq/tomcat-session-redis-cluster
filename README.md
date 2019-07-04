# Store your Tomcat sessions in your Redis Cluster

This project provides an Apache Tomcat 7 Manager able to save and restore sessions into a [Redis cluster](https://redis.io/topics/cluster-spec), using the [Jedis](https://github.com/xetorthio/jedis) client.
This manager implements the serialization for authType and Principal, that can be usefull when a BASIC_AUTH is enabled.
Redis operations are defered in executors for maximun fluidity.

Requirements: 
- JDK, Maven
- an Apache Tomcat
- a Redis cluster


## How do I use it?

1°/ Clone this project

```
    git clone https://github.com/xavierfacq/tomcat-session-redis-cluster.git
```


2°/ Build with maven

```
    mvn clean install
```


3°/ Install the session manager into your Apache Tomcat 7

You can choose the implementation driver you prefer by setting the value of the properties "implementation". Possible values are: jedis, lettuce

3.1 With Jedis

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

3.1.1 Install requested Jar files

```
    cp tomcat-session-redis-cluster.jar $catalina_home/lib/
    cp netty-*.jar $catalina_home/lib/
    cp reactive-streams-1.0.2.jar $catalina_home/lib/
    cp reactor-core-3.2.8.RELEASE.jar $catalina_home/lib/

```

3.1.2 Add the following snippet into your {catalina_home}/conf/context.xml

```xml
    <Manager className="org.apache.tomcat.session.redis.RedisClusterSessionManager" 
        nodes="172.16.50.12:7000,172.16.50.12:7001,172.16.50.12:7002" implementation="lettuce" /> 
```

4°/ Restart your Apache Tomcat 7


Enjoy!
