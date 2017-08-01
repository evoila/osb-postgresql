package de.evoila.cf.broker.persistence.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import de.evoila.cf.broker.model.JobProgress;
import de.evoila.cf.broker.model.RouteBinding;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.ServiceInstanceBinding;
import redis.clients.jedis.Protocol;

/**
 * @author Christian Brinker, evoila.
 *
 */
@Configuration
public class RedisContextConfiguration {

	Logger log = LoggerFactory.getLogger(RedisContextConfiguration.class);

	@Value("${redis.host:'http://localhost'}")
	private String hostname;

	@Value("${redis.port:6379}")
	private int port;

	@Value("${redis.password:''}")
	private String password;

	@Value("${redis.database:0}")
	private int database;

	@Bean
	public RedisConnectionFactory jedisConnFactory() {
		log.info("Trying to connect to redis instance " + hostname + ":" + port + "/" + "database");

		JedisConnectionFactory jedisConnFactory = new JedisConnectionFactory();

		jedisConnFactory.setUsePool(true);
		jedisConnFactory.setHostName(hostname);
		jedisConnFactory.setDatabase(database);
		jedisConnFactory.setPort(port);
		if (password != null && password.length() > 0)
			jedisConnFactory.setPassword(password);
		jedisConnFactory.setTimeout(Protocol.DEFAULT_TIMEOUT);

		log.info("Connection to redis instance successfull");

		return jedisConnFactory;
	}

	/**
	 * @return
	 */
	@Bean
	public RedisTemplate<String, ? extends Object> jacksonServiceInstanceRedisTemplate() {
		RedisTemplate<String, ? extends Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(jedisConnFactory());
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(ServiceInstance.class));
		return redisTemplate;
	}

	/**
	 * @return
	 */
	@Bean
	public RedisTemplate<String, ? extends Object> jacksonServiceInstanceBindingRedisTemplate() {
		RedisTemplate<String, ? extends Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(jedisConnFactory());
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(ServiceInstanceBinding.class));
		return redisTemplate;
	}

	/**
	 * @return
	 */
	@Bean
	public RedisTemplate<String, ? extends Object> jacksonJobProgressRedisTemplate() {
		RedisTemplate<String, ? extends Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(jedisConnFactory());
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(JobProgress.class));
		return redisTemplate;
	}

	/**
	 * @return
	 */
	@Bean
	public RedisTemplate<String, ? extends Object> jacksonRouteBindingRedisTemplate() {
		RedisTemplate<String, ? extends Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(jedisConnFactory());
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(RouteBinding.class));
		return redisTemplate;
	}

	/**
	 * Template
	 * 
	 * @param jedisConnFactory
	 * @param stringRedisSerializer
	 * @param jacksonJsonRedisJsonSerializer
	 */
	@Bean
	@Autowired
	public RedisTemplate<String, String> stringRedisTemplate() {
		RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(jedisConnFactory());
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new StringRedisSerializer());
		return redisTemplate;
	}

}