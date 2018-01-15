package io.foben.k8s.configoperator.service;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class CMService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CMService.class);

    @Value("${kubernetes.client.namespace}")
    private String namespace;

    private KubernetesClient client = new DefaultKubernetesClient();

    @Scheduled(fixedRate=5000)
    public void checkRecords() {
        LOGGER.debug("Analyzing deployments for changed ConfigMaps");
        DeploymentList deploymentList = client.extensions().deployments().inNamespace(namespace).list();
        for (Deployment deployment : deploymentList.getItems()) {
            String deploymentName = deployment.getMetadata().getName();
            LOGGER.debug("Searching for containers in deployment {}", deploymentName);

            PodSpec podSpec = deployment.getSpec().getTemplate().getSpec();
            if (podSpec == null) {
                LOGGER.warn("Could not retrieve Podspec for deployment {}.", deploymentName);
                continue;
            }
            Set<ConfigMapKeySelector> referencedConfigs = new HashSet<ConfigMapKeySelector>();
            for (Container container : podSpec.getContainers()) {
                referencedConfigs.addAll(getConfigMapsReferencedInEnvVars(container));
            }
            for(ConfigMapKeySelector configMapRef : referencedConfigs) {
                String annotation = getConfigMapAnnotation(configMapRef);
                updateConfigMapRef(deploymentName, configMapRef.getName(), annotation);
            }
        }
    }

    private Set<ConfigMapKeySelector> getConfigMapsReferencedInEnvVars(Container container) {
        Set<ConfigMapKeySelector> result = new HashSet<ConfigMapKeySelector>();
        LOGGER.debug("Searching for referenced ConfigMaps in container {}", container.getName());
        for (EnvVar envVar : container.getEnv() ) {
            EnvVarSource varSource = envVar.getValueFrom();
            if (varSource == null) {
                continue;
            }
            ConfigMapKeySelector configMapKeyRef = varSource.getConfigMapKeyRef();
            if (configMapKeyRef != null) {
                LOGGER.debug("Found reference to ConfigMap '{}' in container {}", configMapKeyRef.getName(), container.getName());
                result.add(configMapKeyRef);
            }

        }
        return result;
    }

    private String getConfigMapAnnotation(ConfigMapKeySelector configMapref) {
        ConfigMap configMap = client.configMaps().inNamespace(namespace).withName(configMapref.getName()).get();
        if (configMap == null) {
            LOGGER.warn("Could not retrieve ConfigMap '{}'", configMapref.getName());
            return null;
        }
        String resourceVersion = configMap.getMetadata().getResourceVersion();
        LOGGER.debug("Resource Version for ConfigMap '{}' is '{}'",
                configMapref.getName(), resourceVersion);
        return resourceVersion;
    }

    private void updateConfigMapRef(String deploymentName, String configMapName, String configMapAnnotation) {
        String annotationName = "configmap-ref-" + configMapName;
        LOGGER.debug("Setting annotation '{}' of deployment '{}' to value '{}'",
                annotationName, deploymentName, configMapAnnotation);
        client.extensions().deployments().inNamespace(namespace).withName(deploymentName).edit()
                .editSpec()
                .editTemplate()
                    .editMetadata()
                    .addToAnnotations(annotationName, configMapAnnotation)
                    .endMetadata()
                .endTemplate()
                .endSpec()
                .done();
    }

}
