package org.apache.tomcat.session.redis;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Loader;
import org.apache.catalina.Session;
import org.apache.catalina.session.ManagerBase;
import org.apache.catalina.util.CustomObjectInputStream;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

/**
 * 
 * @author xfacq
 *
 */
public class RedisClusterSessionManager extends ManagerBase {

	private final Log log = LogFactory.getLog(RedisClusterSessionManager.class);

	// configuration
	private String nodes = null;
	private int timeout = Protocol.DEFAULT_TIMEOUT;

	private ClassLoader classLoader;
	private JedisCluster jedisCluster = null;

	public String getNodes() {
		return nodes;
	}

	public void setNodes(String nodes) {
		this.nodes = nodes;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public JedisCluster getJedisCluster() {
		return jedisCluster;
	}

	@Override
	public String getName() {
		return RedisClusterSessionManager.class.getSimpleName();
	}

	@Override
	public void load() throws ClassNotFoundException, IOException {
	}

	@Override
	public void unload() throws IOException {
	}

	@Override
	public Session createSession(String sessionId) {
		RedisClusterSession session = (RedisClusterSession) createEmptySession();

		session.setNew(true);
		session.setValid(true);
		session.setCreationTime(System.currentTimeMillis());
		session.setMaxInactiveInterval(maxInactiveInterval);

		if (sessionId == null) {
			sessionId = generateSessionId();
		}

		session.setId(sessionId);
		session.save();

		return session;
	}

	@Override
	public Session findSession(String sessionId) throws IOException {
		Session session = super.findSession(sessionId);

        if (session == null && sessionId != null) {
			Map<String, Object> attrs = getMap(sessionId);
			if (attrs.isEmpty() || !Boolean.valueOf(String.valueOf(attrs.get("session:isValid")))) {
                return null;
            }

			session = (RedisClusterSession) createEmptySession();
            session.setId(sessionId);
        	((RedisClusterSession)session).load(attrs);

            session.access();
            session.endAccess();
		}

		return session;
	}

	@Override
	public Session createEmptySession() {
		return new RedisClusterSession(this);
	}

	@Override
	public void remove(Session session, boolean update) {
		super.remove(session, update);

		if (session.getIdInternal() != null) {
			((RedisClusterSession)session).delete();
		}
	}

	@Override
	protected void startInternal() throws LifecycleException {
		super.startInternal();

		buildClient();

		setState(LifecycleState.STARTING);
	}

	protected void buildClient() throws LifecycleException {
		try {
			Set<HostAndPort> jedisClusterNodes = new HashSet<HostAndPort>();
			for (String server : getNodes().split(",")) {
				URI uri = URI.create("redis://" + server);
				jedisClusterNodes.add(new HostAndPort(uri.getHost(), uri.getPort()));
			}

			GenericObjectPoolConfig poolConfig = new JedisPoolConfig();
			poolConfig.setMaxTotal(200);
			poolConfig.setMaxIdle(20);
			poolConfig.setMinIdle(5);
			poolConfig.setMaxWaitMillis(TimeUnit.SECONDS.toMillis(timeout));

			jedisCluster = new JedisCluster(jedisClusterNodes, poolConfig);
		} catch (Exception e) {
			log.error("Cannot initialize client", e);
			throw new LifecycleException(e);
		}

		Loader loader = null;

		if (getContainer() != null) {
			loader = getContainer().getLoader();
		}

		if (loader != null) {
			classLoader = loader.getClassLoader();
		}
	}

	@Override
	protected void stopInternal() throws LifecycleException {
		super.stopInternal();

		setState(LifecycleState.STOPPING);

		try {
			if (jedisCluster != null) {
				jedisCluster.close();
			}
		} catch (Exception e) {
			log.error("Cannot stop", e);
			throw new LifecycleException(e);
		}
	}

	protected Object fromString(String s) throws IOException, ClassNotFoundException {
		Object o = null;
		if (s != null) {
			byte[] data = Base64.decode(s);
	        BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(data));
	        ObjectInputStream ois = new CustomObjectInputStream(bis, classLoader);
			o = ois.readObject();
			ois.close();
		}
		return o;
	}

	protected static String toString(Object o) throws IOException {
		 ByteArrayOutputStream baos = new ByteArrayOutputStream();
		 ObjectOutputStream oos = new ObjectOutputStream(baos);
		 oos.writeObject(o);
		 oos.flush();
		 oos.close();
		 return new String(Base64.encode(baos.toByteArray()));
	}

	protected static String buildSessionKey(Session session) {
		return session == null ? null : buildSessionKey(session.getId());
	}

	protected static String buildSessionKey(String sessionId) {
		return sessionId == null ? null : new StringBuilder("jedis_tomcat_session_").append(sessionId).toString();
	}

	private Map<String, Object> getMap(String sessionId) {
		Map<String, Object> attrs = new HashMap<String, Object>();

		try {
			Map<String, String> entries = jedisCluster.hgetAll(buildSessionKey(sessionId));
			if(entries != null) {
				for(Entry<String, String> entry : entries.entrySet()) {
					attrs.put(entry.getKey(), fromString(entry.getValue()));
				}
			}
		} catch (Exception e) {
			log.error("Cannot get map", e);
		}

		return attrs;
	}
}
