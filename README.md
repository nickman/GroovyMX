![GroovyMX - A Helios Project][1]

GroovyMX (or just **gmx**) is a Groovy wrapper for JMX Clients. Taking a page from Groovy's SQL support, 
the idea is to reduce the amount of code to write something useful against a JMX MBeanServer.

Here's a quick example. The following script connects to a remote MBeanServer and prints the committed number of bytes in each Memory Pool.

```groovy
import org.helios.gmx.*;
gmx = Gmx.remote("service:jmx:rmi://testserver1:8002/jndi/rmi://testserver1:8003/jmxrmi");
gmx.mbeans("java.lang:type=MemoryPool,name=*", {
    println "${it.objectName}:\t${it.Usage.committed}";
});
```

The output of the script is:

    java.lang:type=MemoryPool,name=PS Eden Space:  402653184
    java.lang:type=MemoryPool,name=PS Survivor Space:	16777216
    java.lang:type=MemoryPool,name=Code Cache:	3407872
    java.lang:type=MemoryPool,name=PS Perm Gen:	84738048
    java.lang:type=MemoryPool,name=PS Old Gen:	268435456

  * Requirements
  * Architecture
  * Dependencies
  * Gmx
    *  Local
    *  Remote
    *  Attach
  * MetaMBeans
  * Registering Listeners
  * Remote Gmx Agent
    * Upgrading Remote MBeanServerConnections to MBeanServers 
    * Agents
    * Remote Installs
      * RMI
      * SSH
    * Reverse Class Loading
    * Chanined Installs (Propagating Agents)
  * MBean Cross Registration 
  * See [groovy-remote][2] for closure remoting.

[1]: https://github.com/nickman/GroovyMX/blob/master/content/img/gmx-160-X-160.png?raw=true "Helios"
[2]: https://github.com/alkemist/groovy-remote "groovy-remote"