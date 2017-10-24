package org.apache.tomcat.session.redis;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.catalina.Context;
import org.apache.catalina.session.StandardSession;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * 
 * @author xfacq
 *
 */
public class RedisClusterSession extends StandardSession {

	private static final long serialVersionUID = -2518607181636076487L;
	private static final Log log = LogFactory.getLog(RedisClusterSession.class);

	private RedisClusterSessionManager manager;
	private Map<String, Object> attributes;

	@SuppressWarnings("unchecked")
	public RedisClusterSession(RedisClusterSessionManager manager) {
        super(manager);
        this.manager = manager;
        try {
            Field attr = StandardSession.class.getDeclaredField("attributes");
            attributes = (Map<String, Object>) attr.get(this);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }        
    }

    @Override
    public Object getAttribute(String name) {
    	try {
			return manager.fromString(manager.getJedisCluster().hget(getJedisSessionKey(), name));
		} catch (Exception exception) {
			log.error("Cannot get attribute", exception);
			return null;
		}
    }

    @Override
    public void setId(String id, boolean notify) {
        super.setId(id, notify);
        manager.getJedisCluster().hset(getJedisSessionKey(), "session:id", id);
    }

    public void delete() {
    	manager.getJedisCluster().del(getJedisSessionKey());
    	attributes = null;
    }

    @Override
    public void setCreationTime(long time) {
        super.setCreationTime(time);

        if(this.id != null) {
	        try {
	        	Map<String, String> newMap = new HashMap<String, String>(3);
	            newMap.put("session:creationTime", RedisClusterSessionManager.toString(creationTime));
	            newMap.put("session:lastAccessedTime", RedisClusterSessionManager.toString(lastAccessedTime));
	            newMap.put("session:thisAccessedTime", RedisClusterSessionManager.toString(thisAccessedTime));
	            manager.getJedisCluster().hmset(getJedisSessionKey(), newMap);
			} catch (Exception exception) {
				log.error("Cannot set Creation Time", exception);
			}
        }
    }

    @Override
    public void access() {
        super.access();

        if(this.id != null) {
	        try {
		    	Map<String, String> newMap = new HashMap<String, String>(3);
		        newMap.put("session:lastAccessedTime", RedisClusterSessionManager.toString(lastAccessedTime));
		        newMap.put("session:thisAccessedTime", RedisClusterSessionManager.toString(thisAccessedTime));
		        manager.getJedisCluster().hmset(getJedisSessionKey(), newMap);
		
		        if (getExpire() >= 0) {
		        	manager.getJedisCluster().expire(getJedisSessionKey(), getExpire());
		        }
			} catch (Exception exception) {
				log.error("Cannot update access", exception);
			}
        }
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        super.setMaxInactiveInterval(interval);
        
        if(this.id != null) {
	        try {
		        manager.getJedisCluster().hset(getJedisSessionKey(), "session:maxInactiveInterval", RedisClusterSessionManager.toString(maxInactiveInterval));
		
		        if (getExpire() >= 0) {
		        	manager.getJedisCluster().expire(getJedisSessionKey(), getExpire());
		        }
			} catch (Exception exception) {
				log.error("Cannot set Max Inactive Interval", exception);
			}
        }
    }

    @Override
    public void setValid(boolean isValid) {
        super.setValid(isValid);

        if(this.id != null) {
	        if (!isValid) {
	            return;
	        }
	
	        try {
	        	manager.getJedisCluster().hset(getJedisSessionKey(), "session:isValid", RedisClusterSessionManager.toString(isValid));
			} catch (Exception exception) {
				log.error("Cannot set is valid", exception);
			}
        }
    }

    @Override
    public void setNew(boolean isNew) {
        super.setNew(isNew);

        if(this.id != null) {
	        try {
	        	manager.getJedisCluster().hset(getJedisSessionKey(), "session:isNew", RedisClusterSessionManager.toString(isNew));
			} catch (Exception exception) {
				log.error("Cannot set is new", exception);
			}
        }
    }

    @Override
    public void endAccess() {
        boolean oldValue = isNew;

        super.endAccess();

        if (isNew != oldValue) {
            try {
            	manager.getJedisCluster().hset(getJedisSessionKey(), "session:isNew", RedisClusterSessionManager.toString(isNew));
    		} catch (Exception exception) {
				log.error("Cannot set end access", exception);
    		}
        }
    }

    @Override
    public void setAttribute(String name, Object value, boolean notify) {
        super.setAttribute(name, value, notify);

        if(this.id != null && value != null) {
	        try {
	        	manager.getJedisCluster().hset(getJedisSessionKey(), name, RedisClusterSessionManager.toString(value));
			} catch (Exception exception) {
				log.error("Cannot set attribute", exception);
			}
        }
    }

    @Override
    protected void removeAttributeInternal(String name, boolean notify) {
        super.removeAttributeInternal(name, notify);

        if(this.id != null) {
	        try {
	        	manager.getJedisCluster().hdel(getJedisSessionKey(), name);
			} catch (Exception exception) {
				log.error("Cannot remove attribute", exception);
			}
        }
    }

    public void save() {
    	try {
	    	 Map<String, String> newMap = new HashMap<String, String>();
	         newMap.put("session:creationTime", RedisClusterSessionManager.toString(creationTime));
	         newMap.put("session:lastAccessedTime", RedisClusterSessionManager.toString(lastAccessedTime));
	         newMap.put("session:thisAccessedTime", RedisClusterSessionManager.toString(thisAccessedTime));
	         newMap.put("session:maxInactiveInterval", RedisClusterSessionManager.toString(maxInactiveInterval));
	         newMap.put("session:isValid", RedisClusterSessionManager.toString(isValid));
	         newMap.put("session:isNew", RedisClusterSessionManager.toString(isNew));
	         
	         for (Entry<String, Object> entry : attributes.entrySet()) {
	             newMap.put(entry.getKey(), RedisClusterSessionManager.toString(entry.getValue()));
	         }

         	manager.getJedisCluster().hmset(getJedisSessionKey(), newMap);

	        if (getExpire() >= 0) {
	        	manager.getJedisCluster().expire(getJedisSessionKey(), getExpire());
	        }
         } catch (Exception exception) {
				log.error("Cannot save", exception);
 		}
    }

    public void load(Map<String, Object> attrs) {
    	if(attrs == null) return;
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
        Boolean isValid = (Boolean) attrs.remove("session:isValid");
        if (isValid != null) {
            this.isValid = isValid;
        }
        Boolean isNew = (Boolean) attrs.remove("session:isNew");
        if (isNew != null) {
            this.isNew = isNew;
        }

        for (Entry<String, Object> entry : attrs.entrySet()) {
            setAttribute(entry.getKey(), entry.getValue(), false);
        }
    }

	private int getExpire() {
		return ((Context) manager.getContainer()).getSessionTimeout() * 60;
	}

    private String getJedisSessionKey() {
    	return RedisClusterSessionManager.buildSessionKey(this);
    }
}
