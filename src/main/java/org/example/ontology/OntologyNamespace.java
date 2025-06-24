package org.example.ontology;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public interface OntologyNamespace {

    String SRV = "http://example.org/services#";
    String DT = "http://example.org/data-types#";

    Property SRV_CATEGORY   = ResourceFactory.createProperty(SRV,"category");
    Property SRV_NAME       = ResourceFactory.createProperty(SRV,"name");
    Property SRV_PROVIDER   = ResourceFactory.createProperty(SRV,"provider");
    Property SRV_ACCEPTS    = ResourceFactory.createProperty(SRV,"accepts");
    Property SRV_PRODUCES   = ResourceFactory.createProperty(SRV,"produces");
    Property SRV_CAPABILITIES = ResourceFactory.createProperty(SRV,"capabilities");
    Property SRV_PRESERVES  = ResourceFactory.createProperty(SRV,"preserves");

    Property SRV_HAS_LIMIT  = ResourceFactory.createProperty(SRV,"hasLimit");
    Property SRV_LIMIT_NAME = ResourceFactory.createProperty(SRV,"limitName");
    Property SRV_LIMIT_VAL  = ResourceFactory.createProperty(SRV,"limitValue");
    Property SRV_LIMIT_UNIT = ResourceFactory.createProperty(SRV,"limitUnit");

    Property DT_LANGUAGE = ResourceFactory.createProperty(DT,"language");
}
