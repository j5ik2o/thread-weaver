  - name: ${name}
    instanceType: ${instance_type}
    privateNetworking: ${private_networking}
    securityGroups:
      attachIDs: [${security_groups}]
      withLocal: false
      withShared: false
    minSize: ${min_size}
    maxSize: ${max_size}
    labels: {role: ${role}}
#    ssh:
#      allow: ${allow_ssh}
#      publicKeyPath: ${ssh_public_key_path}
    iam:
      instanceProfileARN: ${instance_profile_arn}
