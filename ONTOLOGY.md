## 🗂️ Service-Composition Ontology

This ontology is a **minimal semantic layer** that filters cloud services, given a users intent hands an LLM only the candidates it needs.  
Everything is plain RDF + JSON-LD

### 1. Namespaces & Prefixes

| Prefix | Base IRI                              | Purpose                                           |
| :----: | ------------------------------------- | ------------------------------------------------- |
| `srv:` | `https://example.org/services#`       | Service individuals & their properties            |
| `dt:`  | `https://example.org/data-types#`     | Atomic data formats (`dt:PDF`, `dt:MP3`, …)       |

### 2. Core Vocabulary

| Term | Type | Meaning |
| ---- | ---- | ------- |
| `srv:Service` | **Class** | Every cloud API / managed service |
| `srv:accepts` / `srv:produces` | Object prop. | Point to format nodes that may carry feature literals |
| `srv:preserves` | Object prop. | Declares a feature (e.g. `dt:language`) the service leaves unchanged |
| `srv:capability` | Literal prop. | Free-text verb that aids LLM retrieval (“Translation”, “OCR”, …) |
| `srv:hasLimit` | Object prop. | Links to a blank node holding quota info (`srv:limitName`, `srv:limitValue`, `srv:limitUnit`) |

For ease of use the core vocabulary is defined equivalently `services_aws.jsonld` and `services_gcp.jsonld`.

### 3. Feature Model

* **Features** (language, sample-rate, …) are plain `dt:*` literals on the accepts/produces nodes.  
* Literal **`"ANY"` = wildcard**.  
* **Implicit-preserve rule:** every incoming feature is copied forward unless the service overrides it **or** deletes it via `"ANY"`.  
* For translation services the pipeline replaces `"ANY"` with the user’s target language (e.g. `"de"` → German).

### 4. Files

| File | Contents |
| ---- | -------- |
| `gcp_services.jsonld` | 6 Google Cloud services (Document AI, Text-to-Speech, …) |
| `aws_services.jsonld` | 6 AWS services (Translate, Polly, Textract, …) |


