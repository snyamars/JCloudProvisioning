package com.oscoe.cloud.gcp;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.jclouds.compute.ComputeService;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.predicates.NodePredicates;
import org.jclouds.googlecomputeengine.compute.options.GoogleComputeEngineTemplateOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oscoe.cloud.ComputeInterface;
import com.oscoe.cloud.ComputeServiceException;
import com.oscoe.cloud.dto.CloudSearchCriteriaDto;
import com.oscoe.cloud.model.ComputeResourceSearchResponse;
import com.oscoe.cloud.model.CreateInstanceResponse;
import com.oscoe.cloud.model.InstanceMetadata;
import com.oscoe.cloud.model.InstanceTemplate;
import com.oscoe.cloud.gcp.utils.GCPAdapterUtil;

@Component("GCP")
public class GCPComputeService implements ComputeInterface {

	Logger logger = LoggerFactory.getLogger(GCPComputeService.class);

	@Autowired
	GCPAdapterUtil util;

	@Override
	public ComputeResourceSearchResponse getNodes(CloudSearchCriteriaDto searchCriteria) throws ComputeServiceException {
		logger.info("GCP cloud instance discovery begin for search critera : {}", searchCriteria.toString());
		try {
			ComputeService compute = util.initComputeService();
			ComputeResourceSearchResponse resource = new ComputeResourceSearchResponse("GCP", new ArrayList<InstanceMetadata>());

			if (!searchCriteria.getRegions().isEmpty()) {
				resource.getInstances().addAll(getNodesByLocation(compute, searchCriteria));
			} else {
				for (ComputeMetadata node : compute.listNodes()) {
					resource.getInstances().add(generateInstanceMetadata(compute.getNodeMetadata(node.getId())));
				}
			}
			logger.info("Discovered nodes count for GCP : " + resource.getInstances().size());
			return resource;
		} catch (Exception e) {
			logger.error("{} : Exception while fetching the instance metadata for GCP", "getNodes()", e);
			throw new ComputeServiceException("Could not get the instance metadata for GCP cloud!!");
		}
	}

	private List<InstanceMetadata> getNodesByLocation(ComputeService compute, CloudSearchCriteriaDto searchCriteria) {
		List<InstanceMetadata> instances = new ArrayList<>();
		for (String locationId : searchCriteria.getRegions()) {
			for (ComputeMetadata node : compute.listNodesDetailsMatching(NodePredicates.locationId(locationId))) {
				instances.add(generateInstanceMetadata(compute.getNodeMetadata(node.getId())));
			}
		}
		return instances;
	}

	@Override
	public CreateInstanceResponse createInstance(InstanceTemplate template) throws ComputeServiceException {
		logger.info("GCP instance creation begin for the template : {}", template.toString());
		try {
			ComputeService compute = util.initComputeService();
			CreateInstanceResponse response = new CreateInstanceResponse(template, new ArrayList<>());
			Hardware hardware = getHardware(compute, template.getHardwareId());
			Image image = getImage(compute, template.getImageId());
			Template inputTemplate = compute.templateBuilder().fromImage(image).os64Bit(template.isOs64Bit()).locationId(template.getLocationId())
					.fromHardware(hardware).build();

			inputTemplate.getOptions().as(GoogleComputeEngineTemplateOptions.class).authorizePublicKey(template.getKeyPair());

			Set<? extends NodeMetadata> nodes = compute.createNodesInGroup(template.getGroupName(), template.getInstanceCount(), inputTemplate);
			for (NodeMetadata node : nodes) {
				response.getInstances().add(generateInstanceMetadata(node));
			}
			response.setCount(nodes.size());
			logger.info("New instances created in GCP : ", nodes.size());
			if (template.getInstanceCount() == nodes.size()) {
				response.setStatus("Success");
			} else if (nodes.size() > 0 && template.getInstanceCount() > nodes.size()) {
				response.setStatus("Partial Success");
				logger.info("Partial success in GCP instance creation!!");
			} else {
				response.setStatus("Failure");
				logger.info("Failure in GCP instance creation!!");
			}
			return response;
		} catch (RunNodesException e) {
			logger.error("{} : Exception while creating new instance in GCP", "createInstance()", e);
			throw new ComputeServiceException("Instance creation failed in GCP");
		} catch (NoSuchElementException e) {
			logger.error("{} : {}", "createInstance()", e.getMessage(), e);
			throw new ComputeServiceException(e.getMessage());
		} catch (Exception e) {
			logger.error("{} : Exception while creating new instance in GCP", "createInstance()", e);
			throw new ComputeServiceException("Instance creation failed in GCP due to an internal server error!!");
		}
	}

	private InstanceMetadata generateInstanceMetadata(NodeMetadata metadata) {
		logger.info("GCP instance metadata from cloud : ", util.toJson(metadata));
		InstanceMetadata instance = new InstanceMetadata();

		instance.setId(metadata.getId());
		instance.setName(metadata.getName());
		if (null != metadata.getLocation()) {
			instance.setRegion(metadata.getLocation().getParent().getId());
		}
		instance.setZone(metadata.getLocation().getId());
		instance.setType(metadata.getType().name());
		instance.setProviderId(metadata.getProviderId());

		if (null != metadata.getOperatingSystem()) {
			instance.setOsType(metadata.getOperatingSystem().getFamily().name());
			instance.setOsDescription(metadata.getOperatingSystem().getDescription());
			instance.setOs64bit(metadata.getOperatingSystem().is64Bit());
		}

		instance.setBackendStatus(metadata.getBackendStatus());
		instance.setPrivateAddresses(metadata.getPrivateAddresses().toString());
		instance.setPublicAddresses(metadata.getPublicAddresses().toString());
		instance.setImageId(metadata.getImageId());
		if (null != metadata.getHardware()) {
			instance.setHardwareType(metadata.getHardware().getType().name());
		}
		instance.setStatus(metadata.getStatus().name());

		return instance;
	}

	private Hardware getHardware(ComputeService compute, String hardwareName) throws NoSuchElementException {
		Set<? extends Hardware> profiles = compute.listHardwareProfiles();
		for (Hardware profile : profiles) {
			if (profile.getName().equals(hardwareName)) {
				return profile;
			}
		}
		throw new NoSuchElementException("Hardware name in the request is not a valid one for GCP : " + hardwareName);
	}

	private Image getImage(ComputeService compute, String imageName) throws NoSuchElementException {
		Set<? extends Image> images = compute.listImages();
		for (Image image : images) {
			if (image.getName().equals(imageName)) {
				return image;
			}
		}
		throw new NoSuchElementException("OS image in the request is not a valid one for GCP : " + imageName);
	}
}
