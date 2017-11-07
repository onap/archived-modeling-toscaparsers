.. This work is licensed under a Creative Commons Attribution 4.0 International License.

Checker API
===========

**POST /check_template/**
 - unnamed (isolated/one time) service template checking
 - return: a json representation of the report produced by the checker (an array with json objects as elements, one per error encoutered)

**POST /check_template/{catalog}/{name}**
 - named template submitted as part of the given catalog. A catalog allows a client application to isolate a particular TOSCA schema (type system) against which other templates will be checked. The catalog names are arbitrary (see catalog GET below as a way of checking if a catalog name is in use).
 - return: a json representation of the report produced by the checker (an array with json objects as elements, one per error encoutered). When using the catalog based api one can use import statements within the templates but .. all import references are towards templates submitted prior hence all the errors in the report are for the submitted template).

.. note::
   currently there is no explicit catalog creation API. A new catalog with the given name will be created if not already existing.

**GET /check_template/{catalog}/{name}**
 - checks if a template with the given name was submitted/checked as part of the given catalog.
 - returns HTTP 200 OK if a catalog with that name existed and the given name is associated with a previously submitted (named) template, 404 otherwise. No other catalog or template information is returned.

**POST /check_template/{catalog}**
 - unnamed template submitted as part of the given catalog. The template is processed but not registered so it cannot be referenced afterwards; it can use import statements refering to named templates within this catalog.
 - return: a json representation of the report produced by the checker (an array with json objects as elements, one per error encoutered)

**GET /check_template/{catalog}**
 - checks if a catalog with the given name exists
 - returns HTTP 200 OK if a catalog with that name existed, 404 otherwise. No other catalog information is returned.

**DELETE /check_template/{catalog}**
 - deletes an existing catalog (and all information associated with it)
 - returns HTTP 200 OK if a catalog with that name existed, 404 otherwise
