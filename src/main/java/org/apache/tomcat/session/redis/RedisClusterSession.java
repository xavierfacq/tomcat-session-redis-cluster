package org.apache.tomcat.session.redis;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

	private RedisClusterSessionSerializer getSerializer() {
		return getManager().getSerializer();
	}

	@Override
	public RedisClusterSessionManager getManager() {
		return (RedisClusterSessionManager) manager;
	}

	@Override
	public void setId(String id, boolean notify) {
		super.setId(id, notify);
	}

	@Override
	public void setCreationTime(long time) {
		if(this.creationTime == time) return;
		super.setCreationTime(time);

		if (this.id != null) {
			try {
				Map<String, String> newMap = new HashMap<String, String>(3);
				newMap.put("session:creationTime", getSerializer().serialize(creationTime));
				newMap.put("session:lastAccessedTime", getSerializer().serialize(lastAccessedTime));
				newMap.put("session:thisAccessedTime", getSerializer().serialize(thisAccessedTime));

				getManager().getSessionOperator()
					.hmset(getSessionKey(), newMap);

				if (getExpire() > 0) {
					getManager().getSessionOperator()
						.expire(getSessionKey(), getExpire());
				}
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
				getManager().getSessionOperator()
					.hset(getSessionKey(), "session:thisAccessedTime", getSerializer().serialize(thisAccessedTime));

				if (getExpire() > 0) {
					getManager().getSessionOperator()
						.expire(getSessionKey(), getExpire());
				}
			} catch (Exception exception) {
				log.error("Cannot update access", exception);
			}
		}
	}

	@Override
	public void setMaxInactiveInterval(int interval) {
		if(this.maxInactiveInterval == interval) return;
		super.setMaxInactiveInterval(interval);

		if (this.id != null) {
			try {
				getManager().getSessionOperator()
					.hset(getSessionKey(), "session:maxInactiveInterval", getSerializer().serialize(maxInactiveInterval));
			} catch (Exception exception) {
				log.error("Cannot set Max Inactive Interval", exception);
			}
		}
	}

	@Override
	public void setValid(boolean isValid) {
		if(this.isValid == isValid) return;
		super.setValid(isValid);

		if (this.id != null) {
			try {
				getManager().getSessionOperator()
					.hset(getSessionKey(), "session:isValid", getSerializer().serialize(isValid));
			} catch (Exception exception) {
				log.error("Cannot set is valid", exception);
			}
		}
	}

	@Override
	public void setNew(boolean isNew) {
		if(this.isNew == isNew) return;
		super.setNew(isNew);

		if (this.id != null) {
			try {
				getManager().getSessionOperator()
					.hset(getSessionKey(), "session:isNew", getSerializer().serialize(isNew));
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
				newMap.put("session:lastAccessedTime", getSerializer().serialize(lastAccessedTime));
				newMap.put("session:thisAccessedTime", getSerializer().serialize(thisAccessedTime));

				getManager().getSessionOperator()
					.hmset(getSessionKey(), newMap);
				
				if (getExpire() > 0) {
					getManager().getSessionOperator()
						.expire(getSessionKey(), getExpire());
				}
			} catch (Exception exception) {
				log.error("Cannot set end access", exception);
			}
		}
	}

	@Override
	public Object getAttribute(String name) {
		if (this.id != null && name != null) {
			try {
				String value = getManager().getSessionOperator()
						.hget(getSessionKey(), name);

				return getSerializer().deserialize(value);
			} catch (ClassNotFoundException|IOException exception) {
				log.error("Cannot get attribute", exception);
			}
		}
		return null;
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		if (this.id != null) {
			try {
				return Collections.enumeration(getManager().getSessionOperator().hkeys(getSessionKey()));
			} catch (Exception exception) {
				log.error("Cannot get attribute names", exception);
			}
		}
		return Collections.emptyEnumeration();
	}

	@Override
    @Deprecated
	public String[] getValueNames() {
		if (this.id != null) {
			try {
				Set<String> keys = getManager().getSessionOperator()
						.hkeys(getSessionKey());

				return keys.toArray(new String[keys.size()]);
			} catch (Exception exception) {
				log.error("Cannot get value names", exception);
			}
		}
		return null;
	}

	@Override
	public void setAttribute(String name, Object value, boolean notify) {
		// NOTE: Null value is the same as removeAttribute() - checked & called by super.setAttribute()

		// retrieve current value of this attribute
		Object o = super.getAttribute(name);

		super.setAttribute(name, value, notify);

		if (this.id != null && name != null && value != null) {
			try {
				String outboundValue = getSerializer().serialize(o);
				String inboundValue = getSerializer().serialize(value);

				// only hset() the cluster if the value has really changed
				if(!outboundValue.equals(inboundValue)) {
					getManager().getSessionOperator()
						.hset(getSessionKey(), name, inboundValue);
				}
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
				getManager().getSessionOperator()
					.hdel(getSessionKey(), name);
			} catch (Exception exception) {
				log.error("Cannot remove attribute", exception);
			}
		}
	}

	protected void save() {
		if (this.id == null) return;

		try {
			Map<String, String> newMap = new HashMap<String, String>();

			newMap.put("session:creationTime", getSerializer().serialize(creationTime));
			newMap.put("session:lastAccessedTime", getSerializer().serialize(lastAccessedTime));
			newMap.put("session:thisAccessedTime", getSerializer().serialize(thisAccessedTime));
			newMap.put("session:maxInactiveInterval", getSerializer().serialize(maxInactiveInterval));
			newMap.put("session:isValid", getSerializer().serialize(isValid));
			newMap.put("session:isNew", getSerializer().serialize(isNew));
			newMap.put("session:authType", getSerializer().serialize(authType));
			newMap.put("session:principal", getSerializer().serialize(principal));

			for (Enumeration<String> e = super.getAttributeNames(); e.hasMoreElements();) {
				String key = e.nextElement();
				if(key == null) continue;
				Object o = super.getAttribute(key);
				newMap.put(key, getSerializer().serialize(o));
			}

			getManager().getSessionOperator()
				.hmset(getSessionKey(), newMap);

			if (getExpire() > 0) {
				getManager().getSessionOperator()
					.expire(getSessionKey(), getExpire());
			}
		} catch (Exception exception) {
			log.error("Cannot save", exception);
		}
	}

	protected void delete() {
		if (this.id == null) return;

		try {
			getManager().getSessionOperator()
				.del(getSessionKey());
		} catch (Exception exception) {
			log.error("Cannot set authType", exception);
		}
	}

	protected void load(Map<String, Object> attrs) {
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
		if(authType != null) {
			this.authType = authType;
		}

		GenericPrincipal principal = (GenericPrincipal) attrs.remove("session:principal");
		if(principal != null) {
			this.principal = principal;
		}

		for (Entry<String, Object> entry : attrs.entrySet()) {
			setAttribute(entry.getKey(), entry.getValue(), false);
		}
	}

	private int getExpire() {
		int expire = getManager().getContextInternal().getSessionTimeout() * 60;
		return expire > 0 ? expire : 0;
	}

	protected String getSessionKey() {
		return RedisClusterSessionManager.buildSessionKey(this);
	}

	@Override
	public void setAuthType(String authType) {
		if(this.authType != null && this.authType.equals(authType)) return;
		super.setAuthType(authType);

		if (this.id != null) {
			try {
				getManager().getSessionOperator()
					.hset(getSessionKey(), "session:authType", getSerializer().serialize(authType));
			} catch (Exception exception) {
				log.error("Cannot set authType", exception);
			}
		}
	}

	@Override
	public void setPrincipal(Principal principal) {
		if(this.principal != null && this.principal.equals(principal)) return;
		super.setPrincipal(principal);

		if (this.id != null) {
			try {
				getManager().getSessionOperator()
					.hset(getSessionKey(), "session:principal", getSerializer().serialize(principal));
			} catch (Exception exception) {
				log.error("Cannot set principal", exception);
			}
		}
	}
}
