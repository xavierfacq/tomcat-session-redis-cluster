package org.apache.tomcat.session.redis;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import org.apache.catalina.util.CustomObjectInputStream;

public class RedisClusterSessionSerializer {

	private ClassLoader classLoader;
	private static final String REDIS_NULL_VALUE = "__null__";

	public RedisClusterSessionSerializer(ClassLoader classLoader) {
		super();
		this.classLoader = classLoader;
	}

	protected Object deserialize(String s) throws IOException, ClassNotFoundException {
		Object o = null;
		if (s != null) {
			byte[] data = Base64.getDecoder().decode(s);
			BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(data));
			ObjectInputStream ois = new CustomObjectInputStream(bis, classLoader);
			o = ois.readObject();
			ois.close();

			if (o != null && (o instanceof String) && ((String) o).equals(REDIS_NULL_VALUE)) {
				o = null;
			}
		}
		return o;
	}

	protected String serialize(Object o) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(o == null ? REDIS_NULL_VALUE : o);
		oos.flush();
		oos.close();
		return new String(Base64.getEncoder().encode(baos.toByteArray()));
	}
}
