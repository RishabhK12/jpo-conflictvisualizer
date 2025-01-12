package us.dot.its.jpo.ode.api.controllers;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.web.bind.annotation.RequestBody;

import us.dot.its.jpo.conflictmonitor.monitor.models.config.Config;
import us.dot.its.jpo.conflictmonitor.monitor.models.config.DefaultConfig;
import us.dot.its.jpo.conflictmonitor.monitor.models.config.DefaultConfigMap;
import us.dot.its.jpo.conflictmonitor.monitor.models.config.IntersectionConfig;
import us.dot.its.jpo.conflictmonitor.monitor.models.config.IntersectionConfigMap;
import us.dot.its.jpo.ode.api.ConflictMonitorApiProperties;
import us.dot.its.jpo.ode.api.accessors.config.DefaultConfig.DefaultConfigRepository;
import us.dot.its.jpo.ode.api.accessors.config.IntersectionConfig.IntersectionConfigRepository;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.MediaType;

@RestController
public class ConfigController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class);

    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    DefaultConfigRepository defaultConfigRepository;

    @Autowired
    IntersectionConfigRepository intersectionConfigRepository;

    @Autowired
    KafkaTemplate<String, DefaultConfig> defaultConfigProducer;

    @Autowired
    ConflictMonitorApiProperties props;

    private RestTemplate restTemplate = new RestTemplate();

    private final String defaultConfigTemplate = "%s/config/default/%s";
    private final String intersectionConfigTemplate = "%s/config/intersection/%s/%s/%s";
    private final String defaultConfigAllTemplate = "%s/config/defaults";
    private final String intersectionConfigAllTemplate = "%s/config/intersections";
    


    // General Setter for Default Configs
    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping(value = "/config/default")
    @PreAuthorize("hasRole('ADMIN')")
    public @ResponseBody ResponseEntity<String> default_config(@RequestBody DefaultConfig config) {
        try {
            
            String resourceURL = String.format(defaultConfigTemplate, props.getCmServerURL(), config.getKey());
            ResponseEntity<DefaultConfig> response = restTemplate.getForEntity(resourceURL, DefaultConfig.class);
            
            
            if(response.getStatusCode().is2xxSuccessful()){
                DefaultConfig previousConfig = response.getBody();
                previousConfig.setValue(config.getValue());
                restTemplate.postForEntity(resourceURL, previousConfig, DefaultConfig.class);
                defaultConfigRepository.save(previousConfig);
            }else{
                return ResponseEntity.status(response.getStatusCode()).contentType(MediaType.TEXT_PLAIN).body("Conflict Monitor API was unable to change setting on conflict monitor.");
            }

            return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.TEXT_PLAIN).body(config.toString());
        } catch (Exception e) {
            logger.error("Failure in Default Config" + e.getStackTrace());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_PLAIN)
                    .body(ExceptionUtils.getStackTrace(e));
        }
    }

    // General Setter for Intersection Configs
    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping(value = "/config/intersection")
    @PreAuthorize("hasRole('ADMIN')")
    public @ResponseBody ResponseEntity<String> intersection_config(@RequestBody IntersectionConfig config) {
        try {
            String resourceURL = String.format(intersectionConfigTemplate, props.getCmServerURL(),config.getRoadRegulatorID(),config.getIntersectionID(), config.getKey());
            ResponseEntity<IntersectionConfig> response = restTemplate.getForEntity(resourceURL, IntersectionConfig.class);
            
            if(response.getStatusCode().is2xxSuccessful()){
                IntersectionConfig previousConfig = response.getBody();
                System.out.println(previousConfig);
                if(previousConfig == null){
                    previousConfig = config;
                }
                previousConfig.setValue(config.getValue());
                restTemplate.postForEntity(resourceURL, previousConfig, DefaultConfig.class);
                System.out.println("PostBack Complete");

                intersectionConfigRepository.save(previousConfig);
                System.out.println("Database Postback Complete");
            }else{
                return ResponseEntity.status(response.getStatusCode()).contentType(MediaType.TEXT_PLAIN).body("Conflict Monitor API was unable to change setting on conflict monitor.");
            }

            return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.TEXT_PLAIN).body(config.toString());
        } catch (Exception e) {
            logger.error("Failure in Intersection Config" + e.getStackTrace());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_PLAIN)
                    .body(ExceptionUtils.getStackTrace(e));
        }
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @DeleteMapping(value = "/config/intersection")
    @PreAuthorize("hasRole('ADMIN')")
    public @ResponseBody ResponseEntity<String> intersection_config_delete(@RequestBody IntersectionConfig config) {
        Query query = intersectionConfigRepository.getQuery(config.getKey(), config.getRoadRegulatorID(),
                config.getIntersectionID());
        try {
            String resourceURL = String.format(intersectionConfigTemplate, props.getCmServerURL(),config.getRoadRegulatorID(),config.getIntersectionID(), config.getKey());
            restTemplate.delete(resourceURL);
            intersectionConfigRepository.delete(query);
            return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.TEXT_PLAIN).body(config.toString());
        } catch (Exception e) {
            System.out.println("Received exception when deleting config");
            System.out.println(ExceptionUtils.getStackTrace(e));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_PLAIN)
                    .body(ExceptionUtils.getStackTrace(e));
        }
    }

    // Retrieve All Config Params for Intersection Configs
    @CrossOrigin(origins = "http://localhost:3000")
    @RequestMapping(value = "/config/default/all", method = RequestMethod.GET, produces = "application/json")
    @PreAuthorize("hasRole('USER') || hasRole('ADMIN')")
    public @ResponseBody ResponseEntity<List<DefaultConfig>> default_config_all() {
        
        String resourceURL = String.format(defaultConfigAllTemplate, props.getCmServerURL());
        ResponseEntity<DefaultConfigMap> response = restTemplate.getForEntity(resourceURL, DefaultConfigMap.class);

        if(response.getStatusCode().is2xxSuccessful()){
            DefaultConfigMap configMap = response.getBody();
            ArrayList<DefaultConfig> results = new ArrayList<>(configMap.values());
            return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(results);
        }else{
            return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                    .body(new ArrayList<DefaultConfig>());
        }
    }

    // Retrieve All Parameters for Unique Intersections
    @CrossOrigin(origins = "http://localhost:3000")
    @RequestMapping(value = "/config/intersection/all", method = RequestMethod.GET, produces = "application/json")
    @PreAuthorize("hasRole('USER') || hasRole('ADMIN')")
    public @ResponseBody ResponseEntity<List<IntersectionConfig>> intersection_config_all() {
        

        String resourceURL = String.format(intersectionConfigAllTemplate, props.getCmServerURL());
        ResponseEntity<IntersectionConfigMap> response = restTemplate.getForEntity(resourceURL, IntersectionConfigMap.class);
        if(response.getStatusCode().is2xxSuccessful()){
            IntersectionConfigMap configMap = response.getBody();
            ArrayList<IntersectionConfig> results = new ArrayList<>(configMap.listConfigs());
            return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(results);
        }else{
            return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                    .body(new ArrayList<IntersectionConfig>());
        }
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @RequestMapping(value = "/config/intersection/unique", method = RequestMethod.GET, produces = "application/json")
    @PreAuthorize("hasRole('USER') || hasRole('ADMIN')")
    public @ResponseBody ResponseEntity<List<Config>> intersection_config_unique(
            @RequestParam(name = "road_regulator_id", required = true) int roadRegulatorID,
            @RequestParam(name = "intersection_id", required = true) int intersectionID) {

        // Query Default Configuration
        String defaultResourceURL = String.format(defaultConfigAllTemplate, props.getCmServerURL());
        List<DefaultConfig> defaultList = new ArrayList<>();
        ResponseEntity<DefaultConfigMap> defaultConfigResponse = restTemplate.getForEntity(defaultResourceURL, DefaultConfigMap.class);
        if(defaultConfigResponse.getStatusCode().is2xxSuccessful()){
            DefaultConfigMap configMap = defaultConfigResponse.getBody();
            defaultList = new ArrayList<>(configMap.values());
        }

        // Query Intersection Configuration
        List<IntersectionConfig> intersectionList = new ArrayList<>();
        String intersectionResourceURL = String.format(intersectionConfigAllTemplate, props.getCmServerURL());
        ResponseEntity<IntersectionConfigMap> intersectionConfigResponse = restTemplate.getForEntity(intersectionResourceURL, IntersectionConfigMap.class);
        if(intersectionConfigResponse.getStatusCode().is2xxSuccessful()){
            IntersectionConfigMap configMap = intersectionConfigResponse.getBody();
            ArrayList<IntersectionConfig> results = new ArrayList<>(configMap.listConfigs());

            for(IntersectionConfig config: results){
                if(config.getRoadRegulatorID()== roadRegulatorID && config.getIntersectionID() == intersectionID){
                    intersectionList.add(config);
                }
            }

        }


        List<Config> finalConfig = new ArrayList<>();

        for (DefaultConfig defaultConfig : defaultList) {
            Config addConfig = defaultConfig;
            for (IntersectionConfig intersectionConfig : intersectionList) {
                if (intersectionConfig.getKey().equals(defaultConfig.getKey())) {
                    addConfig = intersectionConfig;
                    System.out.println(defaultConfig.getKey());
                    System.out.println(addConfig);
                    break;
                }
            }
            finalConfig.add(addConfig);
        }


        if (finalConfig.size() > -1) {
            return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(finalConfig);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                    .body(new ArrayList<Config>());
        }
    }
}