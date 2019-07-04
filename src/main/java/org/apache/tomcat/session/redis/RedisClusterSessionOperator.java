package org.apache.tomcat.session.redis;

import java.io.IOException;
import java.util.Map;

public interface RedisClusterSessionOperator {

	public void buildClient(String nodes, int timeout);

	public void shutdown() throws IOException ;

	public Map<String, String> getMap(String sessionId);

	public void del(String sessionKey);

	public void expire(String sessionKey, int expire);

	public void hset(String sessionKey, String field, String value);

	public void hdel(String sessionKey, String field);

	public void hmset(String sessionKey, Map<String, String> map);
}
