package org.apache.tomcat.session.redis.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.tomcat.session.redis.RedisClusterSessionOperator;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;

public class LettuceClusterImpl implements RedisClusterSessionOperator {

	private RedisClusterClient clusterClient;
	private StatefulRedisClusterConnection<String, String> connection;

	public StatefulRedisClusterConnection<String, String> getRedisCluster() {
		return connection;
	}

	@Override
	public void buildClient(String nodes, int timeout) {
		List<RedisURI> redisURIs = new ArrayList<>();
		for(String server : nodes.split(",")) {
			URI uri = URI.create("redis://" + server);
			RedisURI node = RedisURI.create(uri.getHost(), uri.getPort());
			redisURIs.add(node);
		}

		clusterClient = RedisClusterClient.create(redisURIs);

		ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
				.enablePeriodicRefresh()
                .enableAllAdaptiveRefreshTriggers()
				.build();

		clusterClient.setOptions(
				ClusterClientOptions.builder().topologyRefreshOptions(topologyRefreshOptions).build());

		connection = clusterClient.connect();
		connection.setReadFrom(ReadFrom.SLAVE_PREFERRED);
	}

	@Override
	public void shutdown() {
		if (connection != null) {
			connection.close();
		}

		if (clusterClient != null) {
			clusterClient.shutdown();
		}
	}

	@Override
	public Map<String, String> getMap(String sessionId) {
		return connection.sync().hgetall(sessionId);
	}

	@Override
	public void del(String sessionKey) {
		connection.sync().del(sessionKey);
	}

	@Override
	public void expire(String sessionKey, int expire) {
		connection.sync().expire(sessionKey, expire);
	}

	@Override
	public void hset(String sessionKey, String field, String value) {
		connection.sync().hset(sessionKey, field, value);
	}

	@Override
	public void hdel(String sessionKey, String field) {
		connection.sync().hdel(sessionKey, field);
	}

	@Override
	public void hmset(String sessionKey, Map<String, String> map) {
		connection.sync().hmset(sessionKey, map);
	}
}
