package org.example.ontology;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.example.config.CloudServiceFactory;
import org.example.model.CloudService;
import org.example.model.ServiceLimits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OntologyToServiceMapper {

    public static List<CloudService> mapFrom(List<Resource> ontologyServiceChain) {
        if (ontologyServiceChain == null || ontologyServiceChain.isEmpty()) {
            return List.of();
        }
        return ontologyServiceChain.stream()
                .filter(component -> component.getNameSpace().equals(OntologyNamespace.SRV))
                .map(OntologyToServiceMapper::mapResourceToService)
                .toList();
    }

    private static CloudService mapResourceToService(Resource service) {
        String serviceName = service.getProperty(OntologyNamespace.SRV_NAME).getString();
        String serviceCategory = service.getProperty(OntologyNamespace.SRV_CATEGORY).getString();
        String provider = service.getProperty(OntologyNamespace.SRV_PROVIDER).getString();

        List<String> inputTypes = listValues(service, OntologyNamespace.SRV_ACCEPTS);
        List<String> outputTypes = listValues(service, OntologyNamespace.SRV_PRODUCES);

        List<String> capabilities = listValues(service, OntologyNamespace.SRV_CAPABILITIES);

        ServiceLimits serviceLimits = extractServiceLimits(service);
        return CloudServiceFactory.createService(serviceCategory, serviceName, provider,
                inputTypes, outputTypes, capabilities, serviceLimits);
    }

    private static ServiceLimits extractServiceLimits(Resource service) {
        Map<String, Map<String, Object>> limits = new HashMap<>();

        service.listProperties(OntologyNamespace.SRV_HAS_LIMIT).forEachRemaining(st -> {
           Resource limit = st.getObject().asResource();
           String name = limit.getProperty(OntologyNamespace.SRV_LIMIT_NAME).getString();
           int value = limit.getProperty(OntologyNamespace.SRV_LIMIT_VAL).getInt();
           String unit = limit.getProperty(OntologyNamespace.SRV_LIMIT_UNIT).getString();
           limits.put(name, Map.of("value", value, "unit", unit));
        });
        return new ServiceLimits(limits);
    }

    private static List<String> listValues(Resource service, Property property) {
        List<String> list = new ArrayList<>();
        service.listProperties(property).forEachRemaining(st -> {
            String value;
            if (st.getObject().isLiteral()) {
                list.add(st.getObject().asLiteral().getString());
            } else if (st.getObject().isResource() && st.getObject().asResource().hasProperty(RDF.type)) {
                value = st.getObject().asResource().getPropertyResourceValue(RDF.type).getLocalName();
                list.add(value);
            } else {
                list.add(st.getObject().asResource().getLocalName());
            }
        });
        return list;
    }


}
