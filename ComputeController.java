package com.oscoe.cloud.compute.controller;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oscoe.cloud.ComputeServiceException;
import com.oscoe.cloud.compute.LogExecutionTime;
import com.oscoe.cloud.compute.service.ComputeService;
import com.oscoe.cloud.model.ComputeResourceSearchResponse;
import com.oscoe.cloud.model.CreateInstanceResponse;
import com.oscoe.cloud.model.InstanceTemplate;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api/v1/compute")
@Api("Compute Service")
public class ComputeController {

	@Autowired
	ComputeService computeService;

	Logger logger = LoggerFactory.getLogger(ComputeController.class);

	@ApiOperation("Get all instances for all regions in all cloud providers supported by MCM platform")
	@GetMapping("/instances")
	@LogExecutionTime
	public List<ComputeResourceSearchResponse> getAllInstances() throws ComputeServiceException {
		logger.info("{} : Request received for all cloud instances", "getAllInstances()");
		return computeService.getInstancesOnCriteria(Collections.<String>emptyList());
	}

	@ApiOperation("Get all instances for particular cloud providers in all region")
	@GetMapping("/instances/providers/{cloudProviders}")
	@LogExecutionTime
	public List<ComputeResourceSearchResponse> getInstancesForProvider(
			@PathVariable(name = "cloudProviders", required = true) List<String> cloudProviders) throws ComputeServiceException {
		logger.info("{} : Request received for all instances for a specific cloud providers : {}", "getInstancesForProvider()", cloudProviders);
		return computeService.getInstancesOnCriteria(cloudProviders, Collections.<String>emptyList());
	}

	@ApiOperation("Get all instances for all cloud providers in a specific region")
	@GetMapping("/instances/regions/{regions}")
	@LogExecutionTime
	public List<ComputeResourceSearchResponse> getInstancesForRegion(@PathVariable(name = "regions", required = true) List<String> regions)
			throws ComputeServiceException {
		logger.info("{} : Request received for all instances for cloud providers in a specific region : {}", "getInstancesForRegion()", regions);
		return computeService.getInstancesOnCriteria(regions);
	}

	@ApiOperation("Get all instances for a particular cloud provider in a specific region")
	@GetMapping("/instances/{cloudProviders}/{regions}")
	@LogExecutionTime
	public List<ComputeResourceSearchResponse> getInstancesOfProviderForRegion(
			@PathVariable(name = "cloudProviders", required = true) List<String> cloudProviders,
			@PathVariable(name = "regions", required = true) List<String> regions) throws ComputeServiceException {
		logger.info("{} : Request received for all instances for specific cloud providers in a specific region : {} : {}",
				"getInstancesOfProviderForRegion()", cloudProviders, regions);
		return computeService.getInstancesOnCriteria(cloudProviders, regions);
	}

	@ApiOperation("Create new instances as per the template")
	@PostMapping("/instances")
	@LogExecutionTime
	public List<CreateInstanceResponse> createInstance(@RequestBody List<InstanceTemplate> templates) throws ComputeServiceException {
		logger.info("{} : Request received for creating new instances","createInstance()");
		return computeService.createInstance(templates);
	}
}
