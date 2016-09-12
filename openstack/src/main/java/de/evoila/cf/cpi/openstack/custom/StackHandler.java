/**
 * 
 */
package de.evoila.cf.cpi.openstack.custom;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.openstack4j.model.heat.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.cpi.openstack.fluent.HeatFluent;
import de.evoila.cf.cpi.openstack.util.StackProgressObserver;

/**
 * @author Christian Mueller, evoila
 *
 */
@Service(value = "defaultStackHandler")
public class StackHandler {
	/**
	 * 
	 */
	private static final String TEMPLATE = "template";

	public static final boolean DEFAULT_DISABLE_ROLLBACK = false;

	public static final long DEFAULT_TIMEOUT_MINUTES = 10;

	private static String DEFAULT_ENCODING = "UTF-8";

	public static final String IMAGE_ID = "image_id";
	public static final String KEYPAIR = "keypair";
	public static final String NETWORK_ID = "network_id";
	public static final String AVAILABILITY_ZONE = "availability_zone";
	public static final String LOG_PORT = "log_port";
	public static final String LOG_HOST = "log_host";

	private Logger log = LoggerFactory.getLogger(getClass());

	private String defaultHeatTemplate;

	@Value("${openstack.networkId}")
	private String networkId;

	@Value("${openstack.imageId}")
	private String imageId;

	@Value("${openstack.keypair}")
	private String keypair;

	@Value("${openstack.cinder.az}")
	private String availabilityZone;

	@Autowired
	protected HeatFluent heatFluent;

	@Autowired
	protected StackProgressObserver stackProgressObserver;

	@Autowired
	private ApplicationContext appContext;

	@PostConstruct
	public void initialize() {
		final String templatePath = "classpath:openstack/template.yml";
		defaultHeatTemplate = accessTemplate(templatePath);
	}

	protected String accessTemplate(final String templatePath) {

		try {
			InputStream inputStream = appContext.getResource(templatePath).getInputStream();
			return this.readTemplateFile(inputStream);
		} catch (IOException | URISyntaxException e) {
			log.info("Failed to load heat template", e);
			return defaultHeatTemplate;
		}
	}

	private String readTemplateFile(InputStream inputStream) throws IOException, URISyntaxException {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int length;
		while ((length = inputStream.read(buffer)) != -1) {
			result.write(buffer, 0, length);
		}
		return result.toString(DEFAULT_ENCODING);
	}

	public String create(String instanceId, Map<String, String> customParameters)
			throws PlatformException, InterruptedException {
		Map<String, String> completeParameters = new HashMap<String, String>();
		completeParameters.putAll(defaultParameters());
		completeParameters.putAll(customParameters);

		String heatTemplate;
		if (customParameters.containsKey(TEMPLATE)) {
			heatTemplate = accessTemplate(customParameters.get(TEMPLATE));
			completeParameters.remove(TEMPLATE);
		} else {
			heatTemplate = getDefaultHeatTemplate();
		}

		String name = HeatFluent.uniqueName(instanceId);

		heatFluent.create(name, heatTemplate, completeParameters, DEFAULT_DISABLE_ROLLBACK, DEFAULT_TIMEOUT_MINUTES);

		stackProgressObserver.waitForStackCompletion(name);

		return name;
	}

	public void delete(String internalId) {
		Stack stack = heatFluent.get(internalId);

		heatFluent.delete(stack.getName(), stack.getId());
	}

	protected Map<String, String> defaultParameters() {
		Map<String, String> defaultParameters = new HashMap<String, String>();
		defaultParameters.put(IMAGE_ID, imageId);
		defaultParameters.put(KEYPAIR, keypair);
		defaultParameters.put(NETWORK_ID, networkId);
		defaultParameters.put(AVAILABILITY_ZONE, availabilityZone);

		return defaultParameters;
	}

	public String getDefaultHeatTemplate() {
		return defaultHeatTemplate;
	}
}
