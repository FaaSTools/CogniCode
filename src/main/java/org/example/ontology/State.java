package org.example.ontology;

import org.apache.jena.rdf.model.Resource;

import java.util.List;

record State(Resource svc, DataItem item, List<Resource> chain) {
}
