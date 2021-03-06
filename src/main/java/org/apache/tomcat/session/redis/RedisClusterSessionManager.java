package org.apache.tomcat.session.redis;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Loader;
import org.apache.catalina.Session;
import org.apache.catalina.session.ManagerBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.session.redis.impl.JedisClusterImpl;
import org.apache.tomcat.session.redis.impl.LettuceClusterImpl;

/**
 * 
 * @author xfacq
 *
 */
public class RedisClusterSessionManager extends ManagerBase implements Lifecycle {

	private static final String NAME = "RedisClusterSessionManager";

	private final static String prefix_key = "redis_cluster_tomcat_session_";
	private final static Log log = LogFactory.getLog(RedisClusterSessionManager.class);

	// configuration
	private String nodes = null;
	private String implementation = null;
	private int timeout = 0;

	private RedisClusterSessionSerializer serializer;
	private RedisClusterSessionOperator redisClusterSessionOperator;

	public String getNodes() {
		return nodes;
	}

	public void setNodes(String nodes) {
		this.nodes = nodes;
	}

	public String getImplementation() {
		return implementation;
	}

	public void setImplementation(String implementation) {
		this.implementation = implementation;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public RedisClusterSessionSerializer getSerializer() {
		return serializer;
	}

	@Override
	public String getName() {
		return NAME;
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
		session.setMaxInactiveInterval(getContextInternal().getSessionTimeout() * 60);

		if (sessionId == null) {
			sessionId = generateSessionId();
		}

		session.setId(sessionId);

		return session;
	}

	/**
	 * This method is to be compliant with Tomcat 7 and Tomcat 8 
	 * 
	 * @return
	 */
	protected Context getContextInternal() {
		try {
			Method method = this.getClass().getSuperclass().getDeclaredMethod("getContext");
			return (Context) method.invoke(this);
		} catch (Exception ex) {
			try {
				Method method = this.getClass().getSuperclass().getDeclaredMethod("getContainer");
				return (Context) method.invoke(this);
			} catch (Exception exception) {
				log.error("Cannot find context", exception);
			}
		}

		throw new RuntimeException("Error occurred while looking for a context");
	}

	@Override
	public Session findSession(String sessionId) throws IOException {
		RedisClusterSession session = (RedisClusterSession)super.findSession(sessionId);

        if (session == null && sessionId != null) {
			Map<String, Object> attrs = getMap(sessionId);

			if (attrs.isEmpty() || !Boolean.valueOf(String.valueOf(attrs.get("session:isValid")))) {
                return null;
            }

			session = createEmptySession();

			// load backed params
			session.load(attrs);

			// update access
			session.access();
			session.endAccess();

			// this will fire the save
			session.setId(sessionId);

            session.activate();
		}

		return session;
	}

	@Override
	public RedisClusterSession createEmptySession() {
		return new RedisClusterSession(this);
	}

	@Override
	public void remove(Session session, boolean update) {
		super.remove(session, update);
		((RedisClusterSession)session).delete();
	}

	@Override
	public void add(Session session) {
		super.add(session);
		((RedisClusterSession)session).save();
	}

	@Override
	protected void startInternal() throws LifecycleException {
		super.startInternal();

		buildClient();

		setState(LifecycleState.STARTING);
	}

	protected void buildClient() throws LifecycleException {
		try {
			if("jedis".equalsIgnoreCase(implementation)) {
				redisClusterSessionOperator = new JedisClusterImpl();
			} else if("lettuce".equalsIgnoreCase(implementation)) {
				redisClusterSessionOperator = new LettuceClusterImpl();
			}

			redisClusterSessionOperator.buildClient(nodes, timeout);
		} catch (Exception e) {
			log.error("Cannot initialize client", e);
			throw new LifecycleException(e);
		}

		Loader loader = null;

		Context context = getContextInternal();
		if (context != null) {
			loader = context.getLoader();
		}

		if (loader != null) {
			serializer = new RedisClusterSessionSerializer(loader.getClassLoader());
		} else {
			log.error("Cannot find loader");
			throw new LifecycleException("Cannot find loader");
		}
	}

	@Override
	protected void stopInternal() throws LifecycleException {
		setState(LifecycleState.STOPPING);

		try {
			redisClusterSessionOperator.shutdown();
		} catch (Exception e) {
			log.error("Cannot stop", e);
			throw new LifecycleException(e);
		}

		super.stopInternal();
	}

	protected static String buildSessionKey(Session session) {
		return session == null ? null : buildSessionKey(session.getId());
	}

	protected static String buildSessionKey(String sessionId) {
		return sessionId == null ? null : new StringBuilder(prefix_key).append(sessionId).toString();
	}

	private Map<String, Object> getMap(String sessionId) {
		Map<String, Object> attrs = new HashMap<String, Object>();

		try {
			Map<String, String> entries = redisClusterSessionOperator.getMap(buildSessionKey(sessionId));
			if(entries != null && !entries.isEmpty()) {
				for(Entry<String, String> entry : entries.entrySet()) {
					attrs.put(entry.getKey(), serializer.deserialize(entry.getValue()));
				}
			}
		} catch (Exception e) {
			log.error("Cannot get map", e);
		}

		return attrs;
	}

	protected RedisClusterSessionOperator getSessionOperator() {
		return redisClusterSessionOperator;
	}
}
