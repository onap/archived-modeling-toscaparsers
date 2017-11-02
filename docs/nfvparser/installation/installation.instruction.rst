.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0


NFV Parser Installation
===================================

NFV Parer provide two ways for installation, please follow the below installation steps to install nfv parser.

By code source
^^^^^^^^^^^^^^

Step 1: Clone the parser project.

.. code-block:: bash

    git clone http://gerrit.onap.org/r/modeling/toscaparsers

Step 2: Install the nfvparser sub project.

.. code-block:: bash

    # uninstall pre-installed tosca-parser
    pip uninstall -y tosca-parser

    # change directory to nfvparser
    cd ./toscaparsers/nfvparser

    # install requirements
    pip install -r requirements.txt

    # install nfv-toscaparser
    python setup.py install

By package
^^^^^^^^^^
Step 1: Make user pip has been already installed.

.. code-block:: bash

    # for ubuntu or debin
    apt-get install python-pip
    or
    # for centos,rhel or fedora
    yum install python-pip

Step 2: install nfv-toscaparser using pip

.. code-block:: bash

    pip install nfv-toscaparser
    or
    pip install --pre nfv-toscaparser


**Notes**: It must uninstall pre-installed tosca-parser before install the component, which is sure to use the
OPNFV version of tosca-parser other than openstack's components.

