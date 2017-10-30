.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0


Parser nfv toscaparser Installation
===================================

Nfv tocsa provide two method for installation, please follow the below installation steps to install toscaparser.

By code source
^^^^^^^^^^^^^^

Step 1: Clone the parser project.

Step 2: Install the tosca-parser sub project.

.. code-block:: bash

    # uninstall pre-installed tosca-parser
    pip uninstall -y tosca-parser

    # change directory to tosca-parser
    cd parser/tosca2heat/tosca-parser

    # install requirements
    pip install -r requirements.txt

    # install tosca-parser
    python setup.py install

By pacage
^^^^^^^^^^
Step 1: Make user pip has been already installed.

.. code-block:: bash

    # for ubuntu or debin
    apt-get install python-pip
    or
    # for centos,rhel or fedora
    yum install python-pip

Step 2: install nfv-tsoca using pip

.. code-block:: bash

    pip install nfv-tosca


**Notes**: It must uninstall pre-installed tosca-parser before install the component, which is sure to use the
OPNFV version of tosca-parser other than openstack's components.

