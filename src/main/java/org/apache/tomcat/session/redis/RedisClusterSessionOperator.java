package org.apache.tomcat.session.redis;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public interface RedisClusterSessionOperator {

	public void buildClient(String nodes, int timeout);

	public void shutdown() throws IOException ;

	public Map<String, String> getMap(String sessionKey);

	public void del(String sessionKey);

	public void expire(String sessionKey, int expire);

	public void hset(String sessionKey, String field, String value);

	public void hdel(String sessionKey, String field);

	public void hmset(String sessionKey, Map<String, String> map);

	public Set<String> hkeys(String sessionKey);

	public String hget(String sessionKey, String field);
}
