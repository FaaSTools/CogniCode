## 🗂️ Service-Composition Ontology

This ontology is a **minimal semantic layer** that describes cloud services by their effects. From a user's intent, CogniCode derives which services to use and in what order (the composition plan), which the LLM then implements.  
Everything is plain RDF + JSON-LD

### 1. Namespaces & Prefixes

| Prefix | Base IRI                              | Purpose                                           |
| :----: | ------------------------------------- | ------------------------------------------------- |
| `srv:` | `http://example.org/services#`       | Service individuals & their properties            |
| `dt:`  | `http://example.org/data-types#`     | Atomic data formats (`dt:PDF`, `dt:MP3`, …)       |

### 2. Core Vocabulary

| Term | Type | Meaning |
| ---- | ---- | ------- |
| `srv:Service` | **Class** | Every cloud API / managed service |
| `srv:accepts` / `srv:produces` | Object prop. | Point to format nodes that may carry feature literals |
| `srv:preserves` | Object prop. | Declares a feature (e.g. `dt:language`) the service leaves unchanged |
| `srv:capabilities` | Literal prop. | Free-text label that documents the service for readers (“Translation”, “OCR”, …). It is not used for planning and does not enter the prompt. |
| `srv:hasLimit` | Object prop. | Links to a blank node holding quota info (`srv:limitName`, `srv:limitValue`, `srv:limitUnit`) |

For ease of use, the core vocabulary is defined equivalently in `services_aws.jsonld` and `services_gcp.jsonld`.
A SPARQL query in `OntologyPathFinder.java` extracts from the ontology which services accept which data types. A breadth-first search over these links finds the composition plan.

### 3. Feature Model

* **Features** (language, sample-rate, …) are plain `dt:*` literals on the accepts/produces nodes.
* Literal **`"ANY"` = wildcard**.
* **Implicit-preserve rule:** every incoming feature is copied forward unless the service overrides it **or** deletes it via `"ANY"`.
* For translation services the pipeline replaces `"ANY"` with the user’s target language (e.g. `"de"` → German).

### 4. Files

| File | Contents |
| ---- | -------- |
| `services_gcp.jsonld` | 6 Google Cloud services (Document AI, Text-to-Speech, …) |
| `services_aws.jsonld` | 6 AWS services (Translate, Polly, Textract, …) |