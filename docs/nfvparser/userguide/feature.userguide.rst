.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. (c) <optionally add copywriters name>



nfv parser Execution
=====================

Following is shown how to use nfv-toscaparser:

.. code-block:: bash

    tosca-parser --template-file=<path to the YAML template>  [--nrpv]  [--debug]
    or
        tosca-parser --template-file=<path to the CSAR zip file> [--nrpv]  [--debug]
    or
        tosca-parser --template-file=<URL to the template or CSAR>  [--nrpv]  [--debug]
    options:
      --nrpv Ignore input parameter validation when parse template.
      --debug debug mode for print more details other than raise exceptions when errors happen

Example:

.. code-block:: bash

   tosca-parser --template-file=vRNC.yaml --nrpv
