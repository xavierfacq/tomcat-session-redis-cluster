package org.apache.tomcat.session.redis;

import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.catalina.Context;
import org.apache.catalina.ha.session.SerializablePrincipal;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.session.StandardSession;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * TODO: notes are not backupped
 * 
 * @author xfacq
 *
 */
public class RedisClusterSession extends StandardSession {

	private static final long serialVersionUID = -2518607181636076487L;
	private static final Log log = LogFactory.getLog(RedisClusterSession.class);

	public RedisClusterSession(RedisClusterSessionManager manager) {
		super(manager);
	}

	@Override
	public RedisClusterSessionManager getManager() {
		return (RedisClusterSessionManager) manager;
	}

	@Override
	public Object getAttribute(String name) {
		if (this.id != null) {
			try {
				return getManager().fromString(getManager().getJedisCluster().hget(getJedisSessionKey(), name));
			} catch (Exception exception) {
				log.error("Cannot get attribute", exception);
				return null;
			}
		}
		return null;
	}

	@Override
	public void setId(String id, boolean notify) {
		super.setId(id, notify);
	}

	@Override
	public void setCreationTime(long time) {
		super.setCreationTime(time);

		if (this.id != null) {
			try {
				Map<String, String> newMap = new HashMap<String, String>(3);
				newMap.put("session:creationTime", RedisClusterSessionManager.toString(creationTime));
				newMap.put("session:lastAccessedTime", RedisClusterSessionManager.toString(lastAccessedTime));
				newMap.put("session:thisAccessedTime", RedisClusterSessionManager.toString(thisAccessedTime));
				getManager().getJedisCluster().hmset(getJedisSessionKey(), newMap);
			} catch (Exception exception) {
				log.error("Cannot set Creation Time", exception);
			}
		}
	}

	@Override
	public void access() {
		super.access();

		if (this.id != null) {
			try {
				getManager().getJedisCluster().hset(getJedisSessionKey(), "session:thisAccessedTime",
						RedisClusterSessionManager.toString(thisAccessedTime));

				if (getExpire() >= 0) {
					getManager().getJedisCluster().expire(getJedisSessionKey(), getExpire());
				}
			} catch (Exception exception) {
				log.error("Cannot update access", exception);
			}
		}
	}

	@Override
	public void setMaxInactiveInterval(int interval) {
		super.setMaxInactiveInterval(interval);

		if (this.id != null) {
			try {
				getManager().getJedisCluster().hset(getJedisSessionKey(), "session:maxInactiveInterval",
						RedisClusterSessionManager.toString(maxInactiveInterval));

				if (getExpire() >= 0) {
					getManager().getJedisCluster().expire(getJedisSessionKey(), getExpire());
				}
			} catch (Exception exception) {
				log.error("Cannot set Max Inactive Interval", exception);
			}
		}
	}

	@Override
	public void setValid(boolean isValid) {
		super.setValid(isValid);

		if (this.id != null) {
			try {
				getManager().getJedisCluster().hset(getJedisSessionKey(), "session:isValid",
						RedisClusterSessionManager.toString(isValid));
			} catch (Exception exception) {
				log.error("Cannot set is valid", exception);
			}
		}
	}

	@Override
	public void setNew(boolean isNew) {
		super.setNew(isNew);

		if (this.id != null) {
			try {
				getManager().getJedisCluster().hset(getJedisSessionKey(), "session:isNew",
						RedisClusterSessionManager.toString(isNew));
			} catch (Exception exception) {
				log.error("Cannot set is new", exception);
			}
		}
	}

	@Override
	public void endAccess() {
		super.endAccess();

		if (this.id != null) {
			try {
				Map<String, String> newMap = new HashMap<String, String>(3);
				newMap.put("session:lastAccessedTime", RedisClusterSessionManager.toString(lastAccessedTime));
				newMap.put("session:thisAccessedTime", RedisClusterSessionManager.toString(thisAccessedTime));
				newMap.put("session:isNew", RedisClusterSessionManager.toString(isNew));
				getManager().getJedisCluster().hmset(getJedisSessionKey(), newMap);
			} catch (Exception exception) {
				log.error("Cannot set end access", exception);
			}
		}
	}

	@Override
	public void setAttribute(String name, Object value, boolean notify) {
		super.setAttribute(name, value, notify);

		if (this.id != null && name != null && value != null) {
			try {
				getManager().getJedisCluster().hset(getJedisSessionKey(), name,
						RedisClusterSessionManager.toString(value));
			} catch (Exception exception) {
				log.error("Cannot set attribute", exception);
			}
		}
	}

	@Override
	protected void removeAttributeInternal(String name, boolean notify) {
		super.removeAttributeInternal(name, notify);

		if (this.id != null && name != null) {
			try {
				getManager().getJedisCluster().hdel(getJedisSessionKey(), name);
			} catch (Exception exception) {
				log.error("Cannot remove attribute", exception);
			}
		}
	}

	protected void save() {
		try {
			Map<String, String> newMap = new HashMap<String, String>();
			newMap.put("session:creationTime", RedisClusterSessionManager.toString(creationTime));
			newMap.put("session:lastAccessedTime", RedisClusterSessionManager.toString(lastAccessedTime));
			newMap.put("session:thisAccessedTime", RedisClusterSessionManager.toString(thisAccessedTime));
			newMap.put("session:maxInactiveInterval", RedisClusterSessionManager.toString(maxInactiveInterval));
			newMap.put("session:isValid", RedisClusterSessionManager.toString(isValid));
			newMap.put("session:isNew", RedisClusterSessionManager.toString(isNew));
			newMap.put("session:authType", authType == null ? null : RedisClusterSessionManager.toString(authType));
			newMap.put("session:principal",
					principal == null ? null
							: RedisClusterSessionManager
									.toString(SerializablePrincipal.createPrincipal((GenericPrincipal) principal)));

			for (Enumeration<String> e = getAttributeNames(); e.hasMoreElements();) {
				String key = e.nextElement();
				Object o = super.getAttribute(key);
				if (o != null)
					newMap.put(key, RedisClusterSessionManager.toString(o));
			}

			getManager().getJedisCluster().hmset(getJedisSessionKey(), newMap);

			if (getExpire() >= 0) {
				getManager().getJedisCluster().expire(getJedisSessionKey(), getExpire());
			}
		} catch (Exception exception) {
			log.error("Cannot save", exception);
		}
	}

	public void load(Map<String, Object> attrs) {
		if (attrs == null)
			return;
		Long creationTime = (Long) attrs.remove("session:creationTime");
		if (creationTime != null) {
			this.creationTime = creationTime;
		}
		Long lastAccessedTime = (Long) attrs.remove("session:lastAccessedTime");
		if (lastAccessedTime != null) {
			this.lastAccessedTime = lastAccessedTime;
		}
		Long thisAccessedTime = (Long) attrs.remove("session:thisAccessedTime");
		if (thisAccessedTime != null) {
			this.thisAccessedTime = thisAccessedTime;
		}
		Integer maxInactiveInterval = (Integer) attrs.remove("session:maxInactiveInterval");
		if (maxInactiveInterval != null) {
			this.maxInactiveInterval = maxInactiveInterval;
		}
		Boolean isValid = (Boolean) attrs.remove("session:isValid");
		if (isValid != null) {
			this.isValid = isValid;
		}
		Boolean isNew = (Boolean) attrs.remove("session:isNew");
		if (isNew != null) {
			this.isNew = isNew;
		}
		String authType = (String) attrs.remove("session:authType");
		this.authType = authType;

		SerializablePrincipal principal = (SerializablePrincipal) attrs.remove("session:principal");
		this.principal = principal.getPrincipal();

		for (Entry<String, Object> entry : attrs.entrySet()) {
			setAttribute(entry.getKey(), entry.getValue(), false);
		}
	}

	private int getExpire() {
		return ((Context) manager.getContainer()).getSessionTimeout() * 60;
	}

	protected String getJedisSessionKey() {
		return RedisClusterSessionManager.buildSessionKey(this);
	}

	@Override
	public void setAuthType(String authType) {
		super.setAuthType(authType);

		if (this.id != null) {
			try {
				getManager().getJedisCluster().hset(getJedisSessionKey(), "session:authType",
						authType == null ? null : RedisClusterSessionManager.toString(authType));
			} catch (Exception exception) {
				log.error("Cannot set authType", exception);
			}
		}
	}

	@Override
	public void setPrincipal(Principal principal) {
		super.setPrincipal(principal);

		if (this.id != null) {
			try {
				getManager().getJedisCluster().hset(getJedisSessionKey(), "session:principal",
						principal == null ? null
								: RedisClusterSessionManager
										.toString(SerializablePrincipal.createPrincipal((GenericPrincipal) principal)));
			} catch (Exception exception) {
				log.error("Cannot set principal", exception);
			}
		}
	}
}
