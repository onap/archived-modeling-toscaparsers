.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0


This document provides the release notes for nfv-toscaparser.

.. contents::
   :depth: 3
   :local:


Version history
---------------

+--------------------+--------------------+--------------------+--------------------+
| **Date**           | **Ver.**           | **Author**         | **Comment**        |
|                    |                    |                    |                    |
+--------------------+--------------------+--------------------+--------------------+
| 2017-09-25         | 0.5.0                | Shang Xiaodong   |  ONAP release 1    |
|                    |                    |                    |                    |
+--------------------+--------------------+--------------------+--------------------+
|                    |                    |                    |                    |
|                    |                    |                    |                    |
+--------------------+--------------------+--------------------+--------------------+


Summary
=======

Modeling has provides two submodules to serve as simple tosca template parse/prevalidation
tools: nfv-toscaparser and javatoscachecker. Specifically, nfv-toscaparser module provides
an integration of OpenStack tosca-parser and OPNFV parser with additional some unique features
from ONAP.


Release Data
============


+--------------------------------------+--------------------------------------+
| **Project**                          | ONAP Modeling                        |
+--------------------------------------+--------------------------------------+
| **Repo/commit-ID**                   |                                      |
+--------------------------------------+--------------------------------------+
| **Release designation**              | Release 1.0                          |
+--------------------------------------+--------------------------------------+
| **Release date**                     | E.g. 2017-09-25                      |
+--------------------------------------+--------------------------------------+
| **Purpose of the delivery**          | Simple TOSCA Template Parse/Preval-  |
|                                      | idation tooling                      |
+--------------------------------------+--------------------------------------+

Version change
^^^^^^^^^^^^^^

Module version changes
~~~~~~~~~~~~~~~~~~~~~~

- OpenStack tosca-parser have changed from 0.8

- new feature added according to the others ONAP project's requirements

- Fixed some bugs.

Document version changes
~~~~~~~~~~~~~~~~~~~~~~~~

- nfv-toscaparser documentation has adoped a new format.

Reason for version
^^^^^^^^^^^^^^^^^^
Feature additions
~~~~~~~~~~~~~~~~~
<STATE ADDED FEATURES BY REFERENCE TO JIRA>

<EXAMPLE>:

**JIRA BACK-LOG:**

+---------------------+--------------------------------------+
| **REFERENCE ID**    | **SLOGAN**                           |
|                     |                                      |
+---------------------+--------------------------------------+
|  1                  | Verigraph Code Base                  |
+---------------------+--------------------------------------+
|  2                  | Init api gateway framework           |
+---------------------+--------------------------------------+
|  3                  | Add input validation test case       |
+---------------------+--------------------------------------+
|  4                  | Add output in vRNC for substitution  |
|                     | mappings.                            |
+---------------------+--------------------------------------+
|  5                  | Add output validation for            |
|                     | substitution.                        |
+---------------------+--------------------------------------+
|  6                  | Add output validation test case for  |
|                     | substitution.                        |
+---------------------+--------------------------------------+
|  7                  | Add ip output in compute node        |
+---------------------+--------------------------------------+
|  8                  | Package for funectest with docker    |
|                     | container.                           |
+---------------------+--------------------------------------+
|  9                  | Support costum datatype in           |
|                     | capability.                          |
+---------------------+--------------------------------------+
|  10                 | Support metadata validation          |
+---------------------+--------------------------------------+
|  11                 | Support yaml file with suffix of yml |
+---------------------+--------------------------------------+
|  12                 | Support costum datatype definition   |
|                     | cin parameters.                      |
+---------------------+--------------------------------------+
|  13                 | Add required parameters validation   |
|                     | for nested service                   |
+---------------------+--------------------------------------+
|  14                 | Add parameter validation in design   |
|                     | time for ONAP                        |
+---------------------+--------------------------------------+
|  15                 | Add import file with suffix of yml   |
|                     | testcases                            |
+---------------------+--------------------------------------+
|  16                 | Support template version of          |
|                     | tosca_simple_yaml_1_1                |
+---------------------+--------------------------------------+
|  17                 | Add debug mode parameter             |
+---------------------+--------------------------------------+
|  18                 | Refactor heat-translator setup.py    |
+---------------------+--------------------------------------+
|  19                 | code optimizations about graph       |
|                     | manipulation and formula generation. |
+---------------------+--------------------------------------+


Bug corrections
~~~~~~~~~~~~~~~

**JIRA TICKETS:**

+---------------------+--------------------------------------+
| **REFERENCE ID**    | **SLOGAN**                           |
|                     |                                      |
+---------------------+--------------------------------------+
|  1                  | Fix ci bug when integrated with      |
|                     | functest                             |
+---------------------+--------------------------------------+
|  2                  | Fix docs bug                         |
+---------------------+--------------------------------------+
|  3                  | Fix exceptions overwritten when      |
|                     | nested import service topology       |
+---------------------+--------------------------------------+
|  4                  | fix costum datatype definition       |
|                     | in parameters                        |
+---------------------+--------------------------------------+
|  5                  | Fix StatefulEntityType when          |
|                     | entitytype is not define             |
+---------------------+--------------------------------------+
|  6                  | Fix substitution mapping assigned    |
|                     | value to nodetemplate                |
+---------------------+--------------------------------------+
|  7                  | Fix functest_run script for role     |
|                     | of heat_stack_owner                  |
+---------------------+--------------------------------------+


Deliverables
------------

Software deliverables
^^^^^^^^^^^^^^^^^^^^^

- nfv-toscaaparser


Documentation deliverables
^^^^^^^^^^^^^^^^^^^^^^^^^^

- release document
- installation document
- user guide document


Known Limitations, Issues and Workarounds
=========================================

System Limitations
^^^^^^^^^^^^^^^^^^


Known issues
^^^^^^^^^^^^


**JIRA TICKETS:**

+--------------------------------------+--------------------------------------+
| **REFERENCE ID**                     | **SLOGAN**                           |
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

Test runs with the following results:

+--------------------------------------+--------------------------------------+
| **TEST-SUITE**                       | **Results:**                         |
|                                      |                                      |
+--------------------------------------+--------------------------------------+
|   Unit test                          |   PASS                               |
+--------------------------------------+--------------------------------------+
