.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0


This document provides the release notes for <Danube Release> of <Parser>.

.. contents::
   :depth: 3
   :local:


Version history
---------------

+--------------------+--------------------+--------------------+--------------------+
| **Date**           | **Ver.**           | **Author**         | **Comment**        |
|                    |                    |                    |                    |
+--------------------+--------------------+--------------------+--------------------+
| 2017-03-06         | 1.0                | Howard Huang       | Danube Release     |
|                    |                    |                    |                    |
+--------------------+--------------------+--------------------+--------------------+
| 2017-09-25         | 2.0                | Shang Xiaodong     | Euphrates release  |
|                    |                    |                    |                    |
+--------------------+--------------------+--------------------+--------------------+
|                    |                    |                    |                    |
|                    |                    |                    |                    |
+--------------------+--------------------+--------------------+--------------------+

Important notes
===============

**Attention**: Parser's submodules should be installed independantly from OPNFV platform, for
instructions please refer to the installation documention.


Summary
=======

OPNFV Parser Danube release has provides four submodules to serve as translation/prevalidation
tools: tosca2heat, yang2tosca, policy2tosca, and verigraph. Specifically, tosca2heat module
provides an integration of OpenStack tosca-parser and heat-translater
upstream projects 0.7 release with additional Parser unique features.


Release Data
============


+--------------------------------------+--------------------------------------+
| **Project**                          | OPNFV Parser                         |
+--------------------------------------+--------------------------------------+
| **Repo/commit-ID**                   |                                      |
+--------------------------------------+--------------------------------------+
| **Release designation**              | Danube 1.0                           |
+--------------------------------------+--------------------------------------+
| **Release date**                     | E.g. 2015-04-16                      |
+--------------------------------------+--------------------------------------+
| **Purpose of the delivery**          | OPNFV Template Translation/Prevalida-|
|                                      | tion tooling                         |
+--------------------------------------+--------------------------------------+

Version change
^^^^^^^^^^^^^^

Module version changes
~~~~~~~~~~~~~~~~~~~~~~

- OpenStack tosca-parser and heat-translator have changed from 0.6 to 0.7

- new verigraph module

- no feature changes in yang2tosca and policy2tosca since Colorado Release.

Document version changes
~~~~~~~~~~~~~~~~~~~~~~~~

- Parser Dabube Release documentation has adoped a new format.

Reason for version
^^^^^^^^^^^^^^^^^^
Feature additions
~~~~~~~~~~~~~~~~~
<STATE ADDED FEATURES BY REFERENCE TO JIRA>

<EXAMPLE>:

**JIRA BACK-LOG:**

+--------------------------------------+--------------------------------------+
| **JIRA REFERENCE**                   | **SLOGAN**                           |
|                                      |                                      |
+--------------------------------------+--------------------------------------+
| PARSER-111                           | Verigraph Code Base                  |
+--------------------------------------+--------------------------------------+
| PARSER-112                           | Init api gateway framework           |
+--------------------------------------+--------------------------------------+
| PARSER-113                           | Add input validation test case       |
+--------------------------------------+--------------------------------------+
| PARSER-114                           | Add output in vRNC for substitution  |
|                                      | mappings.                            |
+--------------------------------------+--------------------------------------+
| PARSER-115                           | Add output validation for            |
|                                      | substitution.                        |
+--------------------------------------+--------------------------------------+
| PARSER-116                           | Add output validation test case for  |
|                                      | substitution.                        |
+--------------------------------------+--------------------------------------+
| PARSER-121                           | Add ip output in compute node        |
+--------------------------------------+--------------------------------------+
| PARSER-125                           | Package for funectest with docker    |
|                                      | container.                           |
+--------------------------------------+--------------------------------------+
| PARSER-135                           | Support costum datatype in           |
|                                      | capability.                          |
+--------------------------------------+--------------------------------------+
| PARSER-136                           | Support metadata validation          |
+--------------------------------------+--------------------------------------+
| PARSER-137                           | Support yaml file with suffix of yml |
+--------------------------------------+--------------------------------------+
| PARSER-138                           | Support costum datatype definition   |
|                                      | cin parameters.                      |
+--------------------------------------+--------------------------------------+
| PARSER-141                           | Add required parameters validation   |
|                                      | for nested service                   |
+--------------------------------------+--------------------------------------+
| PARSER-142                           | Add parameter validation in design   |
|                                      | time for ONAP                        |
+--------------------------------------+--------------------------------------+
| PARSER-144                           | Add import file with suffix of yml   |
|                                      | testcases                            |
+--------------------------------------+--------------------------------------+
| PARSER-146                           | Support template version of          |
|                                      | tosca_simple_yaml_1_1                |
+--------------------------------------+--------------------------------------+
| PARSER-148                           | Add debug mode parameter             |
+--------------------------------------+--------------------------------------+
| PARSER-151                           | Refactor heat-translator setup.py    |
+--------------------------------------+--------------------------------------+
| PARSER-154                           | code optimizations about graph       |
|                                      | manipulation and formula generation. |
+--------------------------------------+--------------------------------------+


Bug corrections
~~~~~~~~~~~~~~~

**JIRA TICKETS:**

+--------------------------------------+--------------------------------------+
| **JIRA REFERENCE**                   | **SLOGAN**                           |
|                                      |                                      |
+--------------------------------------+--------------------------------------+
| PARSER-117                           | Fix ci bug when integrated with      |
|                                      | functest                             |
+--------------------------------------+--------------------------------------+
| PARSER-122                           | Fix docs bug                         |
+--------------------------------------+--------------------------------------+
| PARSER-145                           | Fix exceptions overwritten when      |
|                                      | nested import service topology       |
+--------------------------------------+--------------------------------------+
| PARSER-152                           | fix costum datatype definition       |
|                                      | in parameters                        |
+--------------------------------------+--------------------------------------+
| PARSER-153                           | Fix StatefulEntityType when          |
|                                      | entitytype is not define             |
+--------------------------------------+--------------------------------------+
| PARSER-155                           | Fix substitution mapping assigned    |
|                                      | value to nodetemplate                |
+--------------------------------------+--------------------------------------+
| PARSER-156                           | Fix functest_run script for role     |
|                                      | of heat_stack_owner                  |
+--------------------------------------+--------------------------------------+


Deliverables
------------

Software deliverables
^^^^^^^^^^^^^^^^^^^^^

- tosca2heat
- yang2tosca
- policy2tosca
- verigraph
- apigateway

Documentation deliverables
^^^^^^^^^^^^^^^^^^^^^^^^^^

- release document
- design document
- development document
- testing document


Known Limitations, Issues and Workarounds
=========================================

System Limitations
^^^^^^^^^^^^^^^^^^




Known issues
^^^^^^^^^^^^


**JIRA TICKETS:**

+--------------------------------------+--------------------------------------+
| **JIRA REFERENCE**                   | **SLOGAN**                           |
|                                      |                                      |
+--------------------------------------+--------------------------------------+
|                                      |                                      |
+--------------------------------------+--------------------------------------+
|                                      |                                      |
+--------------------------------------+--------------------------------------+

Workarounds
^^^^^^^^^^^

<STATE ALL KNOWN WORKAROUNDS TO THE ISSUES STATED ABOVE>

<EXAMPLE>:

- In case the contact with a compute is lost - restart the compute host
- In case the disk is full on a controller - delete all files in /tmp

Test Result
===========
<STATE THE QA COVERAGE AND RESULTS>

<EXAMPLE>:

Fuel@OPNFV Arno RC2 has undergone QA test runs with the following results:

+--------------------------------------+--------------------------------------+
| **TEST-SUITE**                       | **Results:**                         |
|                                      |                                      |
+--------------------------------------+--------------------------------------+
|   Parser with functest in fuel       |   PASS                               |
+--------------------------------------+--------------------------------------+
|   Parser with functest in apex       |   PASS                               |
+--------------------------------------+--------------------------------------+
|   Parser with functest in daisy      |   PASS                               |
+--------------------------------------+--------------------------------------+
|   Parser with functest in compass    |   PASS                               |
+--------------------------------------+--------------------------------------+
|   Parser with functest in joid       |   SKIP                               |
+--------------------------------------+--------------------------------------+

References
==========


For more information on the OPNFV Danube release, please see:

http://opnfv.org/danube
