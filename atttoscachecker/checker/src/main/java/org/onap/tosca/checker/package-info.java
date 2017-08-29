/**
 * The checker provides an api/tool for the verification of TOSCA yaml files.
 * <p>
 * It provides a three stage processing of a tosca yaml file:
 *   - yaml verification: is the document a valid yaml document as per yaml.org/spec. In particular we're using 
 *   the snakeyaml library for parsing the yaml document to a nested structure of java objects. Multiple yaml
 *	 documents per storage element/yaml character stream (file, stream) are supported, see section 9 of yaml spec. 
 *   - tosca yaml grammar validation: is the document a valid tosca yaml
 *   document, as per the the TOSCA simple profile for yaml. We use a modified
 *   version of the kwalify library for this task. The grammar for TOSCA yaml
 *   is itself a yaml document (found in the package in 
 *   resources/tosca-schema.yaml). There are certain limitations on how far 
 *   this grammar can go.  
 *   - consistency verification: we check the type hierarchies for all TOSCA
 *   constructs (data types, capability types, node types, etc), the definition
 *   of all facets of a construct (properties, attributes, etc) across the type
 *   hierachies, the conformity of construct templates (node templates, ..) with
 *   their types, data valuations(input assignements, constants, function calls).
 *
 * Each stage is blocking, i.e. a stage will be performed only if the previous
 * one completed successfully. Additional procesing can be added to each stage by 
 * providing code annotated as per the {@link org.onap.tosca.checker.annotations Annotations} package.  
 * <p>
 * The verification is done across all the imported documents. The common TOSCA 
 * types are by default made available to all documents being processed (the
 * specification is in resources/tosca-common-types.yaml). Networking related
 * types can be made available by importing resources/tosca-network-types.yaml
 * while the tosca nfv profile definitions are available at
 * resources/tosca-nfv-types.yaml. 
 * <p>
 * The main java classes exposed by the package are the Checker, Target
 * and Report. A Target represents a document processed by the Checker. While
 * the Checker starts with a top Target, through import statements it can end up
 * processing a number of Targets. The results of processing a Target are made
 * available through a Report which currently is nothing more that a list of
 * recorded errors.
 *
 * <div>
 * {@code
 *   Checker checker = new Checker();
 *	 checker.check("tests/example.yaml");
 *
 *	 for (Target t: checker.targets())
 *     System.out.println(t.getLocation() + "\n" + t.getReport());
 * }
 * </div>
 * <p>
 * A {@link org.onap.tosca.checker.Report report} consolidates the problems encountered
 * in the processed documents per Target.
 * The errors are recorded as instances of Exception, mostly due to the fact
 * snakeyaml and kwalify do report errors as exceptions. As such there are 3
 * basic types of errros to be expected in a report: YAMLException (from
 * snakeyaml, related to parsing), ValidationException (from kwalify, tosca
 * grammar validation), TargetException (from the checker itself). This might
 * change as we're looking to unify the way errors are reported. A Report 
 * object has a user friendly toString function.
 *
 * A CheckerException thrown during the checking process is an indication of a
 * malfunction in the checker itself.
 * <p>
 * The checker handles targets as URIs. The resolution of a target consists in
 * going from a string representing some path/uri to the absolute URI. 
 * The Targets are found through a TargetLocator which interprets a location string
 * as an URI. A custom TargetLocator (capable of interpreting particular URI schemes)
 * can be plugged in the Checker. Locating a target is actually the first stage of
 * target processing and and intrisic part of the process (resolving imported targets). 
 * The package provides a default {@link org.onap.tosca.checker.CommonLocator locator} 
 * which handles the common uri schemes (the jdk recognizes).
 * The example above highlights the fact that even when processing starts with one target
 * it could end up going over a larger set, through imports. 
 *
 * The results of processing a TOSCA yaml document (all all related documents through 
 * the import relationship) are stored in a {@link org.onap.tosca.checker.Catalog catalog}.
 * Catalogs are re-usable and the checker provides methods for processing targets with
 * respect to an existing catalog. 'With respect to a catalog' means that all the types
 * available in that catalog are visisble to the targets being processed; this facilitates
 * the processing of TOSCA templates organized as a schema (types specifications) and 
 * topology templates.
 * <p>
 * The most common entry point is {@link org.onap.tosca.checker.Checker#check(String) check},
 * where the argument is passed to thelocator and once a {@link org.onap.tosca.checker.Target target}
 * has been obtained, processing continues with the other stages.
 * The checker has processing entry points past the first two processing stages: an already located
 * target <br/>
 *	<ul>
 *    <li>{@link org.onap.tosca.checker.Checker#check(Target) check}</li>
 *    <li>{@link org.onap.tosca.checker.Checker#check(Target, Catalog) check}</li>
 *  </ul>
 * and after a target located and parsed <br/>
 *	<ul>
 *    <li>{@link org.onap.tosca.checker.Checker#validate(Target) check}</li>
 *    <li>{@link org.onap.tosca.checker.Checker#validate(Target, Catalog) check}</li>
 *  </ul>
 * </p>
 * <p>
 * The build process provides an all-in-one jar to facilitate the usage as a tool:
 * <p>
 * Other:
 * <ul>
 *   <li>the checker performs during tosca grammar validation a 'normalization'
 *   process as the tosca yaml profile allows for short forms in the 
 *   specification of a number of its constructs (see spec). The checker changes
 *   the actual structure of the parsed document such that only normalized
 *   (complete) forms of specification are present before the checking phase.
 *   (the kwalify library was extended in order to be able to specify these
 *   short forms in the grammar itself and process/tolerate them at validation
 *   time).
 *   </li>
 *   <li>the checker contains an internal catalog where the types and templates
 *   of different constructs are aggregated and indexed across all targets in
 *   order to facilitate the checking phase. Catalogs can be 'linked' and the
 *   resolution process delegated (the checker maintains a basic catalog with
 *   the core and common types and there is always a second catalog maintaining
 *   the information related to the current targets).
 *   The catalog is currently not exposed by the library.
 *   </li>
 *   <li>imports processing: the import statements present in a target are first 
 *   'detected' during tosca yaml grammar validation phase. At that stage all
 *   imports are (recursively) parsed and validated (first 2 phases). Checking
 *   off all imports (recursively) is done during stage 3.
 *   </li>
 * <ul>
 * <p>
 * ToDo:
 * 	 - nobody says the parsing cannot be pluggable: snakeyaml could be replaced by
 * anything that can take a Reader and produce a POJO hierarchy for the next stages. 
 */

package org.onap.tosca.checker;
