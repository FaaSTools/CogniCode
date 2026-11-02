import org.apache.jena.rdf.model.Resource;
import org.example.model.CloudService;
import org.example.ontology.OntologyPathFinder;
import org.example.ontology.OntologyToServiceMapper;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service-extension example: derives the composition plans of the evaluated
 * use cases and of one new intent from the original ontology and from a copy
 * that adds a single service entry (AWS Comprehend). Uses only the planning
 * part of CogniCode, no LLM calls and no cloud access.
 */
public class ExtensionDemo {

    static final String DT = "http://example.org/data-types#";

    record Intent(String name, String in, String inLang, String out, String outLang) {}

    static final List<Intent> INTENTS = List.of(
            new Intent("UC1", "PDF", "english", "MP3", "english"),
            new Intent("UC2", "MP3", "english", "MP3", "german"),
            new Intent("UC3", "MP4", "english", "Text", "english"),
            new Intent("NEW (audio sentiment)", "MP3", "english", "JSON", "english"));

    static List<Resource> plan(Intent i, Path ontology) {
        return OntologyPathFinder.findValidServiceChain(
                DT + i.in(), List.of(DT + "language=" + i.inLang()),
                DT + i.out(), List.of(DT + "language=" + i.outLang()),
                ontology);
    }

    static String show(List<Resource> chain) {
        return chain.isEmpty() ? "no plan found"
                : chain.stream().map(Resource::getLocalName).collect(Collectors.joining(" -> "));
    }

    public static void main(String[] args) {
        Path original = Path.of(args.length > 0 ? args[0] : "../application/services_aws.jsonld");
        Path extended = Path.of(args.length > 1 ? args[1] : "services_aws_extended.jsonld");

        for (Intent i : INTENTS) {
            String before = show(plan(i, original));
            List<Resource> afterChain = plan(i, extended);
            String after = show(afterChain);
            System.out.printf("%s%n  original ontology: %s%n  extended ontology: %s%n  %s%n%n",
                    i.name(), before, after, before.equals(after) ? "unchanged" : "CHANGED");
        }

        // The new plan is mapped to service objects exactly as Main does before prompting.
        List<CloudService> pipeline = OntologyToServiceMapper.mapFrom(plan(INTENTS.get(3), extended));
        System.out.println("Pipeline handed to the Orchestration Agent for the new intent:");
        pipeline.forEach(s -> System.out.printf("  %s: accepts %s, produces %s%n",
                s.getName(), s.getInputFormat(), s.getOutputFormat()));
    }
}
