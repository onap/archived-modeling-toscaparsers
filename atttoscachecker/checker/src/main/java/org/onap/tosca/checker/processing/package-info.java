/**
 * After checking and cataloging we assume some applications will do some sort of post-processing.
 * While all such processing can be done in java by navigating catalog information this package focuses
 * on processing based on different 'environments' through a so called 'processor'. The package offers one
 * implementation, a Java Script processor that exposes Checker catalog information to java scripts executed
 * through Nashorn, the Javs (8) built-in java script engine.
 *  
 */

package org.onap.tosca.checker.processing;
