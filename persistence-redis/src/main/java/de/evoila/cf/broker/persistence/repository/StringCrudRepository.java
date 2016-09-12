/**
 * 
 */
package de.evoila.cf.broker.persistence.repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author Christian Brinker, evoila.
 *
 * @param <T>
 *            Entity type
 * @param <ID>
 *            ID type
 */
public class StringCrudRepository {

	@Autowired
	@Qualifier("stringRedisTemplate")
	private RedisTemplate<String, String> redisTemplate;

	public long count() {
		return findAllAsList().size();
	}

	public void delete(String id) {
		String key = generateKey(id);
		redisTemplate.opsForValue().getOperations().delete(key);
	}

	private String generateKey(String id) {
		String key = getPrefix() + id.toString();
		return key;
	}

	protected String getPrefix() {
		return "";
	}

	public void delete(Iterable<? extends String> entities) {
		for (String entity : entities) {
			delete(entity);
		}
	}

	public void deleteAll() {
		delete(findAll());
	}

	public boolean exists(String entity) {
		return findOne(entity) != null;
	}

	public Iterable<String> findAll() {
		return findAllAsList();
	}

	private List<String> findAllAsList() {
		List<String> entities = new ArrayList<>();

		Set<String> keys = redisTemplate.keys(getPrefix() + "*");
		Iterator<String> it = keys.iterator();

		while (it.hasNext()) {
			entities.add(findOne(it.next()));
		}

		return entities;
	}

	public Iterable<String> findAll(Iterable<String> ids) {
		List<String> keys = new ArrayList<String>();
		for (String id : ids) {
			String key = generateKey(id);
			keys.add(key);
		}
		return redisTemplate.opsForValue().multiGet(keys);
	}

	public String findOne(String id) {
		String key = generateKey(id);
		return redisTemplate.opsForValue().get(key);
	}

	public String save(String id, String entity) {
		String key = generateKey(id);
		redisTemplate.opsForValue().set(key, entity);
		return entity;
	}

	public Iterable<Entry<String, String>> save(Iterable<Entry<String, String>> entities) {
		for (Entry<String, String> entity : entities) {
			save(entity.getKey(), entity.getValue());
		}
		return entities;
	}

}
