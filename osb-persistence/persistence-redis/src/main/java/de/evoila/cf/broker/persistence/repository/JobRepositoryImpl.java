/**
 * 
 */
package de.evoila.cf.broker.persistence.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import de.evoila.cf.broker.model.JobProgress;
import de.evoila.cf.broker.repository.JobRepository;

/**
 * @author Christian Brinker, evoila.
 *
 */
@Repository
public class JobRepositoryImpl extends CrudRepositoryImpl<JobProgress, String> implements JobRepository {
	
	@Autowired
	@Qualifier("jacksonJobProgressRedisTemplate")
	private RedisTemplate<String, JobProgress> redisTemplate;
	
	@Override
	protected RedisTemplate<String, JobProgress> getRedisTemplate() {
		return this.redisTemplate;
	}
	
	private static final String PREFIX = "job-progress-";

	@Override
	protected String getPrefix() {
		return PREFIX;
	}

	/* (non-Javadoc)
	 * @see de.evoila.cf.broker.persistence.repository.JobRepository#getJobProgress(java.lang.String)
	 */
	@Override
	public JobProgress getJobProgress(String serviceInstanceId) {
		return findOne(serviceInstanceId);
	}

	/* (non-Javadoc)
	 * @see de.evoila.cf.broker.persistence.repository.JobRepository#saveOrUpdateJobProgress(java.lang.String, java.lang.String)
	 */
	@Override
	public void saveOrUpdateJobProgress(String serviceInstanceId, String progress) {
		JobProgress jobProgress = new JobProgress(serviceInstanceId, progress);
		save(jobProgress);
	}

	/* (non-Javadoc)
	 * @see de.evoila.cf.broker.persistence.repository.JobRepository#containsJobProgress(java.lang.String)
	 */
	@Override
	public boolean containsJobProgress(String serviceInstanceId) {
		return exists(serviceInstanceId);
	}

	/* (non-Javadoc)
	 * @see de.evoila.cf.broker.persistence.repository.JobRepository#deleteJobProgress(java.lang.String)
	 */
	@Override
	public void deleteJobProgress(String serviceInstanceId) {
		this.delete(serviceInstanceId);
	}

}
