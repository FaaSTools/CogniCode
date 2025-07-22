package org.example.ontology;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.util.Map;

record DataItem(Resource dtype, Map<Property, RDFNode> feats) {
}
