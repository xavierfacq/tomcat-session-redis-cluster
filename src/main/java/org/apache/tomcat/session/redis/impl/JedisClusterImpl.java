package org.apache.tomcat.session.redis.impl;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.tomcat.session.redis.RedisClusterSessionOperator;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

public class JedisClusterImpl implements RedisClusterSessionOperator {

	private JedisCluster jedisCluster = null;

	@Override
	public void buildClient(String nodes, int timeout) {
		Set<HostAndPort> jedisClusterNodes = new HashSet<HostAndPort>();
		for (String server : nodes.split(",")) {
			URI uri = URI.create("redis://" + server);
			jedisClusterNodes.add(new HostAndPort(uri.getHost(), uri.getPort()));
		}

		GenericObjectPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxTotal(200);
		poolConfig.setMaxIdle(20);
		poolConfig.setMinIdle(5);

		poolConfig.setMaxWaitMillis(TimeUnit.SECONDS.toMillis(timeout <= 0 ? Protocol.DEFAULT_TIMEOUT : timeout));

		jedisCluster = new JedisCluster(jedisClusterNodes, poolConfig);
	}

	@Override
	public void shutdown() throws IOException {
		if (jedisCluster != null) {
			jedisCluster.close();
		}
	}

	@Override
	public Map<String, String> getMap(String sessionId) {
		return jedisCluster.hgetAll(sessionId);
	}

	@Override
	public void del(String sessionKey) {
		jedisCluster.del(sessionKey);
	}

	@Override
	public void expire(String sessionKey, int expire) {
		jedisCluster.expire(sessionKey, expire);
	}

	@Override
	public void hset(String sessionKey, String field, String value) {
		jedisCluster.hset(sessionKey, field, value);
	}

	@Override
	public void hdel(String sessionKey, String field) {
		jedisCluster.hdel(sessionKey, field);
	}

	@Override
	public void hmset(String sessionKey, Map<String, String> map) {
		jedisCluster.hmset(sessionKey, map);
	}
}
