# KubeSpray Jenkins Automation Pipeline

For now this project only works on CentOS/RHEL 7/8.
Jenkins pipeline to automate the installation of Kubernetes on CentOS by using the Kubespray project, the pipeline has 5 Stages

1. Create the inventory file:
   - Takes the passed hostname on the host parameters and fills a pre-built inventory file
2. Create the SSH key pairs, only of they don't exist.
3. Running requirements:
   - SElinux_disable
   - disable_firewalld
   - docker_uninstall
   - k8s_uninstall
   - uninstall_calico
4. Setting KubeSpray Env:
   - Clones the Kubespray project in the Jenking the ${env.WORKSPACE}/roles/tmp/
   - Creates a venv on the ${env.WORKSPACE}/roles/tmp/kubespray/ directory and installs the kubespray requirements
   - Populates the inventory/mycluster/group_vars/all/all.yml with the pass values from the Jenkins user input section
   - Populates the inventory/mycluster/group_vars/k8s_cluster/k8s-cluster.yml with the pass values from the Jenkins user input section
5. Running KubeSpray:
   - This section runs the installation of K8s using KubeSpray, it could take from 7 to 30 mins, this will depend on the connection speed and the number of control plane and worker nodes.

Make sure to distribute the Ansible host public keys to the K8s nodes.