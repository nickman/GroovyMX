import org.helios.gmx.*;


def gmx = Gmx.newInstance();
gmx.mbeans("java.lang:type=MemoryPool,name=*", {
    println "${it.objectName}:${it.Usage.committed}";
});