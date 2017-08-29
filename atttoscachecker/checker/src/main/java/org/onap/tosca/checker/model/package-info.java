/**
 * The package contains Java representations of TOSCA constructs.
 * During processing (parsing, checking, cataloging) a POJO representation of the TOSCA yaml document content
 * is being used. For any post-processing the user has the option of wrapping the POJO representations in these
 * domain specific representations.
 * Domain specific elements are created 'as needed' as the client navigates through the template content and the
 * goal here was not to change the POJO-based structure, but just to wrap it in compatible elements (in this way
 * all other methods of post processing stay compatible: jxpath, etc).
 * The TOSCA specific representations are defined as interfaces for which implementations are built at run-time,
 * implementations which delegate to the existing POJO representations resulting from the checker.
 * 
 * The entry points are defined in {@link org.onap.tosca.checker.models.Models Models}.
 * 
 * ToDo: more entry points needed, at least for catalog based navigation of available types.
 */

package org.onap.tosca.checker.model;
