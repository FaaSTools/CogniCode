package org.example.ontology;

import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class OntologyPathFinder {

    private static final Property SRV_CANFOLLOW = ResourceFactory.createProperty(OntologyNamespace.SRV, "canFollow");

    private static boolean inputFits(Map<Property, RDFNode> need, Map<Property, RDFNode> have) {
        for (var e : need.entrySet()) {
            RDFNode vNeed = e.getValue();
            if (vNeed.isLiteral() && "ANY".equals(vNeed.asLiteral().getString())) {
                continue;
            }
            if (!vNeed.equals(have.get(e.getKey()))) {
                return false;
            }
        }
        return true;
    }

    private static Model buildEdgeGraph(Model cat) {
        String queryFormat = """
                PREFIX srv:  <%s>\n
                PREFIX dt:  <%s>\n
                PREFIX rdf: <%s>\n
                CONSTRUCT { ?dtype srv:canFollow ?svc } WHERE {\n
                    { ?svc srv:accepts ?dtype }\n
                    UNION\n
                    { ?svc srv:accepts / rdf:first|rdf:rest*/rdf:first ?dtype }\n
                }
                """;
        String query = String.format(queryFormat, OntologyNamespace.SRV, OntologyNamespace.DT, RDF.getURI());
        return QueryExecutionFactory.create(query, cat).execConstruct();
    }

    private static Map<Property, RDFNode> getFeatures(Resource node) {
        Map<Property, RDFNode> map = new HashMap<>();
        node.listProperties().forEachRemaining(st -> {
            if (!st.getPredicate().equals(RDF.type)) {     // skip datatype marker
                map.put(st.getPredicate(), st.getObject());
            }
        });
        return map;
    }

    private static Map<Property, RDFNode> makeOutput(Map<Property, RDFNode> chainSpecs,
                                                     Map<Property, RDFNode> currentServiceSpecs,
                                                     List<Property> preserves,
                                                     Map<Property, RDFNode> goalSpecs) {
        // initialize with current specs of the chain
        Map<Property, RDFNode> out = new HashMap<>(chainSpecs);
        currentServiceSpecs.forEach((p, v) -> {
            if (v.isLiteral() && "ANY".equals(v.asLiteral().getString())) {
                RDFNode goalVal = goalSpecs.get(p);
                if (goalVal != null) {
                    out.put(p, goalVal); // use goal value if available
                } else {
                    out.remove(p); // remove if no specific goal value
                }
            } else {
                out.put(p, v);
            }
        });

        for (Property preserve : preserves) {
            if (currentServiceSpecs.containsKey(preserve)) {
                out.put(preserve, currentServiceSpecs.get(preserve));
            }
        }
        return out;
    }

    private static List<Resource> findChain(Resource startType, Map<Property, RDFNode> startFeats,
                                            Resource goalType, Map<Property, RDFNode> goalFeats,
                                            Model catalog, Model edges) {
        Queue<State> q = new ArrayDeque<>();
        q.add(new State(null, new DataItem(startType, startFeats), List.of(startType)));
        Set<String> seen = new HashSet<>();

        while (!q.isEmpty()) {
            State currentState = q.poll();
            if (currentState.item().dtype().equals(goalType) &&
                    inputFits(goalFeats, currentState.item().feats())) {
                return currentState.chain();
            }

            StmtIterator edgeIt = edges.listStatements(currentState.item().dtype(), SRV_CANFOLLOW, (RDFNode) null);
            while (edgeIt.hasNext()) {
                Statement statement = edgeIt.next();
                Resource service = catalog.getResource(statement.getObject().asResource().getURI()).inModel(catalog);

                service.listProperties(OntologyNamespace.SRV_PRODUCES).forEachRemaining(prodStmt -> {

                    Resource outNode = prodStmt.getObject().asResource();
                    Resource nextType = outNode.hasProperty(RDF.type)
                            ? outNode.getPropertyResourceValue(RDF.type)
                            : outNode;

                    Map<Property, RDFNode> nextFeatures = makeOutput(currentState.item().feats(),
                            getFeatures(outNode),
                            service.listProperties(OntologyNamespace.SRV_PRESERVES).toList()
                                    .stream()
                                    .map(st -> st.getObject().as(Property.class))
                                    .collect(Collectors.toList()),
                            goalFeats);

                    String sig = service.getURI() + "|" + nextType + "|" + nextFeatures;
                    if (seen.add(sig)) {
                        var chain = new ArrayList<>(currentState.chain());
                        chain.add(service);        // service
                        chain.add(nextType);      // datatype “branch”
                        q.add(new State(service, new DataItem(nextType, nextFeatures), chain));
                    }
                });

            }
        }
        return List.of();
    }

    public static List<Resource> findValidServiceChain(String startTypeURI,
                                                       List<String> startFeatsUris,
                                                       String goalTypeURI,
                                                       List<String> goalFeatsUris,
                                                       Path serviceCatalogPath) {
        Model catalog = ModelFactory.createDefaultModel();
        catalog.read(serviceCatalogPath.toUri().toString(), "JSON-LD");

        Resource startType = ResourceFactory.createProperty(startTypeURI);
        Resource goalType = ResourceFactory.createProperty(goalTypeURI);

        Map<Property, RDFNode> startFeats = new HashMap<>();
        for (String featUri : startFeatsUris) {

            String propertyName = featUri.split("=")[0];
            String resultValue = featUri.split("=")[1];
            startFeats.put(ResourceFactory.createProperty(propertyName),
                    catalog.createLiteral(resultValue));
        }

        Map<Property, RDFNode> goalFeats = new HashMap<>();
        for (String featUri : goalFeatsUris) {
            String propertyName = featUri.split("=")[0];
            String resultValue = featUri.split("=")[1];
            goalFeats.put(ResourceFactory.createProperty(propertyName),
                    catalog.createLiteral(resultValue));
        }
        return findValidServiceChain(startType, startFeats,
                goalType, goalFeats, catalog);
    }

    public static List<Resource> findValidServiceChain(Resource startType,
                                                       Map<Property, RDFNode> startFeats,
                                                       Resource goalType,
                                                       Map<Property, RDFNode> goalFeats,
                                                       Path serviceCatalogPath) throws Exception {

        Model catalog = ModelFactory.createDefaultModel();
        catalog.read(serviceCatalogPath.toUri().toString(), "JSON-LD");
        return findValidServiceChain(startType, startFeats, goalType, goalFeats, catalog);
    }

    private static List<Resource> findValidServiceChain(Resource startType,
                                                        Map<Property, RDFNode> startFeats,
                                                        Resource goalType,
                                                        Map<Property, RDFNode> goalFeats,
                                                        Model catalog) {
        Model edges = buildEdgeGraph(catalog);
        return findChain(startType, startFeats, goalType, goalFeats, catalog, edges);
    }

}
