SSH Credentials Configuration for [NanoCloud][nc]
====

[NanoCloud][nc] is extensively using SSH for its remotting. On Unix systems [NanoCloud][nc] will try its best to use common 
conventions and work without extra configuration.
But on Windows, there are no such conventions. It is also possible that you would like to tweak something.

By default, [NanoCloud][nc] is using `ssh-credentials.prop` file from user's home directory to load authentication rules.

Simplest `ssh-credentials.prop` will look like:


    #User superuser login for servers in domain [acme.com]
    *.acme.com=superuser

    #Password for superuser account in [acme.com] domain
    superuser@*.acme.com!password=***


You could also configure key based authentication

    #User superuser login for servers in domain [acme.com]
    *.acme.com=superuser

    #Specify private key superuser account in [acme.com] domain
    superuser@*.acme.com!private-key=C:/keys/superuser.id_dsa


All SSH connection options (username, password, keys) could also be configure directly for [ViNode][vinode]. 
But, it is generally good idea to keep sensitive information such as password separate.

`ssh-credentials.prop` is parsed as normal java property file, so all rules for comments, escaping, etc, would apply.

FAQ
----

### Q: Do I always need `ssh-credentials.prop`?
No, if there are no `ssh-credentials.prop`. Current user name and `~/.ssh/id_dsa` or `~/.ssh/id_rsa` keys will be used.

In other words, if you can SSH to target server from command line without entering password (and you are no using SSH key agents), 
there is no need for `ssh-credentials.prop`.

### Q: How do I find "right" user's home on Windows?
Try to execute following Java code.

    System.out.println(System.getProperty("user.home"));

[NanoCloud][nc] is using that system property to lookup `~/` prefixed paths.

### Q: Can I use private key SSH authentication?
Private key authentication is tested for passwordless keys.
It should also work with encrypted keys (if password is supplied), but it have never been tested.

### Q: Will [NanoCloud][nc] will work with SSH authentication forwarding?
No, it is not supported at the moment.

### Q: Can I use SSH key agents (e.g. pageant) to keep password secure?
No, it is not supported at the moment.

### Q: Can I encrypt password in `ssh-credentials.prop` somehow?
No, storing password in plain text is only way now.

 [nc]: https://github.com/gridkit/nanocloud
 [vinode]: https://github.com/gridkit/nanocloud/blob/vicluster-0.8/vicluster-core/src/main/java/org/gridkit/vicluster/ViNode.java