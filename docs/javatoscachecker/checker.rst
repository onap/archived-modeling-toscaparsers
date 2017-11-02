1. History
==========

 * Development started over 2 years ago as part of the Network Automation, the Intelligent Service Composition project
 * It was supporting the model driven composition of resources/services/products

2. Scope
========

 * Development of a tool as generic as possible
   - not bound to a particular schema/profile
   - no particular type of post-processing (orchestrator)
   - limited to TOSCA yaml profile
   - Java language

3. Goals
========

 * Ensure that a (set of) TOSCA template(s)is conform to a correctly defined type system
   - avoid errors at more expensive later processing stages
   - pre-requisite to further processing: UI rendeing, persistence

 * Generate an intermediate form that could facilitate further processing
 * Extensibility
   - accommodate extensions to the standard and checks pertinent to these extensions

4. The checker
==============

 * Validate yaml document
   - supports streaming documents (multiple documents within one file), yaml anchors, etc
   - currently uses the snakeyaml library
 * Syntax check
   - two pre-defined grammars, 1.0 and 1.1, with the possibility of handling a mix of documents version wise
     + other versions/variants can be added 'on the fly', identified through the tosca_definitions_version entry
   - accept the short forms
     + declare the short forms within the grammar
     + builds a canonical form from which the shorts forms are eliminated (so further processing steps do not need to handle them)
   - grammar written in yaml
     + easy to maintain/modify
     + processed through a modified version of the kwalify library
 * Checks
   - type hierarchy checks for all constructs
     + valid re-definitions: from relatively simple (properties) to rather complicated (interface opearations)
   - valid type references: all referenced types are pre-declared (as super-types, as valid source types, etc)
   - templates respect their respective type definition
     + example: check type of interface operation inputs
     + other references: capabilities and requirements in substitution mappings
   - data checks: assignments match the type specification, function argument compatibility (for built-in functions), constraints matching
 * Process the entire document tree specified through import statements
 * Extensibility
   - pluggable document locator/loader
   - pluggable handlers at syntax check (pre/post validation)
   - pluggable checks based on document location
     + a new construct (new grammar) can be subject to new semantic checks plugged without checker code being re-built.

5. Output
=========

 * Error reporting
   - Differs depending on the stages
     + Document position indication during parsing
     + Document path and rule during syntax check
     + Document path during checking
 * Catalog
   - No explicit representation of TOSCA constructs, offers a query interface with results being exposed as common Java types: maps, lists, ..
   - domain specific (TOSCA constructs) navigation built through a proxy based approach on top of above representation 

6. Usage
========

 * API
   - simple API that allows access to any stage of the checker
     + One can build an in-memory representation of a TOSCA document and skip the yaml parsing.
     + Re-usable Catalog (hierarchical Catalogs)
     + One or more documents can be processed and the resulting Catalog be preserved and used for later processing of other documents using the previous catalog as base catalog
 * CLI
   - basic command line interface
 * Service
   - stateful REST service layer on top of checker API
     + a schema (document with type specifications) can be submitted and be referenced (imports) from subsequently submitted templates


7. Post-processing
==================

 * A tiny framework for additional processing
   - one basic implementation of a JavaScript post-processor where the Catalog and topology graph are exposed to javascript scripts implementing business validation rules 

