package org.apache.tomcat.session.redis;

import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

	private ExecutorService executor = Executors.newSingleThreadExecutor();

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
		super.setCreationTime(time);

		if (this.id != null) {
	        executor.submit(new RedisCommandCallable<Boolean>() {
	            @Override protected Boolean execute() throws Exception {
	    			try {
	    				Map<String, String> newMap = new HashMap<String, String>(3);
	    				newMap.put("session:creationTime", getSerializer().serialize(creationTime));
	    				newMap.put("session:lastAccessedTime", getSerializer().serialize(lastAccessedTime));
	    				newMap.put("session:thisAccessedTime", getSerializer().serialize(thisAccessedTime));
	    				getManager().getJedisCluster().hmset(getJedisSessionKey(), newMap);

	    				if (getExpire() > 0) {
	    					getManager().getJedisCluster().expire(getJedisSessionKey(), getExpire());
	    				}
	    			} catch (Exception exception) {
	    				log.error("Cannot set Creation Time", exception);
	    				return false;
	    			}
	    			return true;
	            }
	        });
		}
	}

	@Override
	public void access() {
		super.access();

		if (this.id != null) {
	        executor.submit(new RedisCommandCallable<Boolean>() {
	            @Override protected Boolean execute() throws Exception {
					try {
						getManager().getJedisCluster().hset(getJedisSessionKey(), "session:thisAccessedTime",
								getSerializer().serialize(thisAccessedTime));
		
						if (getExpire() > 0) {
							getManager().getJedisCluster().expire(getJedisSessionKey(), getExpire());
						}
					} catch (Exception exception) {
						log.error("Cannot update access", exception);
	    				return false;
					}
	    			return true;
	            }
	        });
		}
	}

	@Override
	public void setMaxInactiveInterval(int interval) {
		super.setMaxInactiveInterval(interval);

		if (this.id != null) {
	        executor.submit(new RedisCommandCallable<Boolean>() {
	            @Override protected Boolean execute() throws Exception {
					try {
						getManager().getJedisCluster().hset(getJedisSessionKey(), "session:maxInactiveInterval",
								getSerializer().serialize(maxInactiveInterval));
					} catch (Exception exception) {
						log.error("Cannot set Max Inactive Interval", exception);
	    				return false;
					}
	    			return true;
	            }
	        });
		}
	}

	@Override
	public void setValid(boolean isValid) {
		super.setValid(isValid);

		if (this.id != null) {
	        executor.submit(new RedisCommandCallable<Boolean>() {
	            @Override protected Boolean execute() throws Exception {
					try {
						getManager().getJedisCluster().hset(getJedisSessionKey(), "session:isValid",
								getSerializer().serialize(isValid));
					} catch (Exception exception) {
						log.error("Cannot set is valid", exception);
	    				return false;
					}
	    			return true;
	            }
	        });
		}
	}

	@Override
	public void setNew(boolean isNew) {
		super.setNew(isNew);

		if (this.id != null) {
	        executor.submit(new RedisCommandCallable<Boolean>() {
	            @Override protected Boolean execute() throws Exception {
					try {
						getManager().getJedisCluster().hset(getJedisSessionKey(), "session:isNew",
								getSerializer().serialize(isNew));
					} catch (Exception exception) {
						log.error("Cannot set is new", exception);
	    				return false;
					}
	    			return true;
	            }
	        });
		}
	}

	@Override
	public void endAccess() {
		super.endAccess();

		if (this.id != null) {
	        executor.submit(new RedisCommandCallable<Boolean>() {
	            @Override protected Boolean execute() throws Exception {
					try {
						Map<String, String> newMap = new HashMap<String, String>(3);
						newMap.put("session:lastAccessedTime", getSerializer().serialize(lastAccessedTime));
						newMap.put("session:thisAccessedTime", getSerializer().serialize(thisAccessedTime));
						newMap.put("session:isNew", getSerializer().serialize(isNew));
						getManager().getJedisCluster().hmset(getJedisSessionKey(), newMap);
						
						if (getExpire() > 0) {
							getManager().getJedisCluster().expire(getJedisSessionKey(), getExpire());
						}
					} catch (Exception exception) {
						log.error("Cannot set end access", exception);
	    				return false;
					}
	    			return true;
	            }
	        });
		}
	}

	@Override
	public void setAttribute(String name, Object value, boolean notify) {
		super.setAttribute(name, value, notify);

		if (this.id != null && name != null && value != null) {
	        executor.submit(new RedisCommandCallable<Boolean>() {
	            @Override protected Boolean execute() throws Exception {
					try {
						getManager().getJedisCluster().hset(getJedisSessionKey(), name,
								getSerializer().serialize(value));
					} catch (Exception exception) {
						log.error("Cannot set attribute", exception);
	    				return false;
					}
	    			return true;
	            }
	        });
		}
	}

	@Override
	protected void removeAttributeInternal(String name, boolean notify) {
		super.removeAttributeInternal(name, notify);

		if (this.id != null && name != null) {
	        executor.submit(new RedisCommandCallable<Boolean>() {
	            @Override protected Boolean execute() throws Exception {
				try {
					getManager().getJedisCluster().hdel(getJedisSessionKey(), name);
				} catch (Exception exception) {
					log.error("Cannot remove attribute", exception);
    				return false;
				}
    			return true;
	            }
	        });
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
			newMap.put("session:principal", getSerializer().serialize(principal == null ? null : SerializablePrincipal.createPrincipal((GenericPrincipal) principal)));

			for (Enumeration<String> e = getAttributeNames(); e.hasMoreElements();) {
				String key = e.nextElement();
				if(key == null) continue;
				Object o = super.getAttribute(key);
				newMap.put(key, getSerializer().serialize(o));
			}

	        executor.submit(new RedisCommandCallable<Boolean>() {
	            @Override protected Boolean execute() throws Exception {
					getManager().getJedisCluster().hmset(getJedisSessionKey(), newMap);
		
					if (getExpire() > 0) {
						getManager().getJedisCluster().expire(getJedisSessionKey(), getExpire());
					}

					return true;
	            }
	        });
		} catch (Exception exception) {
			log.error("Cannot save", exception);
		}
	}

	protected void delete() {
		if (this.id == null) return;

        executor.submit(new RedisCommandCallable<Boolean>() {
            @Override protected Boolean execute() throws Exception {
				try {
					getManager().getJedisCluster().del(getJedisSessionKey());
				} catch (Exception exception) {
					log.error("Cannot set authType", exception);
    				return false;
				}
    			return true;
            }
        });

        recycleExecutor();
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
		this.authType = authType;

		SerializablePrincipal principal = (SerializablePrincipal) attrs.remove("session:principal");
		this.principal = principal == null ? null : principal.getPrincipal();

		for (Entry<String, Object> entry : attrs.entrySet()) {
			setAttribute(entry.getKey(), entry.getValue(), false);
		}
	}

	private int getExpire() {
		int expire = ((Context) manager.getContainer()).getSessionTimeout() * 60;
		return expire > 0 ? expire : 0;
	}

	protected String getJedisSessionKey() {
		return RedisClusterSessionManager.buildSessionKey(this);
	}

	@Override
	public void setAuthType(String authType) {
		super.setAuthType(authType);

		if (this.id != null) {
	        executor.submit(new RedisCommandCallable<Boolean>() {
	            @Override protected Boolean execute() throws Exception {
					try {
						getManager().getJedisCluster().hset(getJedisSessionKey(), "session:authType",
								getSerializer().serialize(authType));
					} catch (Exception exception) {
						log.error("Cannot set authType", exception);
	    				return false;
					}
	    			return true;
	            }
	        });
		}
	}

	@Override
	public void setPrincipal(Principal principal) {
		super.setPrincipal(principal);

		if (this.id != null) {
	        executor.submit(new RedisCommandCallable<Boolean>() {
	            @Override protected Boolean execute() throws Exception {
					try {
						getManager().getJedisCluster().hset(getJedisSessionKey(), "session:principal",
								getSerializer().serialize(principal == null ? null : SerializablePrincipal.createPrincipal((GenericPrincipal) principal)));
					} catch (Exception exception) {
						log.error("Cannot set principal", exception);
	    				return false;
					}
	    			return true;
	            }
	        });
		}
	}

	private abstract class RedisCommandCallable<T> implements Callable<T> {
		@Override
		public T call() throws Exception {
			try {
				return execute();
			} catch (Exception e) {
				log.error("Cannot RedisCommandCallable", e);
			}
			return null;
		}

		protected abstract T execute() throws Exception;
	}

	@Override
	public void recycle() {
		super.recycle();
		recycleExecutor();
	}

	private void recycleExecutor() {
		if(executor != null && !executor.isShutdown()) {
			executor.shutdownNow();
		}
		executor = Executors.newSingleThreadExecutor();
	}
}
