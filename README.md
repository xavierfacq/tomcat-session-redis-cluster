# tomcat-session-redis-cluster

This project is:

1°/ Build this project

2°/ Copy jedis-2.9.0.jar, commons-pool2-2.4.2.jar & redis-cluster-session.jar into your {catalina_home}/lib/

3°/ Add the following Manager into your {catalina_home}/conf/context.xml

```xml
    <Manager className="org.apache.tomcat.session.redis.RedisClusterSessionManager" 
        nodes="172.16.50.12:7000,172.16.50.12:7001,172.16.50.12:7002" /> 
```
