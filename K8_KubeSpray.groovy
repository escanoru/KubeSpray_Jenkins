def setDescription() {
    def item = Jenkins.instance.getItemByFullName(env.JOB_NAME)
    item.setDescription("<h5><span style=\"color:#138D75\">KubeSpray Automation job. Before running this pipeline, make sure to read the README.MD from <a href='https://github.com/escanoru/KubeSpray_Jenkins'>https://github.com/escanoru/KubeSpray_Jenkins</a></span></h5>")
    item.save()
}

setDescription()


def inventorySample = '''# ## Configure 'ip' variable to bind kubernetes services on a
# ## different ip than the default iface
# ## We should set etcd_member_name for etcd cluster. The node that is not a etcd member do not need to set the value, or can set the empty string value.
[all]
node1 ansible_host=95.54.0.12  # ip=10.3.0.1 etcd_member_name=etcd1
node2 ansible_host=95.54.0.13  # ip=10.3.0.2 etcd_member_name=etcd2
node3 ansible_host=95.54.0.14  # ip=10.3.0.3 etcd_member_name=etcd3
node4 ansible_host=95.54.0.15  # ip=10.3.0.4 etcd_member_name=etcd4
node5 ansible_host=95.54.0.16  # ip=10.3.0.5 etcd_member_name=etcd5
node6 ansible_host=95.54.0.17  # ip=10.3.0.6 etcd_member_name=etcd6

# ## configure a bastion host if your nodes are not directly reachable
# [bastion]
# bastion ansible_host=x.x.x.x ansible_user=some_user

[kube_control_plane]
node1
node2
node3

[etcd]
node1
node2
node3

[kube_node]
node2
node3
node4
node5
node6

[calico_rr]
# Calico advanced options: https://github.com/kubernetes-sigs/kubespray/blob/master/docs/calico.md

[k8s_cluster:children]
kube_control_plane
kube_node
calico_rr'''

pipeline {
	agent { label 'ansible' }
	options {
		ansiColor('gnome-terminal')
		// ansiColor('vga')
		// ansiColor('css')
		// ansiColor('gnome-terminal')
		
		buildDiscarder(logRotator(daysToKeepStr: '90'))
		}
  
    parameters {
        booleanParam(
            name: 'reset_k8s_cluster',
            defaultValue: true,
            description: 'Uninstall previous k8s installation, this will use the reset.yml playbook'
        )
        booleanParam(
            name: 'only_reset_k8s_cluster',
            defaultValue: false,
            description: 'Only uninstall, if this is set to true then the other stages will be skiped'
        )
        booleanParam(
            name: 'run_requirements',
            defaultValue: true,
            description: 'Set OS requirements'
        )
        booleanParam(
            name: 'install_kubespray',
            defaultValue: true,
            description: 'Set OS requirements'
        )
        text(
            name: 'inventory',
            defaultValue: "${inventorySample}",
            description: ''
        )
        string(
            name: 'user',
            defaultValue: 'root',
            description: '<h5>Username that will run the installation, the user must have enough privileges for writing SSL keys in /etc/, installing packages and interacting with various systemd daemons</h5>'
        )
        string(
            name: 'kube_version',
            defaultValue: 'v1.23.5',
            description: '<h5>Change this to use another Kubernetes version</h5>'
        )
        string(
            name: 'cluster_name',
            defaultValue: 'cluster.local',
            description: 'Leave empty if not needed'
        )
        string(
            name: 'http_proxy',
            defaultValue: '',
            description: '<h5>e.g http://my_proxy.com:8080</h5>'
        )
        string(
            name: 'https_proxy',
            defaultValue: '',
            description: '<h5>e.g http://my_proxy.com:8080</h5>'
        )
        string(
            name: 'no_proxy',
            defaultValue: '127.0.0.1,localhost',
            description: 'list of hostnames or range of domains to exclude from the proxy'
        )
        choice(
            name: 'kube_network_plugin',
            choices: ['calico','flannel','cilium','weave','cloud'],
		)
        choice(
            name: 'etcd_deployment_type',
            choices: ['host','docker'],
            description: 'Set this to docker if you are using container_manager: docker'
        )
        string(
            name: 'etcd_data_dir',
            defaultValue: '/var/lib/etcd',
            description: 'Directory where etcd data stored'
        )
        choice(
            name: 'container_manager',
            choices: ['containerd','crio','docker'],
            description: 'docker for docker, crio for cri-o and containerd for containerd.'
		)
        choice(
            name: 'resolvconf_mode',
            choices: ['host_resolvconf','docker_dns','none'],
            description: 'Can be docker_dns, host_resolvconf or none'
		)
        string(
            name: 'docker_daemon_graph',
            defaultValue: '/var/lib/docker',
            description: 'Path used to store Docker data. ONLY valid if container_manager is set to docker'
        )
        string(
            name: 'containerd_storage_dir',
            defaultValue: '/var/lib/container',
            description: 'Path used to store containerd data. ONLY valid if container_manager is set to containerd'
        )
        string(
            name: 'docker_log_opts',
            defaultValue: '--log-opt max-size=50m --log-opt max-file=5',
            description: 'Rotate container stderr/stdout logs option'
        )
        booleanParam(
            name: 'use_external_load_balancer',
            defaultValue: false,
            description: 'Whether or not to use an external load balancer fpr the kube api'
        )
        string(
            name: 'apiserver_loadbalancer_domain_name',
            defaultValue: '',
            description: 'VIP domain name for external Load Balancer. Leave empty if not needed'
        )
        string(
            name: 'apiserver_loadbalancer_address',
            defaultValue: '',
            description: 'VIP ip address for external Load Balancer. Leave empty if not needed'
        )
        string(
            name: 'apiserver_loadbalancer_port',
            defaultValue: '8383',
            description: 'VIP port for external Load Balancer. Leave empty if not needed'
        )
        booleanParam(
            name: 'dashboard_enabled',
            defaultValue: true,
            description: 'Whether or not to install nginx ingress'
        )
        booleanParam(
            name: 'ingress_nginx_enabled',
            defaultValue: true,
            description: 'Whether or not to install nginx ingress'
        )
        booleanParam(
            name: 'metrics_server_enabled',
            defaultValue: true,
            description: 'Whether or not to enable metrics'
        )
        booleanParam(
            name: 'metrics_server_kubelet_insecure_tls',
            defaultValue: true,
            description: 'Do not verify CA of serving certificates presented by Kubelets.  For testing purposes only. This will only take effect if the metrics_server_enabled parameter is set to true'
        )
        string(
            name: 'metrics_server_metric_resolution',
            defaultValue: '60s',
            description: 'Metrics scrape interval. This will only take effect if the metrics_server_enabled parameter is set to true'
        )
        choice(
            name: 'metrics_server_kubelet_preferred_address_types',
            choices: ['InternalIP','ExternalIP','InternalDNS','ExternalDNS','Hostname'],
            description: 'The priority of node address types to use when determining which address to use to connect to a particular node. This will only take effect if the metrics_server_enabled parameter is set to true'
        )            
        booleanParam(
            name: 'helm_enabled',
            defaultValue: true,
            description: 'Whether or not to install HELM'
        )
        booleanParam(
            name: 'cert_manager_enabled',
            defaultValue: true,
            description: 'Whether or not to install cert manager'
        )
        booleanParam(
            name: 'use_internal_loadbalancer',
            defaultValue: false,
            description: 'Whether or not to use internal loadbalancers for kube api servers'
        )
        choice(
            name: 'loadbalancer_apiserver_type',
            choices: ['nginx','haproxy'],
            description: 'What load balancer provider to use, this will only be consider if the loadbalancer_apiserver_type parameter is set to true'
		)
        booleanParam(
            name: 'use_localhost_as_kubeapi_loadbalancer',
            defaultValue: false,
            description: 'Whether or not to use localhost as kubeapi loadbalancer'
        )
        string(
            name: 'kube_service_addresses',
            defaultValue: '10.233.0.0/18',
            description: 'Kubernetes internal network for services, unused block of space.'
        )
        string(
            name: 'kube_pods_subnet',
            defaultValue: '10.233.64.0/18',
            description: 'Internal network. When used, it will assign IP addresses from this range to individual pods. This network must be unused in your network infrastructure!'
        )
        string(
            name: 'kubespray_temp_dir',
            defaultValue: '/tmp/kubespray_temp_dir',
            description: "Where the binaries will be downloaded. Note: ensure that you've enough disk space (about 1G)"
        )
    }
            	
    stages {
        stage('Creating Inventory File') {
			steps {
                sh """
                echo "" > ${WORKSPACE}/inventory.ini
                """
                writeFile file: "${WORKSPACE}/inventory.ini", text: "${inventory}"
                sh """
                ls -lht ${WORKSPACE}/inventory.ini
                cat ${WORKSPACE}/inventory.ini
                """
			}
		}

        stage('Clonning KubeSpray project') {
            steps {
                sh """
                cd ${WORKSPACE}/
                whoami
                echo ${https_proxy}
                git clone https://github.com/kubernetes-sigs/kubespray.git
                cd kubespray
                git checkout tags/v2.19.0
                cp ${WORKSPACE}/roles/scripts/kubeSpray_venv_install_requirements.sh .
                chmod +x kubeSpray_venv_install_requirements.sh
                ./kubeSpray_venv_install_requirements.sh
                """
            }
        }

        stage('Reset K8s Cluster') {
            when {
                expression { params.reset_k8s_cluster == true }
            }
            steps {
                // ansiblePlaybook(
                //     installation: "${WORKSPACE}/kubespray/venv/bin",
                //     playbook: "${env.WORKSPACE}/kubespray/reset.yml",
                //     inventory: "${env.WORKSPACE}/inventory.ini",
                //     become: true,
                //     becomeUser: "root",
                //     forks: 8,
                //     colorized: true,
                //     extras: '-u ${user} --ssh-extra-args=" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null"',
                //     extraVars: [
                //         jenkins_workspace: "${env.WORKSPACE}/",
                //         http_proxy: "${params.http_proxy}",
                //         https_proxy: "${params.https_proxy}",
                //         no_proxy: "${params.no_proxy}",
                //         reset_confirmation: "yes"
                //     ]
                // )
                ansiColor('xterm') {
                    sh '''
                    cd ${WORKSPACE}/kubespray/ ; echo -e "\n"
                    pwd ; echo -e "\n"
                    source venv/bin/activate ; echo -e "\n\n"
                    until time ansible-playbook -i ${WORKSPACE}/inventory.ini reset.yml -u root --become --become-user=root --extra-vars "http_proxy=${http_proxy} https_proxy=${https_proxy} no_proxy=${no_proxy} reset_confirmation='yes'" ; do sleep 5 ; done
                    deactivate ; echo -e "\n"s
                    '''
                }
                
            }
        }

        stage('Setting KubeSpray Env') {
            when {
                expression { params.only_reset_k8s_cluster == false }
            }
            steps {
                sh """
                echo "Setting KubeSpray Env"
                cd ${WORKSPACE}/kubespray
                cp ${WORKSPACE}/roles/scripts/kubeSpray_venv_install_requirements.sh .
                chmod +x kubeSpray_venv_install_requirements.sh
                ./kubeSpray_venv_install_requirements.sh
                rm -rf inventory/mycluster/
                cp -rfp ${WORKSPACE}/kubespray/inventory/sample/ ${WORKSPACE}/kubespray/inventory/mycluster/
                rm -rf ${WORKSPACE}/kubespray/inventory/mycluster/inventory.ini
                cp ${WORKSPACE}/inventory.ini ${WORKSPACE}/kubespray/inventory/mycluster/inventory.ini
                """
                ansiblePlaybook(
                    playbook: "${env.WORKSPACE}/roles/Requirements/populate_vars.yaml",
                    inventory: "${env.WORKSPACE}/inventory.ini",
                    forks: 16,
                    colorized: true,
                    extras: '-u ${user} --ssh-extra-args=" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null" --flush-cache -v',
                    extraVars: [
                        jenkins_workspace: "${env.WORKSPACE}/",
                        kube_version: "${params.kube_version}",
                        cluster_name: "${params.cluster_name}",
                        http_proxy: "${params.http_proxy}",
                        https_proxy: "${params.https_proxy}",
                        no_proxy: "${params.no_proxy}",
                        etcd_deployment_type: "${params.etcd_deployment_type}",
                        etcd_data_dir: "${params.etcd_data_dir}",
                        use_external_load_balancer: "${params.use_external_load_balancer}",
                        apiserver_loadbalancer_domain_name: "${params.apiserver_loadbalancer_domain_name}",
                        apiserver_loadbalancer_address: "${params.apiserver_loadbalancer_address}",
                        apiserver_loadbalancer_port: "${params.apiserver_loadbalancer_port}",
                        dashboard_enabled: "${params.dashboard_enabled}",
                        ingress_nginx_enabled: "${params.ingress_nginx_enabled}",
                        metrics_server_enabled: "${params.metrics_server_enabled}",
                        metrics_server_kubelet_insecure_tls: "${params.metrics_server_kubelet_insecure_tls}",
                        metrics_server_metric_resolution: "${params.metrics_server_metric_resolution}",
                        metrics_server_kubelet_preferred_address_types: "${params.metrics_server_kubelet_preferred_address_types}",
                        helm_enabled: "${params.helm_enabled}",
                        cert_manager_enabled: "${params.cert_manager_enabled}",
                        use_internal_loadbalancer: "${params.use_internal_loadbalancer}",
                        loadbalancer_apiserver_type: "${params.loadbalancer_apiserver_type}",
                        use_localhost_as_kubeapi_loadbalancer: "${params.use_localhost_as_kubeapi_loadbalancer}",
                        kube_network_plugin: "${params.kube_network_plugin}",
                        container_manager: "${params.container_manager}",
                        resolvconf_mode: "${params.resolvconf_mode}",
                        docker_daemon_graph: "${params.docker_daemon_graph}",
                        containerd_storage_dir: "${params.containerd_storage_dir}",
                        docker_log_opts: "${params.docker_log_opts}",
                        kube_service_addresses: "${params.kube_service_addresses}",
                        kube_pods_subnet: "${params.kube_pods_subnet}",
                        local_release_dir: "${params.kubespray_temp_dir}"
                    ]
                )
            }
        }     
        
        stage('Running KubeSpray') {
            when {
                expression { params.install_kubespray == true && params.only_reset_k8s_cluster == false }
            }
            steps {         
                // This is the recommended way of running ansible playbooks/roles from Jennkins
                retry(3) {
                    sh """
                    echo "Starting KubeSpray deployment"
                    cd ${WORKSPACE}/kubespray/
                    """
                    ansiblePlaybook(
                        installation: "${WORKSPACE}/kubespray/venv/bin",
                        playbook: "${env.WORKSPACE}/kubespray/cluster.yml",
                        inventory: "${env.WORKSPACE}/kubespray/inventory/mycluster/inventory.ini",
                        forks: 16,
                        become: true,
                        becomeUser: "root",
                        colorized: true,
                        extras: '-u ${user} --ssh-extra-args=" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null" --flush-cache -v',
                        extraVars: [
                            http_proxy: "${params.http_proxy}",
                            https_proxy: "${params.https_proxy}",
                            no_proxy: "${params.no_proxy}"
                        ]
                    )
                }
                // This also works but doesn't show the colors on the output which could help us find error or warnings in a more visual way.
                // sh '''
                // cd ${WORKSPACE}/kubespray/ ; echo -e "\n"
                // pwd ; echo -e "\n"
                // source venv/bin/activate ; echo -e "\n\n"
                // until time ansible-playbook -i ${WORKSPACE}/inventory.ini cluster.yml -u root --become --become-user=root --extra-vars "http_proxy=${http_proxy} https_proxy=${https_proxy} no_proxy=${no_proxy}" ; do sleep 5 ; done
                // deactivate ; echo -e "\n"s
            }
        }
    }
  
    post {
        always {
            echo 'Cleaning up the workspace'
            deleteDir()
        }
    }
}
