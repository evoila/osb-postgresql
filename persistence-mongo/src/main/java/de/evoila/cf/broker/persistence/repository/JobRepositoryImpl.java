/**
 * 
 */
package de.evoila.cf.broker.persistence.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import de.evoila.cf.broker.model.JobProgress;
import de.evoila.cf.broker.repository.JobRepository;

/**
 * @author Patrick Weber, evoila.
 *
 */
@Service
public class JobRepositoryImpl  implements JobRepository {
	
	@Autowired
	MongoRepository<JobProgress, String> jobRepository;
	
	private static final String PREFIX = "job-progress-";

	
	protected String getPrefix() {
		return PREFIX;
	}

	/* (non-Javadoc)
	 * @see de.evoila.cf.broker.persistence.repository.JobRepository#getJobProgress(java.lang.String)
	 */
	@Override
	public JobProgress getJobProgress(String serviceInstanceId) {
		return jobRepository.findOne(serviceInstanceId);
	}

	/* (non-Javadoc)
	 * @see de.evoila.cf.broker.persistence.repository.JobRepository#saveOrUpdateJobProgress(java.lang.String, java.lang.String)
	 */
	@Override
	public void saveOrUpdateJobProgress(String serviceInstanceId, String progress) {
		JobProgress jobProgress = new JobProgress(serviceInstanceId, progress);
		jobRepository.save(jobProgress);
	}

	/* (non-Javadoc)
	 * @see de.evoila.cf.broker.persistence.repository.JobRepository#containsJobProgress(java.lang.String)
	 */
	@Override
	public boolean containsJobProgress(String serviceInstanceId) {
		return jobRepository.exists(serviceInstanceId);
	}

	/* (non-Javadoc)
	 * @see de.evoila.cf.broker.persistence.repository.JobRepository#deleteJobProgress(java.lang.String)
	 */
	@Override
	public void deleteJobProgress(String serviceInstanceId) {
		jobRepository.delete(serviceInstanceId);
	}

}
